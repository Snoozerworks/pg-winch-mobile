package skarmflyg.org.gohigh.widgets;

import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Parameter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.View;


/**
 * TODO: document your custom view class.
 */
public class Digits extends View {
	public SensorMapping mapping;

	private Bitmap frame_bmp;						// Frame picture
	private RectF rect_wrn;							// Background for warning
	private float canvas_scale;					// Scale value for view

	private int REFRESH_INTERVALL = 100;			// Refresh interval in milliseconds
	private final Paint paint = new Paint();	// Paint for text and pictures.
	private final Paint paint_wrn = new Paint();

	private Thread updater_thread;			// The view updates regular in a separate thread

	private float text_pos[] = new float[2];


	public Digits(Context context) {
		super(context);
		init(null, 0);
	}


	public Digits(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}


	public Digits(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}


	public void setTargetVal(int v) {
		mapping.setTargetVal(v);
	}


	public void attachParam(Parameter p) {
		if (p == null) return;
		mapping.setMapping(p.low, p.high, p.low_map, p.high_map);
	}


	private void init(AttributeSet attrs, int defStyle) {
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Digits, defStyle, 0);

		mapping = new SensorMapping(a.getInt(R.styleable.Digits_raw_lo, 0), //
				a.getInt(R.styleable.Digits_raw_hi, 100), //
				a.getFloat(R.styleable.Digits_val_lo, 0), //
				a.getFloat(R.styleable.Digits_val_hi, 100));

		mapping.setChangeRate(a.getFloat(R.styleable.Digits_rate_limit, 15) * REFRESH_INTERVALL / 1000);
		mapping.setLowWarning(a.getInt(R.styleable.Digits_wrn_lo, 0));
		mapping.setHighWarning(a.getInt(R.styleable.Digits_wrn_hi, 100));
		mapping.format = a.getString(R.styleable.Meter_num_format);

		frame_bmp = BitmapFactory.decodeResource(getResources(), R.drawable.custom_digit_bg);
		int w = frame_bmp.getWidth();

		rect_wrn = getRect(0.10f * w, 0.1f * w, 0.88f * w, 0.6f * w);

		text_pos[0] = w * 0.88f;
		text_pos[1] = w * 0.55f;

		paint.setTextSize(w * 0.30f);
		paint.setTextAlign(Align.RIGHT);
		// paint.setAntiAlias(true);
		paint.setFlags(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);

		paint_wrn.setColor(Color.YELLOW);

		a.recycle();
	}


	private RectF getRect(float left, float top, float witdh, float hight) {
		return new RectF(left, top, left + witdh, top + hight);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int bh = frame_bmp.getHeight();
		int bw = frame_bmp.getWidth();
		int sh = MeasureSpec.getSize(heightMeasureSpec);
		int sw = MeasureSpec.getSize(widthMeasureSpec);

		canvas_scale = Math.min((float) sw / bw, (float) sh / bh);
		float s2 = Math.min((float) getSuggestedMinimumWidth() / bw, (float) getSuggestedMinimumHeight() / bh);

		canvas_scale = Math.max(canvas_scale, s2);
		canvas_scale = Math.min(canvas_scale, 1);

		if (canvas_scale < 1) {
			bh = (int) (bh * canvas_scale + 0.5f);
			bw = (int) (bw * canvas_scale + 0.5f);
		}

		setMeasuredDimension(bw, bh);

	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.save();

		if (mapping.isHigh() || mapping.isLow()) {
			canvas.drawRect(rect_wrn, paint_wrn);
		}

		canvas.scale(canvas_scale, canvas_scale);
		canvas.drawBitmap(frame_bmp, 0, 0, paint);
		canvas.drawText(mapping.getString(), text_pos[0], text_pos[1], paint);
		canvas.restore();

	}


	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		updater_thread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					Digits.this.postInvalidate();
					try {
						Thread.sleep(REFRESH_INTERVALL);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}
			}
		});
		updater_thread.setName("Digits");
		updater_thread.start();
	}


	@Override
	protected void onDetachedFromWindow() {
		updater_thread.interrupt();
		super.onDetachedFromWindow();
	}

}

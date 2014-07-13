package skarmflyg.org.gohigh.widgets;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Mapping;
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
	private int REFRESH_INTERVALL = 100; // Refresh interval in milliseconds

	// Set cyclicRedraw to true if widget shall auto update periodically
	public AtomicBoolean cyclicRedraw = new AtomicBoolean(false);
	private InstrumentBase digitData = new InstrumentBase();

	private Bitmap frame_bmp; // Frame picture
	private RectF rect_wrn; // Background for warning
	private float canvas_scale; // Scale value for view

	private final Paint paint = new Paint(); // Paint for text and pictures.
	private final Paint paint_wrn = new Paint();

	private Thread updater_thread; // The view updates regular in a separate
									// thread

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

	public void setTargetValue(int v) {
		digitData.setTargetValue(v);
		if (!cyclicRedraw.get()) {
			Digits.this.postInvalidate();
		}
	}

	public void setValue(int v) {
		digitData.setValue(v);
		if (!cyclicRedraw.get()) {
			Digits.this.postInvalidate();
		}
	}

	/**
	 * Set mapping of input to output value.
	 * 
	 * @param m
	 */
	public void setMapping(Mapping m) {
		digitData.setMapping(m);
	}

	public void setLowWarning(int low) {
		digitData.setLowWarning(low);
	}

	public void setHighWarning(int high) {
		digitData.setHighWarning(high);
	}

	private void init(AttributeSet attrs, int defStyle) {
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.Digits, defStyle, 0);

		digitData.setMapping(new Mapping(
				a.getInt(R.styleable.Digits_raw_lo, 0), //
				a.getInt(R.styleable.Digits_raw_hi, 100), //
				a.getInt(R.styleable.Digits_val_lo, 0), //
				a.getInt(R.styleable.Digits_val_hi, 100)));
		digitData.setRateLimiter(new RateLimiter(a.getInt(
				R.styleable.Digits_rate_limit, 15)));
		digitData.setLowWarning(a.getInt(R.styleable.Digits_wrn_lo, 0));
		digitData.setHighWarning(a.getInt(R.styleable.Digits_wrn_hi, 100));

		frame_bmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.custom_digit_bg);
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
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		updater_thread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					if (cyclicRedraw.get()) {
						Digits.this.postInvalidate();
					}
					try {
						Thread.sleep(REFRESH_INTERVALL);
					} catch (InterruptedException e) {
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

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.save();

		if (digitData.hasWarning()) {
			canvas.drawRect(rect_wrn, paint_wrn);
		}

		canvas.scale(canvas_scale, canvas_scale);
		canvas.drawBitmap(frame_bmp, 0, 0, paint);
		String str = String.format(Locale.ENGLISH, "%3.1f",
				digitData.getMappedValue());
		canvas.drawText(str, text_pos[0], text_pos[1], paint);

		canvas.restore();

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int bh = frame_bmp.getHeight();
		int bw = frame_bmp.getWidth();
		int sh = MeasureSpec.getSize(heightMeasureSpec);
		int sw = MeasureSpec.getSize(widthMeasureSpec);

		canvas_scale = Math.min((float) sw / bw, (float) sh / bh);
		float s2 = Math.min((float) getSuggestedMinimumWidth() / bw,
				(float) getSuggestedMinimumHeight() / bh);

		canvas_scale = Math.max(canvas_scale, s2);
		canvas_scale = Math.min(canvas_scale, 1);

		if (canvas_scale < 1) {
			bh = (int) (bh * canvas_scale + 0.5f);
			bw = (int) (bw * canvas_scale + 0.5f);
		}

		setMeasuredDimension(bw, bh);

	}

}

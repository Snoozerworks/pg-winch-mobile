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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class Meter extends ImageView {
	public final int REFRESH_INTERVALL = 100;

	// Set cyclicRedraw to true if widget shall auto update periodically
	public AtomicBoolean cyclicRedraw = new AtomicBoolean(false);

	private InstrumentBase digitData = new InstrumentBase();
	private InstrumentBase gaugeData = new InstrumentBase();

	private final Paint paint = new Paint();
	private final Paint paint_wrn = new Paint();
	private Matrix img_matrix = new Matrix();

	private float needle_pos[] = new float[4];
	private float text_pos[] = new float[2];

	private RectF rect_wrn;

	private Bitmap needle_bmp;
	private Bitmap scale_bmp;
	private float canvas_scale;

	private Thread updater_thread;

	public Meter(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public Meter(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public Meter(Context context) {
		super(context);

	}

	private void init(Context context, AttributeSet attrs) {
		Log.d(this.getClass().getSimpleName(), "init");

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Meter);

		// Setup data for the digit display
		digitData.setMapping(new Mapping( //
				a.getInt(R.styleable.Meter_raw_lo, 128), //
				a.getInt(R.styleable.Meter_raw_hi, 896), //
				a.getInt(R.styleable.Meter_val_lo, 0), //
				a.getInt(R.styleable.Meter_val_hi, 110)));
		digitData.setRateLimiter(new RateLimiter(a.getInt(
				R.styleable.Meter_rate_limit, Integer.MAX_VALUE)));
		digitData.setLowWarning(a.getInt(R.styleable.Meter_wrn_lo, 0));
		digitData.setHighWarning(a.getInt(R.styleable.Meter_wrn_hi, 1023));

		// Setup data for the gauge display
		gaugeData.setMapping(new Mapping( //
				a.getInt(R.styleable.Meter_needle_min_val, 128), //
				a.getInt(R.styleable.Meter_needle_max_val, 896), //
				a.getInt(R.styleable.Meter_needle_min_ang, -120), //
				a.getInt(R.styleable.Meter_needle_max_ang, 120)));
		gaugeData.setRateLimiter(new RateLimiter(a.getInt(
				R.styleable.Meter_rate_limit, Integer.MAX_VALUE)));

		a.recycle();

		// Get bitmaps
		scale_bmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.custom_meter_bg);
		needle_bmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.custom_needle);

		int sw = scale_bmp.getWidth();
		int nw = needle_bmp.getWidth();

		needle_pos[0] = (32 / 338f) * sw; // Pivot point dx
		needle_pos[1] = (149 / 338f) * sw;// Pivot point dy
		needle_pos[2] = ((169 - 32) / 338f) * sw; // Translation point dx
		needle_pos[3] = ((169 - 149) / 338f) * sw;// Translation point dy

		text_pos[0] = sw * 0.64f;
		text_pos[1] = sw * 0.86f;

		rect_wrn = getRect(0.30f * sw, 0.71f * sw, 0.42f * sw, 0.22f * sw);

		paint.setTextSize(nw * 0.62f);
		paint.setTextAlign(Align.RIGHT);
		// paint.setAntiAlias(true);
		paint.setFlags(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);

		paint_wrn.setColor(Color.YELLOW);

	}

	private RectF getRect(float left, float top, float witdh, float hight) {
		return new RectF(left, top, left + witdh, top + hight);
	}

	/**
	 * Set mapping of input to output value.
	 * 
	 * @param m
	 */
	public void setMapping(Mapping m) {
		digitData.setMapping(m);
	}

	public void setTargetValue(int v) {
		digitData.setTargetValue(v);
		gaugeData.setTargetValue(v);
		if (!cyclicRedraw.get()) {
			Meter.this.postInvalidate();
		}
	}

	public void setValue(int v) {
		digitData.setValue(v);
		gaugeData.setValue(v);
		if (!cyclicRedraw.get()) {
			Meter.this.postInvalidate();
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		updater_thread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					if (cyclicRedraw.get()) {
						Meter.this.postInvalidate();
					}
					try {
						Thread.sleep(REFRESH_INTERVALL);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		});
		updater_thread.setName("Meter");
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

		float rotation = gaugeData.getLimitedMappedValue();

		img_matrix.reset();
		img_matrix.setRotate(rotation, needle_pos[0], needle_pos[1]);
		img_matrix.postTranslate(needle_pos[2], needle_pos[3]);

		canvas.save();

		if (digitData.hasWarning()) {
			canvas.drawRect(rect_wrn, paint_wrn);
		}

		canvas.drawBitmap(scale_bmp, 0, 0, paint);
		canvas.drawBitmap(needle_bmp, img_matrix, paint);

		String str = String.format(Locale.ENGLISH, "%3.1f",
				digitData.getLimitedMappedValue());
		canvas.drawText(str, text_pos[0], text_pos[1], paint);

		canvas.restore();

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(this.getClass().getSimpleName(), "onMeasure");

		int bh = scale_bmp.getHeight();
		int bw = scale_bmp.getWidth();
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

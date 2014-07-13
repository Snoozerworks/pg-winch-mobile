package skarmflyg.org.gohigh.widgets;

import java.util.Locale;
import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Mapping;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextDigits extends TextView {

	private Mapping valueMapping; // Map from raw input to output value
	private RateLimiter valueRateLimiter; // Rate limit value to display

	// private float current_raw = 0;

	// private float raw_lo = 0;
	// private float raw_hi = 100;
	// private float val_lo = 0;
	// private float val_hi = 100;
	// private float rate_limit = 15;

	// private float map_k;
	// private float map_m;
	// private Parameter parameter;

	public TextDigits(Context context) {
		super(context);
		init(null, 0);
	}

	public TextDigits(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	public TextDigits(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle) {
		// Load attributes
		TypedArray a = getContext().obtainStyledAttributes(attrs,
				R.styleable.Digits, defStyle, 0);

		// rate_limit = a.getFloat(R.styleable.Digits_rate_limit, rate_limit);
		valueRateLimiter = new RateLimiter(a.getInt(
				R.styleable.Digits_rate_limit, 15));

		// raw_hi = a.getFloat(R.styleable.Digits_raw_hi, raw_hi);
		// raw_lo = a.getFloat(R.styleable.Digits_raw_lo, raw_lo);
		// val_hi = a.getFloat(R.styleable.Digits_val_hi, val_hi);
		// val_lo = a.getFloat(R.styleable.Digits_val_lo, val_lo);
		valueMapping = new Mapping( //
				a.getInt(R.styleable.Digits_raw_lo, 0), //
				a.getInt(R.styleable.Digits_raw_hi, 100), //
				a.getInt(R.styleable.Digits_val_lo, 0), //
				a.getInt(R.styleable.Digits_val_hi, 100));

		// map_k = (float) (val_hi - val_lo) / (raw_hi - raw_lo);
		// map_m = val_lo - map_k * raw_lo;

		a.recycle();
	}

	// public void attachParam(Parameter p) {
	// parameter = p;
	// if (parameter == null)
	// return;
	//
	// raw_hi = parameter.high;
	// raw_lo = parameter.low;
	// val_hi = parameter.high_map;
	// val_lo = parameter.low_map;
	// }

	/**
	 * Set mapping of input to output value.
	 * 
	 * @param m
	 */
	public void setMapping(Mapping m) {
		if (m != null) {
			valueMapping = m;
		}
	}

	// public void setRawValue(short v) {
	// target_raw = v;
	//
	// }
	public void setTargetValue(int v) {
		valueRateLimiter.setTargetValue(v);
	}

	// public void setRawValue(float target_raw) {
	// current_raw += Math.min(rate_limit,
	// Math.max(-rate_limit, target_raw - current_raw));
	// String digit_string = String.format(Locale.ENGLISH, "%3.1f", map_k
	// * current_raw + map_m);
	// setText(digit_string);
	// }

	public void update() {
		String str = String.format(Locale.ENGLISH, "%3.1f",
				valueMapping.map(valueRateLimiter.getValue()));
		setText(str);
	}
}

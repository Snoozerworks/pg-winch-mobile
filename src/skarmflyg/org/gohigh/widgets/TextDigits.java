package skarmflyg.org.gohigh.widgets;

import java.util.Locale;
import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Parameter;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextDigits extends TextView {
	private float current_raw = 0;

	private float raw_lo = 0;
	private float raw_hi = 100;
	private float val_lo = 0;
	private float val_hi = 100;
	private float rate_limit = 15;

	private float map_k;
	private float map_m;
	private Parameter parameter;


	public TextDigits(Context context) {
		super(context);
		init(null, 0);
		// TODO Auto-generated constructor stub
	}


	public TextDigits(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
		// TODO Auto-generated constructor stub
	}


	public TextDigits(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
		// TODO Auto-generated constructor stub
	}


	private void init(AttributeSet attrs, int defStyle) {
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Digits,
				defStyle, 0);

		rate_limit = a.getFloat(R.styleable.Digits_rate_limit, rate_limit);
		raw_hi = a.getFloat(R.styleable.Digits_raw_hi, raw_hi);
		raw_lo = a.getFloat(R.styleable.Digits_raw_lo, raw_lo);
		val_hi = a.getFloat(R.styleable.Digits_val_hi, val_hi);
		val_lo = a.getFloat(R.styleable.Digits_val_lo, val_lo);

		map_k = (float) (val_hi - val_lo) / (raw_hi - raw_lo);
		map_m = val_lo - map_k * raw_lo;
	}


	public void attachParam(Parameter p) {
		parameter = p;
		if (parameter == null)
			return;

		raw_hi = parameter.high;
		raw_lo = parameter.low;
		val_hi = parameter.high_map;
		val_lo = parameter.low_map;
	}


	// public void setRawValue(short v) {
	// target_raw = v;
	//
	// }

	public void setRawValue(float target_raw) {
		current_raw += Math.min(rate_limit, Math.max(-rate_limit, target_raw - current_raw));
		String digit_string = String.format(Locale.ENGLISH, "%3.1f", map_k * current_raw + map_m);
		setText(digit_string);
	}

}

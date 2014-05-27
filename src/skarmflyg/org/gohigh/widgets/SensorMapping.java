package skarmflyg.org.gohigh.widgets;

import java.util.Locale;
import skarmflyg.org.gohigh.arduino.Parameter;


public class SensorMapping {
	public int val = 0;			// Current Value
	private int target = 0;	// Setpoint value

	private int x0, x1;		// Map from
	private float y0, y1;	// Map to
	private float k, m;		// Mapping constants

	private int wrn_hi, wrn_lo;		// Map from
	private float change_rate = Integer.MAX_VALUE;	// Change rate value
	public String format = "%3.1f";


	public SensorMapping(int x0, int x1, float y0, float y1) {
		setMapping(x0, x1, y0, y1);
		wrn_hi = x1;
		wrn_lo = x0;
	}


	public void setMapping(int x0, int x1, float y0, float y1) {
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
		this.k = (float) (y1 - y0) / (x1 - x0);
		this.m = y0 - k * x0;
	}


	public void setMapping(Parameter p) {
		setMapping(p.low, p.high, p.low_map, p.high_map);
	}


	public void setHighWarning(int hi) {
		wrn_hi = hi;
	}


	public void setLowWarning(int lo) {
		wrn_lo = lo;
	}


	public int setTargetVal(int v) {
		target = v;

		float delta = target - val;

		if (delta < -change_rate) {
			delta = -change_rate;
		} else if (delta > change_rate) {
			delta = change_rate;
		}
		val += delta;
		return val;
	}


	public int getTarget() {
		return target;
	}


	public void setChangeRate(float r) {
		change_rate = r;
	}


	public boolean isHigh() {
		return val > wrn_hi;
	}


	public boolean isLow() {
		return val < wrn_lo;
	}


	public float map() {
		return val * k + m;
	}


	public float map_lim() {
		int v = val;
		if (v < x0) {
			v = x0;
		} else if (v > x1) {
			v = x1;
		}
		return v * k + m;
	}


	public String getString() {
		return String.format(Locale.ENGLISH, format, val * k + m);
	}

}
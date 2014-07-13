package skarmflyg.org.gohigh.arduino;

import java.util.Locale;

/**
 * Class for mapping an input to an output by linear interpolation between 2
 * points.
 * 
 * @author markus
 * 
 */
public class Mapping {
	private int from_lo = 0;
	private int from_hi = 100;
	private int to_lo = 0;
	private int to_hi = 100;

	private float k = 1;
	private float m = 0;

	public Mapping() {
	}

	public Mapping(int from_lo, int from_hi, int to_lo, int to_hi) {
		setMapping(from_lo, from_hi, to_lo, to_hi);
	}

	static public float map(int val, int from_lo, int from_hi, int to_lo,
			int to_hi) {
		return (float) (val - from_lo) * (to_hi - to_lo) / (from_hi - from_lo)
				+ to_lo;
	}

	public void setMapping(int from_lo, int from_hi, int to_lo, int to_hi) {
		this.from_lo = from_lo;
		this.from_hi = from_hi;
		this.to_lo = to_lo;
		this.to_hi = to_hi;
		k = (float) (to_hi - to_lo) / (from_hi - from_lo);
		m = to_lo - k * from_lo;
	}

	public int getLow() {
		return from_lo;
	}

	public int getLowMap() {
		return to_lo;
	}

	public int getHigh() {
		return from_hi;
	}

	public int getHighMap() {
		return to_hi;
	}

	/**
	 * Map value val to scaled value.
	 * 
	 * @param val
	 * @return
	 */
	public float map(int val) {
		return k * val + m;
	}

	/**
	 * As map() but limits output to low_map/high_map if below/above range.
	 * 
	 * @param val
	 * @return
	 */
	public float mapLim(int val) {
		if (val < from_lo) {
			return map(from_lo);
		} else if (val > from_hi) {
			return map(from_hi);
		} else {
			return map(val);
		}
	}

	/**
	 * Map from scaled to raw value.
	 * 
	 * @param val
	 * @return
	 */
	public int mapInverse(float val) {
		return (int) ((val - m) / k);
	}

	public String toString() {
		return String.format(Locale.ENGLISH, "map (lo|hi): (%d|%d)", //
				from_lo, from_hi, to_lo, to_hi);
	}
}

package skarmflyg.org.gohigh.widgets;

import skarmflyg.org.gohigh.arduino.Mapping;

public class InstrumentBase {

	// Map from raw input to output value
	private Mapping valueMapping = new Mapping();

	// Rate limit value to display
	private RateLimiter valueRateLimiter = new RateLimiter();

	// A low level warning.
	private int rawMinWarn = Integer.MIN_VALUE;

	// A high level warning.
	private int rawMaxWarn = Integer.MAX_VALUE;

	/**
	 * Retrieve the rate limited, boundary checked and mapped value.
	 * 
	 * @return
	 */
	public float getLimitedMappedValue() {
		return valueMapping.mapLim(valueRateLimiter.getValue());
	}

	/**
	 * Retrieve the rate limited and mapped value.
	 * 
	 * @return
	 */
	public float getMappedValue() {
		return valueMapping.map(valueRateLimiter.getValue());
	}

	/**
	 * Retrieve the rate limited value.
	 * 
	 * @return
	 */
	public int getValue() {
		return valueRateLimiter.getValue();
	}

	/**
	 * Return true when output value is outside warning levels.
	 * 
	 * @return
	 */
	public boolean hasWarning() {
		return (valueRateLimiter.getValue() > rawMaxWarn || valueRateLimiter
				.getValue() < rawMinWarn);
	}

	public void setTargetValue(int v) {
		valueRateLimiter.setTargetValue(v);
	}

	/**
	 * Set value without applying rate limitation.
	 * 
	 * @param v
	 */
	public void setValue(int v) {
		valueRateLimiter.setTargetValue(v);
	}

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

	/**
	 * Set mapping of input to output value.
	 * 
	 * @param m
	 */
	public void setRateLimiter(RateLimiter r) {
		if (r != null) {
			valueRateLimiter = r;
		}
	}

	/**
	 * Set low warning level, comparing against the raw value.
	 * 
	 * @param low
	 */
	public void setLowWarning(int low) {
		rawMinWarn = low;
	}

	/**
	 * Set high warning level, comparing against the raw value.
	 * 
	 * @param high
	 */
	public void setHighWarning(int high) {
		rawMaxWarn = high;
	}
}

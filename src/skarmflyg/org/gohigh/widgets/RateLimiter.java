package skarmflyg.org.gohigh.widgets;

/**
 * Keep a previous value to limit the output of next based on max allowed
 * change.
 * 
 * 
 * @author markus
 * 
 */
public class RateLimiter {
	private int val = 0; // Current Value
	private int changeRate = 1000;

	public RateLimiter() {
	}

	public RateLimiter(int rate_lim) {
		changeRate = rate_lim;
	}

	/**
	 * Set change rate to use.
	 * 
	 * @param rate
	 */
	public void setChangeRate(int rate) {
		changeRate = rate;
	}

	/**
	 * Sets value to target without applying rate limitation.
	 * 
	 * @param target
	 */
	public void resetToTarget(int target) {
		val = target;
	}

	/**
	 * Calculate rate limited output given a target value.
	 * 
	 * @param target
	 * @return
	 */
	public int setTargetValue(int target) {
		int delta = target - val;

		if (delta < -changeRate) {
			delta = -changeRate;
		} else if (delta > changeRate) {
			delta = changeRate;
		}
		val += delta;
		return val;
	}

	/**
	 * Get current output value.
	 * 
	 * @return
	 */
	public int getValue() {
		return val;
	}
}

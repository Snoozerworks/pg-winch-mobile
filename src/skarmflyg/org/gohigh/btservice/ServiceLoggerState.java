package skarmflyg.org.gohigh.btservice;

public enum ServiceLoggerState {
	LOGGER_ACTIVE, //
	LOGGER_INACTIVE;

	// Unique number for this enum
	public static final int ENUM_TYPE = 1;

	/**
	 * Return the integer (ordinal) value.
	 * 
	 * @return
	 */
	public int toInt() {
		return this.ordinal();
	}

	/**
	 * Given an integer (ordinal) value, return the enum.
	 * 
	 * @return Enum or null if not found.
	 */
	static public ServiceLoggerState toEnum(int ordinal) {
		if (ordinal >= 0 && ordinal < ServiceLoggerState.values().length) {
			return ServiceLoggerState.values()[ordinal];
		} else {
			return null;
		}
	}

}

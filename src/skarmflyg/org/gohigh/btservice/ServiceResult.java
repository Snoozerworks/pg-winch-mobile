package skarmflyg.org.gohigh.btservice;

public enum ServiceResult {
	PARAMETER_RECEIVED, //
	SAMPLE_RECEIVED, //
	PACKAGE_TIMEOUT, //
	CONNECTION_TIMEOUT, //
	ANS_TXT; //

	// Unique number for this enum
	public static final int ENUM_TYPE = 2;

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
	static public ServiceResult toEnum(int ordinal) {
		if (ordinal >= 0 && ordinal < ServiceResult.values().length) {
			return ServiceResult.values()[ordinal];
		} else {
			return null;
		}
	}

}

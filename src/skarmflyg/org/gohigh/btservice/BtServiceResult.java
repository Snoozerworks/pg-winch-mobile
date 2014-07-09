package skarmflyg.org.gohigh.btservice;

public enum BtServiceResult {
	// STATE_CONNECTED, STATE_DISCONNECTED, STATE_SAMPELS, STATE_SYNCS,
	// STATE_STOPPED, //
	PARAMETER_RECEIVED, //
	SAMPLE_RECEIVED, //
	PACKAGE_TIMEOUT, //
	CONNECTION_TIMEOUT, //
	ANS_TXT; //

	// Unique number for this enum
	public static final byte ENUM_TYPE = 0;

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
	static public BtServiceResult toEnum(int ordinal) {
		if (ordinal >= 0 && ordinal < BtServiceResult.values().length) {
			return BtServiceResult.values()[ordinal];
		} else {
			return null;
		}
	}

	// private int val;
	//
	// // Lookup table
	// private static final SparseArray<BtServiceResponse> lookup = new
	// SparseArray<BtServiceResponse>();
	//
	// // Populate the lookup table on loading time
	// static {
	// for (BtServiceResponse s : EnumSet.allOf(BtServiceResponse.class))
	// lookup.put(s.Value(), s);
	// }
	//
	//
	// private BtServiceResponse() {
	// val = this.ordinal();
	// }
	//
	//
	// public int Value() {
	// return val;
	// }
	//
	//
	// // This method can be used for reverse lookup purpose
	// static public BtServiceResponse get(int v) {
	// return lookup.get(v);
	// }
	//
	//
	// public boolean Equal(int v) {
	// return v == val;
	// }

}

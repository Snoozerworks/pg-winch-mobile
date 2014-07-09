package skarmflyg.org.gohigh.btservice;

/**
 * Bluetooth service status.
 * 
 * @author markus
 * 
 */
public enum BtServiceStatus {
	STATE_CONNECTED, //
	STATE_DISCONNECTED, //
	STATE_SAMPELS, //
	STATE_SYNCS, //
	STATE_STOPPED; //

	// Unique number for this enum
	public static final byte ENUM_TYPE = 1;

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
	static public BtServiceStatus toEnum(int ordinal) {
		if (ordinal >= 0 && ordinal < BtServiceStatus.values().length) {
			return BtServiceStatus.values()[ordinal];
		} else {
			return null;
		}
	}

}

package skarmflyg.org.gohigh.btservice;

/**
 * Bluetooth service status.
 * 
 * @author markus
 * 
 */
public enum ServiceState {
	STATE_CONNECTED, //
	STATE_DISCONNECTED, //
	STATE_SAMPELS, //
	STATE_SYNCS, //
	STATE_STOPPED; //

	// Unique number for this enum
	public static final byte ENUM_TYPE = 3;

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
	static public ServiceState toEnum(int ordinal) {
		if (ordinal >= 0 && ordinal < ServiceState.values().length) {
			return ServiceState.values()[ordinal];
		} else {
			return null;
		}
	}

}

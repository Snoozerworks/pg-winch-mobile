package skarmflyg.org.gohigh.btservice;

/**
 * Commands for the bluetooth service.
 * 
 * @author markus
 * 
 */
public enum BtServiceCommand {
	CONNECT, DISCONNECT, STOP, //
	UP, DOWN, SELECT, SETP, //
	GET_SAMPLE, GET_SAMPLES, GET_PARAMETERS, GET_STATE, _READ;

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
	static public BtServiceCommand toEnum(int ordinal) {
		if (ordinal >= 0 && ordinal < BtServiceCommand.values().length) {
			return BtServiceCommand.values()[ordinal];
		} else {
			return null;
		}
	}

}

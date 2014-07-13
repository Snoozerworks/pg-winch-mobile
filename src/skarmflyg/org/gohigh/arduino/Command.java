package skarmflyg.org.gohigh.arduino;

import skarmflyg.org.gohigh.btservice.BtServiceCommand;

public enum Command {
	NOCMD((byte) 0x00), // No command
	SET((byte) 0x01), // Set switch active
	UP((byte) 0x02), // Up switch active
	DOWN((byte) 0x03), // Down switch active
	SETP((byte) 0x04), // Set parameter (virtual) switch active
	GET((byte) 0x05); // Get sample (virtual) switch active

	private byte byteVal;

	Command(byte b) {
		byteVal = b;
	}

	// public boolean equals(byte i) {
	// return (byteVal == i);
	// }

	public byte getByte() {
		return byteVal;
	}

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

	/**
	 * Compare integer representation with enum.
	 * 
	 * @param cmd
	 * @return True if enum_int==ordinal()
	 */
	public boolean is(int cmd) {
		return ordinal() == cmd;
	}
}

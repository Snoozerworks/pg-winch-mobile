package skarmflyg.org.gohigh.arduino;

public enum Command {
	NOCMD((byte) 0x00), // Request mode change to Installation Settings
	CONF((byte) 0x01), // Request mode change to Installation Settings
	SET((byte) 0x02), // Set switch active
	UP((byte) 0x03), // Up switch active
	DOWN((byte) 0x04), // Down switch active
	SETP((byte) 0x05), // Set parameter (virtual) switch active
	GET((byte) 0x06); // Get sample (virtual) switch active

	private byte byteVal;


	Command(byte b) {
		byteVal = b;
	}


	public boolean equals(byte i) {
		return (byteVal == i);
	}


	public byte getByte() {
		return byteVal;
	}

	// public byte getByte() {
	// return (byte) (short_val & 0xFF);
	// }
}

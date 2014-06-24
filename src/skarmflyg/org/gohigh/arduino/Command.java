package skarmflyg.org.gohigh.arduino;

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


	public boolean equals(byte i) {
		return (byteVal == i);
	}


	public byte getByte() {
		return byteVal;
	}

}

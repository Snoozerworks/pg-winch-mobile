package skarmflyg.org.gohigh.arduino;

/**
 * Abstract class defining a base class for the different types of data packages
 * received from the Arduino.
 * 
 * @author Markus
 * 
 */
abstract public class DataPackage {
	public Mode mode;
	public byte[] raw;

	public DataPackage(byte byte_size) {
		mode = Mode.NOMODE;
		raw = new byte[byte_size];
	}

	/**
	 * Load data from raw byte array.
	 */
	abstract public void loadBytes(byte[] bytearr);

	@Override
	abstract public String toString();

	static public short byte2short(byte b1) {
		return (short) (b1 & 0xFF);
	}

	static public short byte2short(byte b1, byte b2) {
		return (short) (((b1 & 0xFF) << 8) | (b2 & 0xFF));
	}

	static public long byte2long(byte b1, byte b2, byte b3, byte b4) {
		return ((b1 & 0xFF) << 24) | //
				((b2 & 0xFF) << 16) | //
				((b3 & 0xFF) << 8) | //
				(b4 & 0xFF);
	}

}

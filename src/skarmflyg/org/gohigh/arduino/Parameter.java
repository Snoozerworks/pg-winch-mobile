package skarmflyg.org.gohigh.arduino;

import java.util.Arrays;

import android.os.Parcel;

/**
 * Objects of class Parameter represents one parameter from the winch.
 * 
 * @author markus
 * 
 */
public class Parameter extends DataPackage {
	static final public byte BYTE_SIZE = 35;
	static final private byte PARAM_DESCR_BYTE_SIZE = 21;

	public byte index = (byte) 255;
	public short val = 0;
	public short low = 0;
	public short high = 100;
	public short low_map = 0;
	public short high_map = 100;
	public short step = 1;

	public float val_map; // Mapped value

	private float k = 1;
	private float m = 0;

	public String descr;

	public Parameter() {
		super(BYTE_SIZE);
		raw = new byte[BYTE_SIZE];
	}

	public Parameter(byte[] raw_data) {
		super(BYTE_SIZE);
		raw = new byte[BYTE_SIZE];
		this.LoadBytes(raw_data);
	}

	public Parameter(Parcel in) {
		super(BYTE_SIZE);
		in.readByteArray(raw);
		LoadBytes(raw);
	}

	@Override
	public void LoadBytes(byte[] bytearr) {
		if (bytearr.length < BYTE_SIZE) {
			return;
		}
		raw = Arrays.copyOf(bytearr, BYTE_SIZE);

		mode = Mode.get(raw[0]);
		index = raw[1];
		val = byte2short(raw[2], raw[3]);
		low = byte2short(raw[4], raw[5]);
		high = byte2short(raw[6], raw[7]);
		low_map = byte2short(raw[8], raw[9]);
		high_map = byte2short(raw[10], raw[11]);
		step = byte2short(raw[12], raw[13]);

		k = (float) (high_map - low_map) / (high - low);
		m = low_map - k * low;

		val_map = Map(val);

		byte[] byte_descr = new byte[PARAM_DESCR_BYTE_SIZE];
		System.arraycopy(raw, 14, byte_descr, 0, PARAM_DESCR_BYTE_SIZE);
		descr = new String(byte_descr);
	}

	static public float Map(short val, short low, short high, short low_map,
			short high_map) {
		return (float) (val - low) * (high_map - low_map) / (high - low)
				+ low_map;
	}

	static public Mode getMode(byte[] raw) {
		return Mode.get(raw[0]);
	}

	static public byte getIndex(byte[] raw) {
		return (byte) raw[1];
	}

	static public short getVal(byte[] raw) {
		return byte2short(raw[2], raw[3]);
	}

	static public short getLow(byte[] raw) {
		return byte2short(raw[4], raw[5]);
	}

	static public short getHigh(byte[] raw) {
		return byte2short(raw[6], raw[7]);
	}

	/**
	 * Map raw value to scaled value.
	 * 
	 * @param val
	 * @return
	 */
	public float Map(short val) {
		return k * val + m;
	}

	/**
	 * Map from scaled to raw value.
	 * 
	 * @param val
	 * @return
	 */
	public short MapInverse(float val) {
		float inv;
		inv = (val - (float) m) / (float) k;
		return (short) inv;
	}

	@Override
	public String toString() {
		String format = "(%2d) %s\nmode.......: %s\nvalue......: %d\nlim (lo|hi): (%d,%d)\nmap (lo|hi): (%d|%d)";
		return String.format(format, index, descr, mode.toString(), val, low,
				high, low_map, high_map);
	}

	public String toStringMapped() {
		String format = "(%2d) %s\nmode.......: %s\nvalue......: %.1f\nlim (lo|hi): (%d|%d)";
		return String.format(format, index, descr, mode.toString(), val_map,
				low_map, high_map);
	}

}

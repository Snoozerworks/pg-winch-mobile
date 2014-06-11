package skarmflyg.org.gohigh.arduino;

import android.os.Parcel;

/**
 * Objects of class Parameter represents one parameter from the winch.
 * 
 * @author markus
 *
 */
public class Parameter extends DataPackage {
	static final public byte BYTE_SIZE = 35;

	public short index;
	public short val;
	public short low;
	public short high;
	public short low_map;
	public short high_map;
	public short step;

	public float val_map; // Mapped value

	private float k;
	private float m;

	public String descr;


	public Parameter() {
		super(BYTE_SIZE);
		raw = new byte[BYTE_SIZE];
	}


	public Parameter(Parcel in) {
		super(BYTE_SIZE);
		// raw = new byte[BYTE_SIZE];
		in.readByteArray(raw);
		LoadBytes();
	}


	public void LoadBytes() {
		mode = Mode.get(raw[0]);
		index = byte2short(raw[1]);
		val = byte2short(raw[2], raw[3]);
		low = byte2short(raw[4], raw[5]);
		high = byte2short(raw[6], raw[7]);
		low_map = byte2short(raw[8], raw[9]);
		high_map = byte2short(raw[10], raw[11]);
		step = byte2short(raw[12], raw[13]);

		k = (float) (high_map - low_map) / (high - low);
		m = low_map - k * low;

		val_map = Map(val);

		byte[] byte_descr = new byte[21];
		System.arraycopy(raw, 14, byte_descr, 0, 21);
		descr = new String(byte_descr);
	}


	static public float Map(short val, short low, short high, short low_map, short high_map) {
		return (float) (val - low) * (high_map - low_map) / (high - low) + low_map;
	}


	public float Map(short val) {
		return k * val + m;
	}


	@Override
	public String toString() {
		String format = "%s\nmode.......: %s\nvalue......: %d\nlim (lo|hi): (%d,%d)\nmap (lo|hi): (%d|%d)";
		return String.format(format, descr, mode.toString(), val, low, high, low_map, high_map);
	}


	public String toStringMapped() {
		String format = "%s\nmode.......: %s\nvalue......: %.1f\nlim (lo|hi): (%d|%d)";
		return String.format(format, descr, mode.toString(), val_map, low_map, high_map);
	}

}

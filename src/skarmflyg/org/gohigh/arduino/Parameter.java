package skarmflyg.org.gohigh.arduino;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Objects of class Parameter represents one parameter from the winch.
 * 
 * @author markus
 * 
 */
public class Parameter extends DataPackage {
	static final public byte BYTE_SIZE = 35;
	static final private byte PARAM_DESCR_BYTE_SIZE = 21;

	private final Mapping mapping = new Mapping();
	public int index = -1;
	public int val = 0;
	public int step = 1;
	public float val_map; // Mapped value

	public String descr;

	public Parameter() {
		super(BYTE_SIZE);
	}

	public Parameter(byte param_index) {
		super(BYTE_SIZE);
		index = param_index;
		descr = "Default parameter index " + index;
	}

	public Parameter(byte[] raw_data) {
		super(BYTE_SIZE);
		this.loadBytes(raw_data);
	}

	// public Parameter(Parcel in) {
	// super(BYTE_SIZE);
	// in.readByteArray(raw);
	// loadBytes(raw);
	// }

	public Mapping getMapping() {
		return mapping;
	}

	@Override
	public void loadBytes(byte[] bytearr) {
		// short low, high, low_map, high_map;

		if (bytearr.length < BYTE_SIZE) {
			return;
		}
		raw = Arrays.copyOf(bytearr, BYTE_SIZE);
		mode = Mode.toEnum(raw[0]);
		index = raw[1];
		val = byte2short(raw[2], raw[3]);
		step = byte2short(raw[12], raw[13]);
		mapping.setMapping( //
				byte2short(raw[4], raw[5]), //
				byte2short(raw[6], raw[7]), //
				byte2short(raw[8], raw[9]), //
				byte2short(raw[10], raw[11]));

		val_map = mapping.map(val);

		byte[] byte_descr = new byte[PARAM_DESCR_BYTE_SIZE];
		System.arraycopy(raw, 14, byte_descr, 0, PARAM_DESCR_BYTE_SIZE);

		// Translation to ISO-8859-1 to incoming bytes needs to be done.
		// Sym - Dec - Hex - ISO-8859-1
		// å - 128 - \x80 - 229
		// ä - 225 - \xE1 - 228
		// ö - 239 - \xEF - 246
		// Å - 129 - \x81 - 197
		// Ä - 130 - \x82 - 196
		// Ö - 131 - \x83 - 214

		int i = 0;
		while (i < PARAM_DESCR_BYTE_SIZE) {
			byte b = byte_descr[i];
			switch (b) {
			case (byte) 0x00:
				byte_descr[i] = (byte) 32; // Replace null character with ascii space 0x20.
				break;
			
			case (byte) 0x80:
				byte_descr[i] = (byte) 229; // å ISO-8859-1
				break;

			case (byte) 0xE1:
				byte_descr[i] = (byte) 228; // ä ISO-8859-1
				break;

			case (byte) 0xEF:
				byte_descr[i] = (byte) 246; // ö ISO-8859-1
				break;

			case (byte) 0x81:
				byte_descr[i] = (byte) 197; // Å ISO-8859-1
				break;

			case (byte) 0x82:
				byte_descr[i] = (byte) 196; // Ä ISO-8859-1
				break;

			case (byte) 0x83:
				byte_descr[i] = (byte) 214; // Ö ISO-8859-1
				break;

			}
			i++;

		}

		try {
			descr = new String(byte_descr, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Map from scaled to raw value.
	 * 
	 * @param val
	 * @return
	 */
	public int MapInverse(float val) {
		return mapping.mapInverse(val);
	}

	@Override
	public String toString() {
		String format = "(%2d) %s\nmode.......: %s\nvalue......: %d\n%s";
		return String.format(format, //
				index, //
				descr, //
				mode.toString(), //
				val, //
				mapping.toString());
	}

	public String toStringMapped() {
		String format = "(%2d) %s\nmode.......: %s\nvalue......: %.1f\nlim (lo|hi): (%d|%d)";
		return String.format(format, //
				index, //
				descr, //
				mode.toString(), //
				val_map, //
				mapping.getLowMap(), //
				mapping.getHighMap());
	}

	static public String csvHeaders() {
		return "mode,index,value,low,high,step,low_map,high_map,descr\n";
	}

	public String toCsv() {
		String format = "%d,%d,%d,%d,%d,%d,%d,%d,%s\n";
		return String.format(format, //
				mode.getByte(), //
				index, //
				val, //
				mapping.getLow(), //
				mapping.getHigh(), //
				step, //
				mapping.getLowMap(), //
				mapping.getHighMap(), //
				descr);
	}

	static public float Map(short val, short low, short high, short low_map,
			short high_map) {
		return (float) (val - low) * (high_map - low_map) / (high - low)
				+ low_map;
	}

	static public Mode getMode(byte[] raw) {
		return Mode.toEnum(raw[0]);
	}

	static public byte getIndex(byte[] raw) {
		return (byte) raw[1];
	}

	static public int getVal(byte[] raw) {
		return byte2short(raw[2], raw[3]);
	}

	static public int getLow(byte[] raw) {
		return byte2short(raw[4], raw[5]);
	}

	static public int getHigh(byte[] raw) {
		return byte2short(raw[6], raw[7]);
	}

}

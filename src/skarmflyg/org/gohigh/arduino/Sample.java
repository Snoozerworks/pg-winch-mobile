package skarmflyg.org.gohigh.arduino;

import java.util.Arrays;

/**
 * Objects of class Sample represents one sampled data from the winch.
 * 
 * 
 * @author markus
 */
public class Sample extends DataPackage {
	static final public byte BYTE_SIZE = 11;

	// Indices of parameters used for mapping.
	static final public byte PARAM_INDEX_DRUM = 0;
	static final public byte PARAM_INDEX_PUMP = 1;
	static final public byte PARAM_INDEX_TEMP_HI = 2;
	static final public byte PARAM_INDEX_TEMP_LO = 3;
	// static final public byte PARAM_INDEX_PRES = ??;

	// public short index;
	public long time;
	public short tach_pump;
	public short tach_drum;
	public short temp;
	public short pres;

	public Sample() {
		super(BYTE_SIZE);
	}

	public Sample(byte[] raw_data) {
		super(BYTE_SIZE);
		this.loadBytes(raw_data);
	}

	@Override
	public void loadBytes(byte[] bytearr) {
		if (bytearr.length < BYTE_SIZE) {
			return;
		}
		raw = Arrays.copyOf(bytearr, BYTE_SIZE);
		mode = getMode(raw);
		time = getTime(raw);
		tach_pump = getTachPump(raw);
		tach_drum = getTachDrum(raw);
		temp = getTemp(raw);
		pres = getPres(raw);
	}

	static public Mode getMode(byte[] raw) {
		return Mode.toEnum((byte) (raw[0] & 0xFF));
	}

	static public long getTime(byte[] raw) {
		return byte2long(raw[1], raw[2], raw[3], raw[4]);
	}

	static public short getTachPump(byte[] raw) {
		return byte2short(raw[5]);
	}

	static public short getTachDrum(byte[] raw) {
		return byte2short(raw[6]);
	}

	static public short getTemp(byte[] raw) {
		return byte2short(raw[7], raw[8]);
	}

	static public short getPres(byte[] raw) {
		return byte2short(raw[9], raw[10]);
	}

	@Override
	public String toString() {
		String format = "mode : %d\ntime : %d\npump : %d\ndrum : %d\ntemp : %d\npres : %d\n";
		return String.format(format, mode.getByte(), time, tach_pump,
				tach_drum, temp, pres);
	}

	static public String csvHeaders() {
		return "mode,time,pump_speed_raw,drum_speed_raw,temp,pres\n";
	}

	public String toCsv() {
		String format = "%d,%d,%d,%d,%d,%d\n";
		return String.format(format, mode.getByte(), time, tach_pump,
				tach_drum, temp, pres);
	}

}

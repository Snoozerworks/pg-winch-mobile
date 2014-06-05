package skarmflyg.org.gohigh.arduino;

public class Sample extends DataPackage {
	static final public byte BYTE_SIZE = 11;

	public short index;
	public long time;
	public short tach_pump;
	public short tach_drum;
	public short temp;
	public short pres;


	public Sample() {
		super(BYTE_SIZE);
	}


	public void LoadBytes() {
//		mode = Mode.values()[byte2short(raw[0])];
		mode = Mode.get((byte) (raw[0] & 0xFF));
		time = byte2long(raw[1], raw[2], raw[3], raw[4]);
		tach_pump = byte2short(raw[5]);
		tach_drum = byte2short(raw[6]);
		temp = byte2short(raw[7], raw[8]);
		pres = byte2short(raw[9], raw[10]);
	}


	@Override
	public String toString() {
		String format = "mode : %d\ntime : %d\npump : %d\ndrum : %d\ntemp : %d\npres : %d\n";
		return String.format(format, mode.getByte(), time, tach_pump, tach_drum, temp, pres);
	}

}

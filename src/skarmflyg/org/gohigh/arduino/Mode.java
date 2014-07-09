package skarmflyg.org.gohigh.arduino;

import java.util.EnumSet;
import android.util.SparseArray;

public enum Mode {
	NOMODE((byte) 0x00), // Undefined mode.
	STARTUP((byte) 0x01), // Startup checks
	CONFIG_IS((byte) 0x02), // Set parameters (installation parameters)
	CONFIG_OS((byte) 0x03), // Set parameters (operation parameters)
	IDLE((byte) 0x04), // Winch in stand by (lever in neutral)
	TOWING((byte) 0x05); // In towing operation (lever not in neutral)

	private byte byteVal;

	// Lookup table
	private static final SparseArray<Mode> lookup = new SparseArray<Mode>();

	// Populate the lookup table on loading time
	static {
		for (Mode s : EnumSet.allOf(Mode.class))
			lookup.append(s.getByte(), s);
	}

	/**
	 * Constructor
	 * 
	 * @param mode
	 */
	private Mode(byte mode) {
		byteVal = mode;
	}

	public byte getByte() {
		return byteVal;
	}

	public Boolean equals(byte b) {
		return b == byteVal;
	}

	// This method can be used for reverse lookup purpose
	public static Mode get(byte mode) {
		// Mode m = lookup.get(mode);
		Mode m = (Mode) lookup.get(mode);
		return (m == null) ? NOMODE : m;
	}

}

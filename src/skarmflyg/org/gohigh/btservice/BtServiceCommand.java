package skarmflyg.org.gohigh.btservice;

import java.util.EnumSet;
import android.util.SparseArray;

public enum BtServiceCommand {
	CONNECT, DISCONNECT, KILL, TIMEOUT, STOP, UP, DOWN, SELECT, SETP, //
	GET_SAMPLE, GET_SAMPLES, GET_PARAMETER, GET_PARAMETERS, GET_STATE, _READ;

	private int val;

	// Lookup table
	private static final SparseArray<BtServiceCommand> lookup = new SparseArray<BtServiceCommand>();

	// Populate the lookup table on loading time
	static {
		for (BtServiceCommand s : EnumSet.allOf(BtServiceCommand.class))
			lookup.put(s.Value(), s);
	}


	private BtServiceCommand() {
		val = this.ordinal();
	}


	public int Value() {
		return val;
	}


	/**
	 * Lookup command given its value v. Returns null if v doesn't exist.
	 * 
	 * @param v
	 *            Command value
	 * @return Command or null of not found
	 */
	static public BtServiceCommand get(int v) {
		return lookup.get(v);
	}


	public boolean Equal(int v) {
		return v == val;
	}

}

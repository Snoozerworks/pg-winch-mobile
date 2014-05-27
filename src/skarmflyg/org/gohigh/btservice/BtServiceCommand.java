package skarmflyg.org.gohigh.btservice;

import java.util.EnumSet;
import android.util.SparseArray;

public enum BtServiceCommand {
	CONNECT, DISCONNECT, TIMEOUT, UP, DOWN, SET, GET_SAMPLE, GET_SAMPLES, GET_PARAMETER, GET_PARAMETERS, KILL;

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


	// This method can be used for reverse lookup purpose
	static public BtServiceCommand get(int v) {
		return lookup.get(v);
	}


	public boolean Equal(int v) {
		return v == val;
	}

}

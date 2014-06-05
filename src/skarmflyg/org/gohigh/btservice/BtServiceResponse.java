package skarmflyg.org.gohigh.btservice;

import java.util.EnumSet;
import android.util.SparseArray;

public enum BtServiceResponse {
	STATE_CONNECTED, STATE_DISCONNECTED, STATE_SAMPELS, STATE_SYNCS, STATE_STOPPED, //
	PARAMETER_RECEIVED, SAMPLE_RECEIVED, PACKAGE_TIMEOUT, ANS_TXT, //
	HANDLER_SET, HANDLER_UNSET;

	private int val;

	// Lookup table
	private static final SparseArray<BtServiceResponse> lookup = new SparseArray<BtServiceResponse>();

	// Populate the lookup table on loading time
	static {
		for (BtServiceResponse s : EnumSet.allOf(BtServiceResponse.class))
			lookup.put(s.Value(), s);
	}


	private BtServiceResponse() {
		val = this.ordinal();
	}


	public int Value() {
		return val;
	}


	// This method can be used for reverse lookup purpose
	static public BtServiceResponse get(int v) {
		return lookup.get(v);
	}


	public boolean Equal(int v) {
		return v == val;
	}

}

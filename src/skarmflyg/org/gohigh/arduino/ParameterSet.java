package skarmflyg.org.gohigh.arduino;

import android.util.SparseArray;

public class ParameterSet extends SparseArray<Parameter> {

	public void put(Parameter p) {
		this.put(p.index, p);
	}


	/**
	 * Check if a parameter with same index exists in set.
	 * 
	 * @param p
	 *            Parameter to check
	 * @return True if exists.
	 */
	public boolean exists(Parameter p) {
		try {
			this.valueAt(p.index);
		} catch (Exception e) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		String s = "";
		int count = this.size();
		for (int i = 0; i < count; i++) {
			Parameter obj = (Parameter) this.valueAt(i);
			s += Integer.toString(obj.index) + "-" + obj.toString() + "\n";
		}
		return s;
	}
}

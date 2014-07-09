package skarmflyg.org.gohigh.arduino;

import android.util.SparseArray;

public class ParameterSet {
	private SparseArray<Parameter> arr = new SparseArray<>(0);

	/**
	 * Add parameter to set. Update if parameter already exist.
	 * 
	 * @param p
	 */
	public void put(Parameter p) {
		Parameter p_in_arr = arr.get(p.index);
		if (p_in_arr == null) {
			arr.put(p.index, p);
		} else {
			p_in_arr.LoadBytes(p.raw);
		}
	}

	/**
	 * Get parameter given it index.
	 * 
	 * @param index
	 * @return
	 */
	public Parameter get(byte index) {
		return arr.get(index);
	}

	/**
	 * Check if a parameter with same index exists in set.
	 * 
	 * @param p
	 *            Parameter to check
	 * @return True if exists.
	 */
	public boolean exists(Parameter p) {
		return (arr.get(p.index) != null);
	}

	@Override
	public String toString() {
		String s = "";
		int count = arr.size();
		for (int i = 0; i < count; i++) {
			Parameter obj = (Parameter) arr.valueAt(i);
			s += Integer.toString(obj.index) + "-" + obj.toString() + "\n";
		}
		return s;
	}
}

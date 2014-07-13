package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.arduino.Parameter;
import android.util.SparseArray;

public class ParameterSet {
	private SparseArray<Parameter> param_arr = new SparseArray<>(0);

	/**
	 * Add parameter to set. Update if parameter already exist.
	 * 
	 * @param p
	 */
	public void put(Parameter p) {
		Parameter p_in_arr = param_arr.get(p.index);
		if (p_in_arr == null) {
			param_arr.put(p.index, p);
		} else {
			p_in_arr.loadBytes(p.raw);
		}
	}

	/**
	 * Get parameter given it index.
	 * 
	 * @param index
	 * @return
	 */
	public Parameter get(byte index) {
		return param_arr.get(index);
	}

	/**
	 * Check if a parameter with same index exists in set.
	 * 
	 * @param p
	 *            Parameter to check
	 * @return True if exists.
	 */
	public boolean exists(Parameter p) {
		return (param_arr.get(p.index) != null);
	}

	@Override
	public String toString() {
		String s = "";
		int count = param_arr.size();
		for (int i = 0; i < count; i++) {
			Parameter obj = (Parameter) param_arr.valueAt(i);
			s += Integer.toString(obj.index) + "-" + obj.toString() + "\n";
		}
		return s;
	}

	public String toCSV() {
		String s = "";
		int count = param_arr.size();
		for (int i = 0; i < count; i++) {
			Parameter parameter = (Parameter) param_arr.valueAt(i);
			s += parameter.toCsv();
		}
		return s;
	}
}

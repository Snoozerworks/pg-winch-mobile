package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.ParameterSet;
import skarmflyg.org.gohigh.arduino.Sample;
import com.androidplot.xy.XYSeries;

public class SampleStore {
	private Sample[] samples_arr;
	private int head;
	private int count;
	private ParameterSet parameters;

	// Max store size
	private static int MAX_SIZE;

	public SampleStore(int size) {
		if (size < 1) {
			throw new IllegalArgumentException();
		}
		MAX_SIZE = size;
		head = -1; // Initial value
		count = 0;
		samples_arr = new Sample[MAX_SIZE];
		parameters = new ParameterSet();

	}

	public Parameter getParameter(byte index) {
		return parameters.get(index);
	}

	public ParameterSet getParameters() {
		return parameters;
	}

	public Sample getSample(int i) {
		return samples_arr[getPos(i)];
	}

	public int max_size() {
		return MAX_SIZE;
	}

	public int size() {
		return count;
	}

	public void add(Parameter p) {
		parameters.put(p);
	}

	public void add(Sample s) {
		if (count < MAX_SIZE) {
			count++;
		}
		head = (head + 1) % MAX_SIZE;
		samples_arr[head] = s;
	}

	public SampleXYSeries getXYSeriesDrum() {
		return new SampleXYSeries("Drum spd") {
			@Override
			public Number getY(int arg0) {
				return mapping.Map(samples_arr[getPos(arg0)].tach_drum);
			}
		};
	}

	public SampleXYSeries getXYSeriesPump() {
		return new SampleXYSeries("Pump spd") {
			@Override
			public Number getY(int arg0) {
				return mapping.Map(samples_arr[getPos(arg0)].tach_pump);
			}
		};
	}

	public SampleXYSeries getXYSeriesTemp() {
		return new SampleXYSeries("Oil temp") {
			@Override
			public Number getY(int arg0) {
				return mapping.Map(samples_arr[getPos(arg0)].temp);
			}
		};
	}

	public SampleXYSeries getXYSeriesPres() {
		return new SampleXYSeries("Pressure") {
			@Override
			public Number getY(int arg0) {
				return mapping.Map(samples_arr[getPos(arg0)].pres);
			}
		};
	}

	public SampleXYSeries getXYSeriesMode() {
		return new SampleXYSeries("Mode") {
			@Override
			public Number getY(int arg0) {
				return samples_arr[getPos(arg0)].mode.getByte();
			}
		};
	}

	private int getPos(int i) {
		return (MAX_SIZE + head - i) % MAX_SIZE;
	}

	/**
	 * Base class to extent to create XYSeries different sample data fields.
	 * 
	 * @author markus
	 * 
	 */
	public abstract class SampleXYSeries implements XYSeries {
		private String title;
		protected Parameter mapping;

		/**
		 * Create XYSeries for plotting.
		 * 
		 * @param t
		 *            Name of series.
		 * @param param_index
		 *            Index of parameter used to map.
		 */
		public SampleXYSeries(String t) {
			title = t;
			mapping = new Parameter();
		}

		public void SetMapping(Parameter p) {
			mapping = p;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public Number getX(int index) {
			return index;
		}

		@Override
		public int size() {
			return count;
		}

	}
}

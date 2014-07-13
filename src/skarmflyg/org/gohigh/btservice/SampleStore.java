package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.arduino.Mapping;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import com.androidplot.xy.XYSeries;

public class SampleStore {
	protected ParameterSet parameters;

	private Sample[] samples_arr;
	private int count;
	private int head;

	private Parameter param_drum;
	private Parameter param_pump;
	private Parameter param_temp;
	private Parameter param_oil_lo;

	// Max store size
	private static int MAX_SIZE;

	public SampleStore(int size) {
		if (size < 1) {
			throw new IllegalArgumentException();
		}
		MAX_SIZE = size;
		head = -1; // Initial value. First sample will be 0.
		count = 0;
		samples_arr = new Sample[MAX_SIZE];
		parameters = new ParameterSet();

		// Add default parameters for those used for plotting.
		param_drum = new Parameter(Sample.PARAM_INDEX_DRUM);
		param_pump = new Parameter(Sample.PARAM_INDEX_PUMP);
		param_temp = new Parameter(Sample.PARAM_INDEX_TEMP_HI);
		param_oil_lo = new Parameter(Sample.PARAM_INDEX_TEMP_LO);

		parameters.put(param_drum);
		parameters.put(param_pump);
		parameters.put(param_temp);
		parameters.put(param_oil_lo);
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

	public void add(Sample sample) {
		if (count < MAX_SIZE) {
			count++;
		}
		head = (head + 1) % MAX_SIZE;
		samples_arr[head] = sample;
	}

	/**
	 * Record time between current and previous sample to detect lost packages.
	 * 
	 * @return
	 */
	public long getDeltaTime() {
		if (count < 2) {
			return 0;
		}
		return getSample(0).time - getSample(1).time;
	}

	private int getPos(int i) {
		return (MAX_SIZE + head - i) % MAX_SIZE;
	}

	public SampleXYSeries getXYSeriesDrum() {
		return new SampleXYSeries("Drum spd", param_drum.getMapping()) {
			@Override
			public Number getY(int arg0) {
				return map(samples_arr[getPos(arg0)].tach_drum);
			}
		};
	}

	public SampleXYSeries getXYSeriesPump() {
		return new SampleXYSeries("Pump spd", param_pump.getMapping()) {
			@Override
			public Number getY(int arg0) {
				return map(samples_arr[getPos(arg0)].tach_pump);
			}
		};
	}

	public SampleXYSeries getXYSeriesTemp() {
		return new SampleXYSeries("Oil temp", param_temp.getMapping()) {
			@Override
			public Number getY(int arg0) {
				return map(samples_arr[getPos(arg0)].temp);
			}
		};
	}

	public SampleXYSeries getXYSeriesPres() {
		return new SampleXYSeries("Pressure") {
			@Override
			public Number getY(int arg0) {
				return map(samples_arr[getPos(arg0)].pres);
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

	/**
	 * Base class to extent to create XYSeries different sample data fields.
	 * 
	 * @author markus
	 * 
	 */
	public abstract class SampleXYSeries implements XYSeries {
		private String title;
		protected Mapping mapping = new Mapping();

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
		}

		public SampleXYSeries(String t, Mapping m) {
			title = t;
			setMapping(m);
		}

		/**
		 * Mapping to use by map().
		 * 
		 * @param m
		 */
		public void setMapping(Mapping m) {
			if (m != null) {
				mapping = m;
			}
		}

		protected float map(int val) {
			return mapping.map(val);
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

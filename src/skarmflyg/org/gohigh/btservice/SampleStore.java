package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.arduino.Sample;
import com.androidplot.xy.XYSeries;

public class SampleStore {
	private Sample[] samples_arr;
	private int head;
	private int count;

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

	public void add(Sample s) {
		if (count < MAX_SIZE) {
			count++;
		}
		head = (head + 1) % MAX_SIZE;
		samples_arr[head] = s;
	}

	public XYSeries getXYSeriesDrum() {
		return new SampleXYSeries("Drum spd") {
			@Override
			public Number getY(int arg0) {
				return samples_arr[getPos(arg0)].tach_drum;
			}
		};
	}

	public XYSeries getXYSeriesPump() {
		return new SampleXYSeries("Pump spd") {
			@Override
			public Number getY(int arg0) {
				return samples_arr[getPos(arg0)].tach_pump;
			}
		};
	}

	public XYSeries getXYSeriesTemp() {
		return new SampleXYSeries("Oil temp") {
			@Override
			public Number getY(int arg0) {
				return samples_arr[getPos(arg0)].temp;
			}
		};
	}

	public XYSeries getXYSeriesPres() {
		return new SampleXYSeries("Pressure") {
			@Override
			public Number getY(int arg0) {
				return samples_arr[getPos(arg0)].pres;
			}
		};
	}

	public XYSeries getXYSeriesMode() {
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
	abstract class SampleXYSeries implements XYSeries {
		String title;

		public SampleXYSeries(String t) {
			title = t;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public Number getX(int index) {
			return index;
			// return samples_arr[getPos(index)].time;
		}

		@Override
		public int size() {
			return count;
		}

	}
}

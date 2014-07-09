package skarmflyg.org.gohigh;

import java.text.DecimalFormat;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BTService;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.BtServiceStatus;
import skarmflyg.org.gohigh.btservice.SampleStore;
import skarmflyg.org.gohigh.btservice.SampleStore.SampleXYSeries;
import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

public class SampleAct extends BaseAct {

	private com.androidplot.xy.XYPlot dynamicPlot;
	private TextView txt;
	private BtServiceListener listener;
	private SampleXYSeries drumSeries;
	private SampleXYSeries pumpSeries;
	private SampleXYSeries tempSeries;

	// private SampleXYSeries presSeries;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_sample);
		super.onCreate(savedInstanceState);

		dynamicPlot = (XYPlot) findViewById(id.chart);

		// only display whole numbers in domain labels
		dynamicPlot.getGraphWidget().setDomainValueFormat(
				new DecimalFormat("0"));
		dynamicPlot.setRangeBoundaries(-256, 256, BoundaryMode.FIXED);

		dynamicPlot.setDomainStepValue(BTService.GRAPH_SAMPEL_COUNT);
		dynamicPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
		dynamicPlot.setTicksPerDomainLabel(2);

		dynamicPlot.setTicksPerRangeLabel(3);

		listener = new ServiceListener();

	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder binder) {
		super.onServiceConnected(className, binder);

		// Add data series to plot
		SampleStore sampel_data = btService.getSampleStore();

		drumSeries = sampel_data.getXYSeriesDrum();
		pumpSeries = sampel_data.getXYSeriesPump();
		tempSeries = sampel_data.getXYSeriesTemp();

		dynamicPlot.addSeries(drumSeries, //
				new LineAndPointFormatter(Color.BLUE, null, null, null));
		dynamicPlot.addSeries(pumpSeries, //
				new LineAndPointFormatter(Color.RED, null, null, null));
		dynamicPlot.addSeries(tempSeries, //
				new LineAndPointFormatter(Color.RED, null, null, null));
	}

	@Override
	TextView getTextView() {
		return (TextView) findViewById(id.txt_log);
	}

	@Override
	BtServiceListener getBtListener() {
		return listener;
	}

	/**
	 * Handle BtService events.
	 * 
	 * @author markus
	 * 
	 */
	private class ServiceListener implements BtServiceListener {

		@Override
		public void onText(CharSequence s) {
			logTxt(txt, s.toString());
		}

		@Override
		public void onSampleReceived(Sample s) {
			dynamicPlot.redraw();
		}

		@Override
		public void onParameterReceived(Parameter p) {

			switch (p.index) {
			case Sample.PARAM_INDEX_DRUM:
				drumSeries.SetMapping(p);
				break;

			case Sample.PARAM_INDEX_PUMP:
				pumpSeries.SetMapping(p);
				break;

			case Sample.PARAM_INDEX_TEMP:
				tempSeries.SetMapping(p);
				break;

			}

		}

		@Override
		public void onStatusChange(BtServiceStatus status) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onPackageTimeout() {
			logTxt(txt, "Package timeout");

		}

		@Override
		public void onConnectionTimeout() {
			logTxt(txt, "Connection timeout");
		}
	};

}

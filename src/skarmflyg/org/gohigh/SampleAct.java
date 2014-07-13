package skarmflyg.org.gohigh;

import java.text.DecimalFormat;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.ServiceState;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_sample);
		txt = (TextView) findViewById(id.txt_log);

		super.onCreate(savedInstanceState);

		dynamicPlot = (XYPlot) findViewById(id.chart);

		// only display whole numbers in domain labels
		dynamicPlot.setDomainValueFormat(new DecimalFormat("0"));
		dynamicPlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 5);

		dynamicPlot.setRangeBoundaries(0, 1000, BoundaryMode.FIXED);
		dynamicPlot.setRangeValueFormat(new DecimalFormat("0"));
		dynamicPlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 100);
		dynamicPlot.setTicksPerRangeLabel(1);

		listener = new ServiceListener();
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder binder) {
		super.onServiceConnected(className, binder);

		// Get data series
		SampleStore sampel_data = btService.getSampleStore();
		drumSeries = sampel_data.getXYSeriesDrum();
		pumpSeries = sampel_data.getXYSeriesPump();
		tempSeries = sampel_data.getXYSeriesTemp();

		// Add series to plot
		LineAndPointFormatter drumFormat = new LineAndPointFormatter();
		drumFormat.configure(getApplicationContext(), R.xml.plf);
		drumFormat.getLinePaint().setColor(Color.RED);
		dynamicPlot.addSeries(drumSeries, drumFormat);

		LineAndPointFormatter pumpFormat = new LineAndPointFormatter();
		pumpFormat.configure(getApplicationContext(), R.xml.plf);
		pumpFormat.getLinePaint().setColor(Color.GREEN);
		dynamicPlot.addSeries(pumpSeries, pumpFormat);

		LineAndPointFormatter tempFormat = new LineAndPointFormatter();
		tempFormat.configure(getApplicationContext(), R.xml.plf);
		tempFormat.getLinePaint().setColor(Color.BLUE);
		dynamicPlot.addSeries(tempSeries, tempFormat);

	}

	@Override
	TextView getTextView() {
		return txt;
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
			long dt = btService.getSampleStore().getDeltaTime();
			logTxtSet(txt, "period: " + Long.toString(dt) + "ms");
		}

		@Override
		public void onParameterReceived(Parameter p) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStateChange(ServiceState state) {
			logTxt(txt, "State changed to " + state.toString());
		}

		@Override
		public void onPackageTimeout() {
			logTxt(txt, "Package timeout");

		}

		@Override
		public void onConnectionTimeout() {
			logTxt(txt, "Connection timeout");
		}

		@Override
		public void onRecordStateChange(boolean is_recording) {
			// TODO Auto-generated method stub

		}

		@Override
		public String getId() {
			return "SampleActListener";
		}
	};

}

package skarmflyg.org.gohigh;

import java.text.DecimalFormat;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.btservice.BTService;
import skarmflyg.org.gohigh.btservice.BtServiceResponse;
import skarmflyg.org.gohigh.btservice.SampleStore;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class SampleAct extends BaseAct {

	// private static final int GRAPH_SAMPEL_COUNT = 50;
	// private static final int FILE_SAMPEL_COUNT = 1500;

	private static com.androidplot.xy.XYPlot dynamicPlot;

	// private static SampleStoreFile sampel_data;

	private BtResponseHandler btResponesHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_sample);

		// Create bluetooth service handler
		btResponesHandler = new BtResponseHandler();

		dynamicPlot = (XYPlot) findViewById(id.chart);

		// Create data series
		// sampel_data = new SampleStoreFile(GRAPH_SAMPEL_COUNT,
		// FILE_SAMPEL_COUNT);

		// Add data series to plot
		SampleStore sampel_data = btService.getSampleStore();
		dynamicPlot.addSeries(sampel_data.getXYSeriesDrum(), //
				new LineAndPointFormatter(Color.BLUE, null, null, null));
		dynamicPlot.addSeries(sampel_data.getXYSeriesPump(), //
				new LineAndPointFormatter(Color.RED, null, null, null));

		// only display whole numbers in domain labels
		dynamicPlot.getGraphWidget().setDomainValueFormat(
				new DecimalFormat("0"));
		dynamicPlot.setRangeBoundaries(-256, 256, BoundaryMode.FIXED);

		dynamicPlot.setDomainStepValue(BTService.GRAPH_SAMPEL_COUNT);
		dynamicPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
		dynamicPlot.setTicksPerDomainLabel(2);

		dynamicPlot.setTicksPerRangeLabel(3);

		super.onCreate(savedInstanceState);
	}

	@Override
	Handler getBtResponseHandler() {
		return btResponesHandler;
	}

	@Override
	TextView getTextView() {
		return (TextView) findViewById(id.txt_log);
	}

	static private class BtResponseHandler extends Handler {
		public void handleMessage(Message msg) {

			// TODO Messages from bt service should be passed to BaseAct too...
			// This is ugly.
			BtServiceResponse reported_state = BtServiceResponse.get(msg.what);

			// logTxt("State: " + reported_state.toString());

			switch (reported_state) {
			case STATE_SAMPELS:
			case STATE_SYNCS:
			case STATE_CONNECTED:
			case STATE_DISCONNECTED:
			case PACKAGE_TIMEOUT:
			case PARAMETER_RECEIVED:
			case ANS_TXT:
			case STATE_STOPPED:
				break;

			case SAMPLE_RECEIVED:
				dynamicPlot.redraw();

				break;
			}
		}
	}

}

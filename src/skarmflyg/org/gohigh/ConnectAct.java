package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BTService;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.ServiceState;
import skarmflyg.org.gohigh.widgets.Digits;
import skarmflyg.org.gohigh.widgets.Meter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ConnectAct extends BaseAct {

	private Meter viewForceMeter;
	private Digits viewTempDigits;
	private Digits viewDrumDigits;
	private Digits viewPumpDigits;

	private ToggleButton viewBtnConnect;
	private ToggleButton viewBtnSample;
	private Button viewBtnSettingAct;
	private ToggleButton viewBtnSync;

	private TextView txt;

	private BtServiceListener listener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_connect);

		// Get views
		txt = (TextView) findViewById(id.txt_log);
		viewForceMeter = (Meter) findViewById(id.force_meter);
		viewTempDigits = (Digits) findViewById(id.temperature);
		viewDrumDigits = (Digits) findViewById(id.drum_spd);
		viewPumpDigits = (Digits) findViewById(id.pump_spd);
		viewBtnConnect = (ToggleButton) findViewById(id.btn_connect);
		viewBtnSample = (ToggleButton) findViewById(id.btn_log);
		viewBtnSync = (ToggleButton) findViewById(id.btn_op_load);
		viewBtnSettingAct = (Button) findViewById(id.btn_settings_act);

		// Connect click listeners
		viewBtnConnect.setOnClickListener(on.clickConnectBtn);
		viewBtnSample.setOnClickListener(on.clickSampleBtn); // setOnClickListener(onClickSampleBtn);
		viewBtnSync.setOnClickListener(on.clickSyncBtn);
		viewBtnSettingAct.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goSettings();
			}
		});

		viewForceMeter.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				goGraph();
			}
		});

		listener = new ServiceListener();

		super.onCreate(savedInstanceState);

	}

	@Override
	public void onBackPressed() {
		// Only stop service when going back from ConnectAct.
		startService(new Intent(this, BTService.class)
				.setAction(BTService.ACTION_KILL_SERVICE));
		super.onBackPressed();
	}

	public void goGraph() {
		startActivity(new Intent(this, SampleAct.class));
	}

	public void goSettings() {
		startActivity(new Intent(this, ParameterAct.class));
	}

	@Override
	TextView getTextView() {
		return (TextView) findViewById(id.txt_log);
	}

	private void zeroMeters() {
		viewDrumDigits.setValue(0);
		viewPumpDigits.setValue(0);
		viewTempDigits.setValue(0);
		viewForceMeter.setValue(0);
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
		public void onSampleReceived(Sample sample) {
			logTxtSet(txt, sample.toString());
			long dt = btService.getSampleStore().getDeltaTime();
			logTxt(txt, "\nperiod: " + Long.toString(dt) + "ms");

			viewForceMeter.setTargetValue(sample.pres);
			viewTempDigits.setTargetValue(sample.temp);
			viewDrumDigits.setTargetValue(sample.tach_drum);
			viewPumpDigits.setTargetValue(sample.tach_pump);
		}

		@Override
		public void onParameterReceived(Parameter param) {
			logTxtSet(txt, param.toString());

			switch (param.index) {
			case Sample.PARAM_INDEX_DRUM:
				viewDrumDigits.setMapping(param.getMapping());
				viewDrumDigits.setHighWarning(param.val);
				break;

			case Sample.PARAM_INDEX_PUMP:
				viewPumpDigits.setMapping(param.getMapping());
				viewPumpDigits.setHighWarning(param.getMapping().mapInverse(
						2600));
				break;

			case Sample.PARAM_INDEX_TEMP_HI:
				viewTempDigits.setHighWarning(param.val);
				viewTempDigits.setMapping(param.getMapping());
				break;

			case Sample.PARAM_INDEX_TEMP_LO:
				viewTempDigits.setLowWarning(param.val);
				break;

			default:
				break;
			}

		}

		@Override
		public void onText(CharSequence s) {
			logTxt(txt, s.toString());
		}

		@Override
		public void onStateChange(ServiceState status) {
			switch (status) {
			case STATE_DISCONNECTED:
				logTxt(txt, "Bluetooth nerkopplad.");
				viewBtnConnect.setChecked(false);
				viewBtnSample.setVisibility(View.INVISIBLE);
				viewBtnSync.setVisibility(View.INVISIBLE);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;

			case STATE_CONNECTED:
				viewBtnConnect.setChecked(true);
				logTxt(txt, "Bluetooth uppkopplad.");

			case STATE_STOPPED:
				viewBtnSample.setVisibility(View.VISIBLE);
				viewBtnSample.setChecked(false);
				viewBtnSync.setVisibility(View.VISIBLE);
				viewBtnSync.setChecked(false);
				viewBtnSettingAct.setVisibility(View.VISIBLE);
				zeroMeters();
				break;

			case STATE_SAMPELS:
				viewBtnSample.setChecked(true);
				viewBtnSample.setVisibility(View.VISIBLE);
				viewBtnSync.setVisibility(View.INVISIBLE);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;

			case STATE_SYNCS:
				viewBtnSample.setVisibility(View.INVISIBLE);
				viewBtnSync.setVisibility(View.VISIBLE);
				viewBtnSync.setChecked(true);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;

			default:
				break;
			}

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

		}

		@Override
		public String getId() {
			return "ConnectActServiceListener";
		}
	}
}

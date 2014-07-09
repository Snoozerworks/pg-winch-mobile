package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.BtServiceStatus;
import skarmflyg.org.gohigh.widgets.Digits;
import skarmflyg.org.gohigh.widgets.Meter;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ConnectAct extends BaseAct {

	private Meter viewPressureGauge;
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
		viewPressureGauge = (Meter) findViewById(id.meter1);
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

		viewPressureGauge.setOnClickListener(new OnClickListener() {

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
		stopService(serviceIntent);
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
		viewTempDigits.setTargetVal(0);
		viewDrumDigits.setTargetVal(0);
		viewTempDigits.setTargetVal(0);
		viewPressureGauge.setTargetVal(0);

	}

	private void applyParameters() {
		Parameter p;

		p = btService.getParameter(Sample.PARAM_INDEX_DRUM);
		if (null != p) {
			viewDrumDigits.mapping.setHighWarning(p.val);
			viewDrumDigits.mapping.setMapping(p);
		}

		p = btService.getParameter(Sample.PARAM_INDEX_PUMP);
		if (null != p) {
			viewPumpDigits.mapping.setHighWarning(p.val);
			viewPumpDigits.mapping.setMapping(p);

		}

		p = btService.getParameter(Sample.PARAM_INDEX_TEMP);
		if (null != p) {
			viewTempDigits.mapping.setHighWarning(p.val);
			viewTempDigits.mapping.setMapping(p);

		}

		p = btService.getParameter((byte) 3);
		if (null != p) {
			viewTempDigits.mapping.setLowWarning(p.val);
		}

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
		public void onSampleReceived(Sample s) {
			logTxtSet(txt, s.toString());
			viewPressureGauge.setTargetVal(s.pres);
			viewTempDigits.setTargetVal(s.temp);
			viewDrumDigits.setTargetVal(s.tach_drum);
			viewPumpDigits.setTargetVal(s.tach_pump);
		}

		@Override
		public void onParameterReceived(Parameter param) {
			logTxtSet(txt, param.toString());
			applyParameters();
		}

		@Override
		public void onText(CharSequence s) {
			logTxt(txt, s.toString());
		}

		@Override
		public void onStatusChange(BtServiceStatus status) {
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
	};

	@Override
	public void onServiceConnected(ComponentName className, IBinder binder) {
		super.onServiceConnected(className, binder);

	}
}

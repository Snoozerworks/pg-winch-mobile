package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BtServiceResponse;
import skarmflyg.org.gohigh.widgets.Digits;
import skarmflyg.org.gohigh.widgets.Meter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ConnectAct extends BaseAct {

	static private Meter viewPressureGauge;
	static private Digits viewTempDigits;
	static private Digits viewDrumDigits;
	static private Digits viewPumpDigits;

	static private ToggleButton viewBtnConnect;
	static private ToggleButton viewBtnSample;
	static private Button viewBtnSettingAct;
	static private ToggleButton viewBtnSync;

	private BtResponseHandler btServiceHandler; // Message handler for


	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_connect);

		// Create bluetooth service handler
		btServiceHandler = new BtResponseHandler();

		// Get views
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

	@Override
	Handler getBtResponseHandler() {
		return btServiceHandler;
	}

	static private class BtResponseHandler extends Handler {
		public void handleMessage(Message msg) {

			// TODO Messages from bt service should be passed to BaseAct too...
			// This is ugly.
			BtServiceResponse reported_state = BtServiceResponse.get(msg.what);

			switch (reported_state) {
			case STATE_DISCONNECTED:
				logTxt("Bluetooth nerkopplad.");
				viewBtnConnect.setChecked(false);
				viewBtnSample.setVisibility(View.INVISIBLE);
				viewBtnSync.setVisibility(View.INVISIBLE);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;

			case STATE_CONNECTED:
				viewBtnConnect.setChecked(true);
				logTxt("Bluetooth uppkopplad.");

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

//			case HANDLER_SET:
//				logTxt(txtBtServiceConnected.toString());
//				break;
//
//			case HANDLER_UNSET:
//				logTxt(txtBtServiceDisconnected.toString());
//				break;

			case PACKAGE_TIMEOUT:
				logTxt("Package timeout.");
				break;

			case PARAMETER_RECEIVED:
				Parameter param = new Parameter();
				param.LoadBytes((byte[]) msg.obj);
				if (parameters.get(param.index) != null) {
					stopGetting();
					logTxt("Parameters synronized.");
					applyParameters();

				} else {
					logTxtSet(param.toString());
					parameters.put(param);
				}
				break;

			case SAMPLE_RECEIVED:
				Sample sam = new Sample();
				sam.LoadBytes((byte[]) msg.obj);
				logTxtSet(sam.toString());
				viewPressureGauge.setTargetVal(sam.pres);
				viewTempDigits.setTargetVal(sam.temp);
				viewDrumDigits.setTargetVal(sam.tach_drum);
				viewPumpDigits.setTargetVal(sam.tach_pump);
				break;

			case ANS_TXT:
				logTxt(msg.obj.toString());
				break;

			default:
				break;
			}

		};

	};

	static private void zeroMeters() {
		viewTempDigits.setTargetVal(0);
		viewDrumDigits.setTargetVal(0);
		viewTempDigits.setTargetVal(0);
		viewPressureGauge.setTargetVal(0);

	}

	static private void applyParameters() {
		short i = 0;
		Parameter p;
		while ((p = parameters.get(i++)) != null) {
			switch (p.index) {
			case 0:
				viewDrumDigits.mapping.setHighWarning(p.val);
				viewDrumDigits.mapping.setMapping(p);
				break;
			case 1:
				viewPumpDigits.mapping.setHighWarning(p.val);
				viewPumpDigits.mapping.setMapping(p);
				break;
			case 2:
				viewTempDigits.mapping.setHighWarning(p.val);
				viewTempDigits.mapping.setMapping(p);
				break;
			case 3:
				viewTempDigits.mapping.setLowWarning(p.val);
				break;
			}
		}

	}

}

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

public class ConnectAct extends BaseAct {

	static private Meter viewPressureGauge;
	static private Digits viewTempDigits;
	static private Digits viewDrumDigits;
	static private Digits viewPumpDigits;

	static private Button viewBtnConnect;
	static private Button viewBtnSample;
	static private Button viewBtnSettingAct;
	static private Button viewBtnSync;

	static private CharSequence txtBtnConnect;
	static private CharSequence txtBtnDisconnect;
	static private CharSequence txtBtServiceConnected = "_Service bound";
	static private CharSequence txtBtServiceDisconnected = "_Service unbound";

	static private ConnHandler btServiceHandler; // Message handler for bluetooth service


	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_connect);

		// Create bluetooth service handler
		btServiceHandler = new ConnHandler();

		// Get texts
		txtBtnConnect = getText(R.string.btn_connect);
		txtBtnDisconnect = getText(R.string.btn_disconnect);
		txtBtServiceConnected = getText(R.string.btservice_connected);
		txtBtServiceDisconnected = getText(R.string.btservice_disconnected);

		// Get views
		viewPressureGauge = (Meter) findViewById(id.meter1);
		viewTempDigits = (Digits) findViewById(id.temperature);
		viewDrumDigits = (Digits) findViewById(id.drum_spd);
		viewPumpDigits = (Digits) findViewById(id.pump_spd);
		viewBtnConnect = (Button) findViewById(id.btn_connect);
		viewBtnSample = (Button) findViewById(id.btn_go_sample);
		viewBtnSync = (Button) findViewById(id.btn_op_load);
		viewBtnSettingAct = (Button) findViewById(id.btn_settings_act);

		// Connect click listeners
		viewBtnConnect.setOnClickListener(onClickConnectBtn);
		viewBtnSample.setOnClickListener(onClickSampleBtn);
		viewBtnSync.setOnClickListener(onClickSyncBtn);
		viewBtnSettingAct.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goSettings();
			}
		});

		super.onCreate(savedInstanceState);

	}


	@Override
	protected void onDestroy() {
		stopService(serviceIntent);
		serviceIntent = null;
		super.onDestroy();
	}


	public void goSettings() {
		startActivity(new Intent(this, ParameterAct.class));
	}


	// @Override
	// protected void onResume() {
	// setHandler(new ConnHandler());
	// super.onResume();
	// }

	// @Override
	// public void onDestroy() {
	// Log.i(this.getClass().getSimpleName(), "onDestroy");
	// doStopService();
	// super.onDestroy();
	// }

	@Override
	TextView getTextView() {
		return (TextView) findViewById(id.txt_log);
	}


	@Override
	Handler getBtServiceHandler() {
		return btServiceHandler;
	}

	static class ConnHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (BtServiceResponse.get(msg.what)) {
			case CONNECTED:
				logTxt("Bluetooth uppkopplad.");
				mode = MODES.ONLINE_STANDBY;
				break;

			case HANDLER_SET:
				// mode = (BTService.is_connected) ? MODES.STANDBY : MODES.DISCONNECTED;
				logTxt(txtBtServiceConnected.toString());
				break;

			case DISCONNECTED:
				logTxt("Bluetooth nerkopplad.");
				mode = MODES.OFFLINE;
				break;

			case HANDLER_UNSET:
				// mode = MODES.DISCONNECTED;
				logTxt(txtBtServiceDisconnected.toString());
				break;

			case PACKAGE_TIMEOUT:
				logTxt("Package timeout.");
				mode = MODES.ONLINE_STANDBY;
				break;

			case PARAMETER_RECEIVED:
				Parameter param = new Parameter();
				param.LoadBytes((byte[]) msg.obj);
				logTxtSet(param.toString());
				parameters.put(param);
				applyParameters();

				// if (mode.equals(MODES.ONLINE_GET_PARAMS)) {
				// if (parameters.exists(param)) {
				// mode = MODES.ONLINE_STANDBY;
				// applyParameters();
				// } else {
				// getParameter();
				// }
				// }
				break;

			case SAMPLE_RECEIVED:
				Sample sam = new Sample();
				sam.LoadBytes((byte[]) msg.obj);
				logTxtSet(sam.toString());
				viewPressureGauge.setTargetVal(sam.pres);
				viewTempDigits.setTargetVal(sam.temp);
				viewDrumDigits.setTargetVal(sam.tach_drum);
				viewPumpDigits.setTargetVal(sam.tach_pump);

				// if (mode.equals(MODES.ONLINE_GET_SAMPLES)) {
				// Sample sam = new Sample();
				// sam.LoadBytes((byte[]) msg.obj);
				// viewPressureGauge.setTargetVal(sam.pres);
				// viewTempDigits.setTargetVal(sam.temp);
				// viewDrumDigits.setTargetVal(sam.tach_drum);
				// viewPumpDigits.setTargetVal(sam.tach_pump);
				// getSamples();
				// logTxtSet(sam.toString());
				// }
				break;

			case ANS_TXT:
				logTxt(msg.obj.toString());
				break;

			default:
				break;
			}

			refreshBtnVisibility();

		};


		static private void refreshBtnVisibility() {
			switch (mode) {
			case OFFLINE:
				viewBtnConnect.setText(txtBtnConnect);
				viewBtnSample.setVisibility(View.INVISIBLE);
				viewBtnSync.setVisibility(View.INVISIBLE);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;
			case ONLINE_GET_SAMPLES:
				viewBtnSample.setVisibility(View.VISIBLE);
				viewBtnSync.setVisibility(View.INVISIBLE);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;
			case ONLINE_STANDBY:
				viewBtnConnect.setText(txtBtnDisconnect);
				viewBtnSample.setVisibility(View.VISIBLE);
				viewBtnSync.setVisibility(View.VISIBLE);
				viewBtnSettingAct.setVisibility(View.VISIBLE);
				break;
			case ONLINE_GET_PARAMS:
				viewBtnSample.setVisibility(View.INVISIBLE);
				viewBtnSync.setVisibility(View.VISIBLE);
				viewBtnSettingAct.setVisibility(View.INVISIBLE);
				break;
			}
		}

	};


	static protected void applyParameters() {
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

	// public void onClick(View v) {
	//
	// if (v.getId() == id.btn_connect) {
	// if (BTService.isConnected()) {
	// btService.BTDisconnect();
	// } else {
	// btService.BTConnect();
	// }
	// return;
	// }
	//
	// if (!BTService.isConnected())
	// return;
	//
	// switch (v.getId()) {
	// case id.btn_go_sample:
	// if (mode.equals(MODES.ONLINE_GET_SAMPLES)) {
	// mode = MODES.ONLINE_STANDBY;
	// } else {
	// getSample();
	// }
	// break;
	//
	// case id.btn_op_load:
	// getParameter();
	// break;
	//
	// case id.btn_settings_act:
	// startActivity(new Intent(this, ParameterAct.class));
	// break;
	//
	// }
	//
	// }
}

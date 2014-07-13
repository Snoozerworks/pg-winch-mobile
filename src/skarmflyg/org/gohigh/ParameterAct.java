package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BTService;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.ServiceState;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ParameterAct extends BaseAct {
	private static Button viewBtnSet;
	private static Button viewBtnUp;
	private static Button viewBtnDown;
	private static EditText viewEditValue;
	private static Parameter param;
	private TextView txt;

	private BtServiceListener listener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_parameter);

		// Create listener
		listener = new ServiceListener();

		// Get views
		txt = (TextView) findViewById(id.txt_log);
		viewBtnSet = (Button) findViewById(id.btn_set);
		viewBtnUp = (Button) findViewById(id.btn_up);
		viewBtnDown = (Button) findViewById(id.btn_down);
		viewEditValue = (EditText) findViewById(id.editValue);
		viewEditValue.setRawInputType(Configuration.KEYBOARD_12KEY);

		// Connect click listeners
		viewBtnSet.setOnClickListener(on.clickSetBtn);
		viewBtnUp.setOnClickListener(on.clickUpBtn);
		viewBtnDown.setOnClickListener(on.clickDownBtn);
		viewEditValue.setOnKeyListener(onEditValueEntered);

		super.onCreate(savedInstanceState);

	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder binder) {
		super.onServiceConnected(className, binder);

		Intent paramIntent = new Intent(this, BTService.class);
		paramIntent.setAction(BTService.ACTION_GET_PARAMETER);
		if (param != null) {
			// Re-retrieve last viewed parameter if any.
			paramIntent.putExtra("index", param.index);
		}
		startService(paramIntent);

	}

	OnKeyListener onEditValueEntered = new OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			float editor_val;

			if (keyCode != 66 || param == null) {
				return false;
			}

			try {
				editor_val = Float.parseFloat(viewEditValue.getText()
						.toString());
			} catch (NumberFormatException e) {
				// No number entered
				return false;
			}

			Intent paramIntent = new Intent(v.getContext(), BTService.class);
			paramIntent.setAction(BTService.ACTION_SET_PARAMETER);
			paramIntent.putExtra("index", param.index);
			paramIntent.putExtra("value", param.MapInverse(editor_val));
			startService(paramIntent);
			return false;
		}
	};

	@Override
	TextView getTextView() {
		return (TextView) findViewById(id.txt_log);
	}

	@Override
	BtServiceListener getBtListener() {
		return listener;
	}

	private class ServiceListener implements BtServiceListener {
		@Override
		public void onText(CharSequence s) {
			logTxt(txt, s.toString());
		}

		@Override
		public void onSampleReceived(Sample s) {
		}

		@Override
		public void onParameterReceived(Parameter p) {
			param = p;
			viewEditValue.setText(String.format("%.1f", p.val_map));
			logTxtSet(txt, p.toStringMapped());
		}

		@Override
		public void onStateChange(ServiceState status) {
			switch (status) {
			case STATE_CONNECTED:
				logTxt(txt, "Bluetooth uppkopplad.");
				break;

			case STATE_DISCONNECTED:
				logTxt(txt, "Bluetooth nerkopplad.");
				viewBtnSet.setVisibility(View.INVISIBLE);
				viewBtnUp.setVisibility(View.INVISIBLE);
				viewBtnDown.setVisibility(View.INVISIBLE);
				break;

			case STATE_STOPPED:
				viewBtnSet.setVisibility(View.VISIBLE);
				viewBtnUp.setVisibility(View.VISIBLE);
				viewBtnDown.setVisibility(View.VISIBLE);
				break;

			case STATE_SAMPELS:
			case STATE_SYNCS:
				viewBtnSet.setVisibility(View.INVISIBLE);
				viewBtnUp.setVisibility(View.INVISIBLE);
				viewBtnDown.setVisibility(View.INVISIBLE);
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
			// TODO Auto-generated method stub

		}

		@Override
		public String getId() {
			return "ParameterActServiceListener";
		}

	}

}
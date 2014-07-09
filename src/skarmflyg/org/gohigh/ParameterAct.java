package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.BtServiceStatus;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

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
		viewEditValue.setOnEditorActionListener(onEditValueEntered);

		super.onCreate(savedInstanceState);

	}

	private void param_set() {
		short val;
		float inp_val;

		if (param == null) {
			return;
		}

		try {
			inp_val = Float.parseFloat(viewEditValue.getText().toString());
			val = param.MapInverse(inp_val);
		} catch (NumberFormatException e) {
			// No number entered
			return;
		}

		sendParameter((byte) param.index, val);

	}

	OnEditorActionListener onEditValueEntered = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_SEND) {
				param_set();
			}
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
			viewEditValue.setText(String.format("%.1f", p.val_map));
			logTxtSet(txt, p.toStringMapped());
		}

		@Override
		public void onStatusChange(BtServiceStatus status) {
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

	}

}
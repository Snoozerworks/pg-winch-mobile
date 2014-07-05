package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.btservice.BtServiceResponse;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

	static private BtResponseHandler btServiceHandler; // Message handler for
														// bluetooth service

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_parameter);

		// Create bluetooth service handler
		btServiceHandler = new BtResponseHandler();

		// Get views
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

	// @Override
	// protected void onResume() {
	// setHandler(new ConnHandler());
	// super.onResume();
	// }

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
			case STATE_SAMPELS:
			case STATE_SYNCS:
				viewBtnSet.setVisibility(View.INVISIBLE);
				viewBtnUp.setVisibility(View.INVISIBLE);
				viewBtnDown.setVisibility(View.INVISIBLE);
				break;

			case STATE_STOPPED:
				viewBtnSet.setVisibility(View.VISIBLE);
				viewBtnUp.setVisibility(View.VISIBLE);
				viewBtnDown.setVisibility(View.VISIBLE);
				break;

			case STATE_CONNECTED:
				logTxt("Bluetooth uppkopplad.");
				break;

			case STATE_DISCONNECTED:
				logTxt("Bluetooth nerkopplad.");
				viewBtnSet.setVisibility(View.INVISIBLE);
				viewBtnUp.setVisibility(View.INVISIBLE);
				viewBtnDown.setVisibility(View.INVISIBLE);
				break;

			case PACKAGE_TIMEOUT:
				logTxtSet("Package timeout.");
				break;

			case PARAMETER_RECEIVED:
				param = new Parameter();
				param.LoadBytes((byte[]) msg.obj);
				viewEditValue.setText(String.format("%.1f", param.val_map));

				// String format =
				// "%s\nLäge.......: %d\nVärde......: %d\nGräns (min,max): (%d,%d)\nMapp (lo,hi): (%d,%d)";
				// return String.format(format, param.descr,
				// param.mode.toString(), val, low, high,
				// low_map, high_map);

				logTxtSet(param.toStringMapped());
				break;

			case ANS_TXT:
				logTxt(msg.obj.toString());
				break;

//			case HANDLER_SET:
//			case HANDLER_UNSET:
			case SAMPLE_RECEIVED:
				break;
			}

		};
	};

}

package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.btservice.BtServiceResponse;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ParameterAct extends BaseAct {
	private static Button viewBtnSet;
	private static Button viewBtnUp;
	private static Button viewBtnDown;
	private static EditText viewEditValue;

	static private ConnHandler btServiceHandler; // Message handler for bluetooth service


	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_parameter);

		// Create bluetooth service handler
		btServiceHandler = new ConnHandler();

		// Get views
		viewBtnSet = (Button) findViewById(id.btn_set);
		viewBtnUp = (Button) findViewById(id.btn_up);
		viewBtnDown = (Button) findViewById(id.btn_down);
		viewEditValue = (EditText) findViewById(id.editValue);

		// Connect click listeners
		viewBtnSet.setOnClickListener(onClickSetBtn);
		viewBtnUp.setOnClickListener(onClickUpBtn);
		viewBtnDown.setOnClickListener(onClickDownBtn);

		// Update button visibility
		refreshBtnVisibility();

		super.onCreate(savedInstanceState);

	}


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
			case DISCONNECTED:
				logTxt("Bluetooth nerkopplad.");
				mode = MODES.OFFLINE;
				break;
			case PACKAGE_TIMEOUT:
				logTxtSet("Package timeout.");
				mode = MODES.ONLINE_STANDBY;
				break;

			case PARAMETER_RECEIVED:
				mode = MODES.ONLINE_STANDBY;
				Parameter param = new Parameter();
				param.LoadBytes((byte[]) msg.obj);
				viewEditValue.setText(String.format("%d", (int) param.val_map));

				// String format =
				// "%s\nL�ge.......: %d\nV�rde......: %d\nGr�ns (min,max): (%d,%d)\nMapp (lo,hi): (%d,%d)";
				// return String.format(format, param.descr, param.mode.toString(), val, low, high,
				// low_map, high_map);

				logTxtSet(param.toStringMapped());
				break;

			case ANS_TXT:
				logTxt(msg.obj.toString());
				break;

			case HANDLER_SET:
			case HANDLER_UNSET:
			case SAMPLE_RECEIVED:
			default:
				break;
			}

			refreshBtnVisibility();

		};
	};


	static private void refreshBtnVisibility() {
		switch (mode) {
		case OFFLINE:
		case ONLINE_GET_SAMPLES:
		case ONLINE_GET_PARAMS:
			viewBtnSet.setVisibility(View.INVISIBLE);
			viewBtnUp.setVisibility(View.INVISIBLE);
			viewBtnDown.setVisibility(View.INVISIBLE);
			break;
		case ONLINE_STANDBY:
			viewBtnSet.setVisibility(View.VISIBLE);
			viewBtnUp.setVisibility(View.VISIBLE);
			viewBtnDown.setVisibility(View.VISIBLE);
			break;
		}
	}

}

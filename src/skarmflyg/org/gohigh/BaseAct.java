package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.arduino.ParameterSet;
import skarmflyg.org.gohigh.btservice.BTService;
import skarmflyg.org.gohigh.btservice.BtServiceCommand;
import skarmflyg.org.gohigh.btservice.BtServiceResponse;
import skarmflyg.org.gohigh.btservice.LocalBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

abstract public class BaseAct extends Activity implements ServiceConnection {
	enum MODES {
		OFFLINE, ONLINE_STANDBY, ONLINE_GET_SAMPLES, ONLINE_GET_PARAMS
	};

	static protected BtServiceResponse reported_state;
	static protected ParameterSet parameters; // Set of winch parameters
	static private BTService btService; // Bluetooth service
	protected Intent serviceIntent;

	static private TextView tv_txt_log;


	/**
	 * Method shall return a message handler for the bluetooth service.
	 * 
	 * @return
	 */
	abstract Handler getBtServiceHandler();


	/**
	 * Method shall return a text view which will be used for text updates.
	 * 
	 * @return
	 */
	abstract TextView getTextView();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(this.getClass().getSimpleName(), "onCreate");

		parameters = new ParameterSet();

		// Attach click listener to logging text view.
		getTextView().setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				switch (v.getId()) {
				case id.txt_log:
					logTxtSet("");
					break;
				}
			}
		});

		serviceIntent = new Intent(this, BTService.class);
		startService(serviceIntent);
		bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
	}


	@Override
	protected void onResume() {
		super.onResume();
		Log.d(this.getClass().getSimpleName(), "onResume.");
		// Log.d(this.getClass().getSimpleName(), "onResume. Mode: " + mode.toString());

		tv_txt_log = getTextView();
		logTxtSet("Resuming.");

		if (btService != null) {
			btService.winchSendCommand(BtServiceCommand.GET_STATE);
		}
		// logTxtSet("Resuming. Mode: " + mode.toString());

		// bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
		BTService.setClientHandler(getBtServiceHandler());
	}

	static class ConnHandler extends Handler {
		public void handleMessage(Message msg) {
		}
	}


	@Override
	protected void onPause() {
		Log.i(this.getClass().getSimpleName(), "onPause");

		// Free some stuff
		tv_txt_log = null;

		// unbindService(this);
		BTService.unsetClientHandler();
		super.onPause();
	}


	@Override
	protected void onDestroy() {
		Log.i(this.getClass().getSimpleName(), "onDestroy");
		unbindService(this);
		super.onDestroy();
	}


	// /**
	// * Stop the bluetooth service. Called from extending classes.
	// */
	// protected void doStopService() {
	// if (btService != null) {
	// btService.BTDisconnect();
	// }
	// stopService(serviceIntent);
	// btService = null;
	// }

	// /**
	// * Call from child class to set the handler for messages from BTservice.
	// *
	// * @param h
	// */
	// protected void setHandler(Handler h) {
	// btServiceHandler = h;
	// }

	/**
	 * Implements interface of ServiceConnection.
	 * 
	 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
	 *      android.os.IBinder)
	 */
	public void onServiceConnected(ComponentName className, IBinder binder) {
		Log.i(this.getClass().getSimpleName(), "onServiceConnected: " + className.toShortString());
		btService = ((LocalBinder<BTService>) binder).getService();
		btService.winchSendCommand(BtServiceCommand.GET_STATE);

		// btService.setClientHandler(getBtServiceHandler());
		Toast.makeText(BaseAct.this, R.string.btservice_connected, Toast.LENGTH_SHORT).show();
	}


	/**
	 * Implements interface of ServiceConnection.
	 * 
	 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
	 */
	public void onServiceDisconnected(ComponentName className) {
		Log.i(this.getClass().getSimpleName(), "onServiceDisconnected" + className.toShortString());
		// btService.unsetClientHandler();
		btService = null;
		Toast.makeText(BaseAct.this, R.string.btservice_disconnected, Toast.LENGTH_SHORT).show();
	}

	protected View.OnClickListener onClickConnectBtn = new OnClickListener() {
		public void onClick(View v) {
			if (BTService.isConnected()) {
				btService.BTDisconnect();
			} else {
				btService.BTConnect();
			}
		}
	};


	static protected void getDown() {
		btService.winchSendCommand(BtServiceCommand.DOWN);
	}


	static protected void getUp() {
		btService.winchSendCommand(BtServiceCommand.UP);
	}


	static protected void getParameter() {
		btService.winchSendCommand(BtServiceCommand.GET_PARAMETER);
	}


	static protected void getParameters() {
		parameters.clear(); // Clear current set of parameters
		btService.winchSendCommand(BtServiceCommand.GET_PARAMETERS);
	}


	static protected void stopGetting() {
		btService.winchSendCommand(BtServiceCommand.STOP);
	}


	static protected void getSamples() {
		if (reported_state != null && reported_state == BtServiceResponse.STATE_SAMPELS) {
			btService.winchSendCommand(BtServiceCommand.STOP);
		} else {
			btService.winchSendCommand(BtServiceCommand.GET_SAMPLES);
		}
	}

	protected View.OnClickListener onClickDownBtn = new OnClickListener() {
		public void onClick(View v) {
			getDown();
		}
	};
	protected View.OnClickListener onClickUpBtn = new OnClickListener() {
		public void onClick(View v) {
			getUp();
		}
	};
	protected View.OnClickListener onClickSetBtn = new OnClickListener() {
		public void onClick(View v) {
			getParameter();
		}
	};
	protected View.OnClickListener onClickSampleBtn = new OnClickListener() {
		public void onClick(View v) {
			getSamples();
		}
	};
	protected View.OnClickListener onClickSyncBtn = new OnClickListener() {
		public void onClick(View v) {
			getParameters();
		}
	};


	static protected void logTxt(String txt) {
		if (tv_txt_log == null) {
			return;
		}
		tv_txt_log.append(txt + "\n");
	}


	static protected void logTxtSet(String txt) {
		if (tv_txt_log == null)
			return;
		tv_txt_log.setText(txt + "\n");
	}

}

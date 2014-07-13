package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.btservice.BTService;
import skarmflyg.org.gohigh.btservice.BtServiceListener;
import skarmflyg.org.gohigh.btservice.BTService.BtBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

abstract public class BaseAct extends Activity implements ServiceConnection {

	// Bluetooth service
	protected BTService btService;

	// Service intent
	protected Intent serviceIntent;

	/**
	 * Method shall return a message handler for the bluetooth service.
	 * 
	 * @return
	 */
	abstract BtServiceListener getBtListener();

	/**
	 * Method shall return a text view which will be used for text updates.
	 * 
	 * @return
	 */
	abstract TextView getTextView();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.i(this.getClass().getSimpleName(), "onCreate");

		// Make service "started"
		serviceIntent = new Intent(this, BTService.class);
		startService(serviceIntent);

		// Attach click listener to logging text view.
		getTextView().setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				switch (v.getId()) {
				case id.txt_log:
					logTxtSet(getTextView(), "");
					break;
				}
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		// Log.i(this.getClass().getSimpleName(), "onResume.");

		// Bind to service
		Intent serviceIntent = new Intent(this, BTService.class);
		bindService(serviceIntent, this, BIND_AUTO_CREATE);

		logTxtSet(getTextView(), "Resuming.");
	}

	@Override
	protected void onPause() {
		Log.i(this.getClass().getSimpleName(), "onPause");

		// Unbind service
		unbindService(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.i(this.getClass().getSimpleName(), "onDestroy");
		super.onDestroy();
	}

	/**
	 * Called as a result of a call to onBind() in the service attempted to bind
	 * to.
	 * 
	 * Implements interface of ServiceConnection.
	 * 
	 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
	 *      android.os.IBinder)
	 */
	public void onServiceConnected(ComponentName className, IBinder binder) {
		Log.i(this.getClass().getSimpleName(), "onServiceConnected: "
				+ className.toShortString());

		BtBinder b = (BtBinder) binder;
		btService = b.getService();
		boolean is_set = btService.setListener(getBtListener());

		if (is_set) {
			// Get state if listener wasn't already attached.
			Intent serviceIntent = new Intent(this, BTService.class)
					.setAction(BTService.ACTION_GET_STATE);
			startService(serviceIntent);
		}

		Toast.makeText(BaseAct.this, R.string.btservice_connected,
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * Implements interface of ServiceConnection.
	 * 
	 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
	 */
	public void onServiceDisconnected(ComponentName className) {
		// Log.i(this.getClass().getSimpleName(), "onServiceDisconnected"
		// + className.toShortString());
		btService = null;
		Toast.makeText(BaseAct.this, R.string.btservice_disconnected,
				Toast.LENGTH_SHORT).show();
	}

	// Keep onClick listeners
	final protected Clickers on = new Clickers();

	/**
	 * Class to encapsulate common onClick listeners.
	 * 
	 * @author markus
	 */
	class Clickers {
		OnClickListener clickSampleBtn = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CompoundButton buttonView = (CompoundButton) v;
				Intent playIntent = new Intent(v.getContext(), BTService.class);
				if (buttonView.isChecked()) {
					playIntent.setAction(BTService.ACTION_START_SAMPLING);
				} else {
					playIntent.setAction(BTService.ACTION_STOP_SAMPLING);
				}
				startService(playIntent);
			}
		};

		OnClickListener clickSetBtn = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(v.getContext(), BTService.class)
						.setAction(BTService.ACTION_GET_PARAMETER));
			}
		};

		OnClickListener clickUpBtn = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(v.getContext(), BTService.class)
						.setAction(BTService.ACTION_WINCH_BTN_UP));
			}
		};

		OnClickListener clickDownBtn = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(v.getContext(), BTService.class)
						.setAction(BTService.ACTION_WINCH_BTN_DOWN));
			}
		};

		OnClickListener clickConnectBtn = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CompoundButton buttonView = (CompoundButton) v;
				if (buttonView.isChecked()) {
					startService(new Intent(v.getContext(), BTService.class)
							.setAction(BTService.ACTION_CONNECT));
					buttonView.setChecked(false);
				} else {
					startService(new Intent(v.getContext(), BTService.class)
							.setAction(BTService.ACTION_DISCONNECT));
					buttonView.setChecked(true);
				}

			}
		};

		OnClickListener clickSyncBtn = new OnClickListener() {
			@Override
			public void onClick(View v) {
				CompoundButton buttonView = (CompoundButton) v;
				if (buttonView.isChecked()) {
					startService(new Intent(v.getContext(), BTService.class)
							.setAction(BTService.ACTION_GET_PARAMETERS));
				} else {
					startService(new Intent(v.getContext(), BTService.class)
							.setAction(BTService.ACTION_STOP_SAMPLING));
				}
			}
		};

	}

	static protected void logTxt(TextView tv_txt_log, String txt) {
		if (tv_txt_log == null) {
			return;
		}
		tv_txt_log.append(txt + "\n");
	}

	static protected void logTxtSet(TextView tv_txt_log, String txt) {
		if (tv_txt_log == null)
			return;
		tv_txt_log.setText(txt + "\n");
	}

}

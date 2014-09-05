package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.ConnectAct;
import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class BTService extends Service {
	// Number of samples to show in graph
	public static final int GRAPH_SAMPEL_COUNT = 100;
	// Max number of samples to store in file
	private final int FILE_SAMPEL_COUNT = 5000;

	public static final String ACTION_CONNECT = "skarmflyg.org.gohigh.action.CONNECT";
	public static final String ACTION_DISCONNECT = "skarmflyg.org.gohigh.action.DISCONNECT";
	public static final String ACTION_GET_PARAMETER = "skarmflyg.org.gohigh.action.GET_PARAM";
	public static final String ACTION_GET_PARAMETERS = "skarmflyg.org.gohigh.action.GET_PARAMS";
	public static final String ACTION_SET_PARAMETER = "skarmflyg.org.gohigh.action.SET_PARAM";
	public static final String ACTION_GET_STATE = "skarmflyg.org.gohigh.action.GET_STATE";
	public static final String ACTION_KILL_SERVICE = "skarmflyg.org.gohigh.action.KILL";
	public static final String ACTION_STOP_RECORDING = "skarmflyg.org.gohigh.action.REC_OFF";
	public static final String ACTION_START_RECORDING = "skarmflyg.org.gohigh.action.REC_ON";
	public static final String ACTION_STOP_SAMPLING = "skarmflyg.org.gohigh.action.PLAY_OFF";
	public static final String ACTION_START_SAMPLING = "skarmflyg.org.gohigh.action.PLAY_ON";
	public static final String ACTION_WINCH_BTN_DOWN = "skarmflyg.org.gohigh.action.WINCH_DOWN";
	public static final String ACTION_WINCH_BTN_UP = "skarmflyg.org.gohigh.action.WINCH_UP";

	// Binder object which is used by clients to send commands to service.
	private final BtBinder btBinder = new BtBinder();

	// Bluetooth thread name
	private static final String BT_THREAD_NAME = "WinchThread";

	// Bluetooth thread.
	private static BtThread btThread;

	// Handler for messages sent from thread
	private static final BtHandlerOnMain btHandlerOnMain = new BtHandlerOnMain();

	// Receive bluetooth events.
	private static BroadcastReceiver btBroadcastReceiver;
	private static BtServiceListener btServiceResponseListener;

	// Store data
	private static SampleStoreFile sampleStore;

	// Notifier
	private static NotificationManager mNM;

	// Store context
	private static Context appContext;

	// Keep service state
	private static ServiceState serviceState = ServiceState.STATE_DISCONNECTED;

	// Unique Identification Number for the Notification. We use it on
	// Notification start, and to cancel it.
	private static final int NOTIFICATION = R.string.btservice_started;

	@Override
	public void onCreate() {
		Log.i(this.getClass().getSimpleName(), "onCreate");

		appContext = getApplicationContext();

		// Handle message from bluetooth thread.
		Context c = getApplicationContext();
		btThread = new BtThread(c, BT_THREAD_NAME, btHandlerOnMain);
		btThread.start();

		/**
		 * Broadcast receiver used to listen for when the bluetooth connects and
		 * disconnects. When the bluetooth is connected/disconnected the
		 * MSG_BT_CONNECTED/MSG_BT_DISCONNECTED is sent to the client message
		 * handler.
		 */
		btBroadcastReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
					serviceState = ServiceState.STATE_CONNECTED;
					if (btServiceResponseListener != null) {
						btServiceResponseListener
								.onStateChange(ServiceState.STATE_CONNECTED);
					}
					createNotification();

				} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED
						.equals(action)) {
					serviceState = ServiceState.STATE_DISCONNECTED;
					if (btServiceResponseListener != null) {
						btServiceResponseListener
								.onStateChange(ServiceState.STATE_DISCONNECTED);
					}
					stopRecord();
					createNotification();
				}

			}
		};

		sampleStore = new SampleStoreFile(GRAPH_SAMPEL_COUNT, FILE_SAMPEL_COUNT);

		// Add intent filters to catch bluetooth connection and disconnection.
		IntentFilter btIntentFilter = new IntentFilter();
		btIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		btIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(btBroadcastReceiver, btIntentFilter);

		// Display a notification to announce started service.
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		createNotification();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(this.getClass().getSimpleName(), "onStartCommand");

		// Different requests as sent from the action bar.
		String action = intent.getAction();

		if (action != null) {
			int param_index = intent.getIntExtra("index", 0);
			int param_value = intent.getIntExtra("value", Integer.MIN_VALUE);

			if (action == ACTION_CONNECT) {
				btThread.sendCommand(BtServiceCommand.CONNECT, null);

			} else if (action == ACTION_DISCONNECT) {
				btThread.sendCommand(BtServiceCommand.DISCONNECT, null);

			} else if (action == ACTION_GET_PARAMETER) {
				if (intent.hasExtra("index")) {
					// Get parameter by index
					btThread.sendCommand(BtServiceCommand.SETP,
							new byte[] { (byte) param_index });
				} else {
					// Get next parameter
					btThread.sendCommand(BtServiceCommand.SETP, null);
				}

			} else if (action == ACTION_GET_PARAMETERS) {
				btThread.sendCommand(BtServiceCommand.GET_PARAMETERS, null);

			} else if (action == ACTION_GET_STATE) {
				btThread.sendCommand(BtServiceCommand.GET_STATE, null);

			} else if (action == ACTION_KILL_SERVICE) {
				// Cancel the persistent notification and kill service.
				// onDestroy will be called as a consequence of calling
				// stopSelf().
				mNM.cancel(NOTIFICATION);
				stopSelf();

			} else if (action == ACTION_STOP_RECORDING) {
				stopRecord();

			} else if (action == ACTION_START_RECORDING) {
				startRecord();

			} else if (action == ACTION_SET_PARAMETER) {
				if (intent.hasExtra("index") && intent.hasExtra("value")) {
					btThread.sendCommand(BtServiceCommand.SETP, new byte[] {
							(byte) param_index, //
							(byte) ((param_value & 0xFF) >> 8), //
							(byte) (param_value & 0xFF) });
				}

			} else if (action == ACTION_START_SAMPLING) {
				btThread.sendCommand(BtServiceCommand.GET_SAMPLES, null);

			} else if (action == ACTION_STOP_SAMPLING) {
				btThread.sendCommand(BtServiceCommand.STOP, null);

			} else if (action == ACTION_WINCH_BTN_DOWN) {
				btThread.sendCommand(BtServiceCommand.DOWN, null);

			} else if (action == ACTION_WINCH_BTN_UP) {
				btThread.sendCommand(BtServiceCommand.UP, null);

			} else {
				Log.d(this.getClass().getSimpleName(),
						"Service started with empty action");
			}
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Log.i(this.getClass().getSimpleName(), "onBind");
		return btBinder;
	}

	@Override
	public void onDestroy() {
		Log.i(this.getClass().getSimpleName(), "onDestroy");

		// Disconnect bluetooth
		btThread.getLooper().quit();

		// Unregister broadcast receiver
		unregisterReceiver(btBroadcastReceiver);

		// Clear some stuff (shouldn't be necessary)
		btThread = null;
		btBroadcastReceiver = null;
		btServiceResponseListener = null;

		// Tell the user we stopped.
		Toast.makeText(this, R.string.btservice_stopped, Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Build and show notification based on service status and recording status.
	 */
	private static void createNotification() {

		// Build the notification
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				appContext);
		mBuilder.setOngoing(true);
		mBuilder.setSmallIcon(R.drawable.ic_stat_notify); //
		mBuilder.setContentTitle(appContext.getText(R.string.app_name)); //
		mBuilder.setWhen(System.currentTimeMillis());

		// PendingIntent:s to launch if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0,
				new Intent(appContext, ConnectAct.class), 0);
		mBuilder.setContentIntent(contentIntent);

		switch (serviceState) {
		case STATE_CONNECTED:
			mBuilder.setContentText(appContext
					.getText(R.string.state_connected));
			break;

		case STATE_DISCONNECTED:
			mBuilder.setContentText(appContext
					.getText(R.string.state_disconnected));
			break;
		case STATE_SAMPELS:
			mBuilder.setContentText(appContext.getText(R.string.state_sampels));
			break;

		case STATE_STOPPED:
			mBuilder.setContentText(appContext.getText(R.string.state_stopped));
			break;

		case STATE_SYNCS:
			mBuilder.setContentText(appContext.getText(R.string.state_syncs));
			break;

		default:
			break;

		}

		// Add extra actions
		if (sampleStore.isWriting()) {
			PendingIntent recordOffIntent = PendingIntent.getService(
					appContext, 0, new Intent(ACTION_STOP_RECORDING, null,
							appContext, BTService.class), 0);
			mBuilder.addAction(R.drawable.ic_menu_stop, "Stoppa log",
					recordOffIntent);

		} else {
			PendingIntent recordOnIntent = PendingIntent.getService(appContext,
					0, new Intent(ACTION_START_RECORDING, null, appContext,
							BTService.class), 0);
			mBuilder.addAction(R.drawable.ic_menu_record, "Starta log",
					recordOnIntent);
		}

		mNM.notify(NOTIFICATION, mBuilder.build());
	}

	/**
	 * Return the storage for samples.
	 * 
	 * @return
	 */
	public SampleStore getSampleStore() {
		return sampleStore;
	}

	/**
	 * Start saving samples to file.
	 */
	private static void startRecord() {
		sampleStore.startWrite();
		btHandlerOnMain
				.dispatchMessage(btHandlerOnMain.obtainMessage(
						ServiceLoggerState.ENUM_TYPE,
						ServiceLoggerState.LOGGER_ACTIVE));
	}

	/**
	 * Stop saving samples to file.
	 */
	private static void stopRecord() {
		sampleStore.stopWrite();
		btHandlerOnMain.dispatchMessage(btHandlerOnMain.obtainMessage(
				ServiceLoggerState.ENUM_TYPE,
				ServiceLoggerState.LOGGER_INACTIVE));
	}
	
	public void setBtMac(String mac_address) {
		btThread.setMac(mac_address);
	}

	private static class BtHandlerOnMain extends Handler {

		public void handleMessage(Message msg) {

			switch (msg.what) {
			case ServiceState.ENUM_TYPE:
				handleState(msg);
				break;

			case ServiceResult.ENUM_TYPE:
				handleResult(msg);
				break;

			case ServiceLoggerState.ENUM_TYPE:
				handleLoggerState(msg);
				break;

			default:
				throw new IllegalArgumentException();
			}

		}

		private void handleLoggerState(Message msg) {
			ServiceLoggerState logState = ServiceLoggerState.toEnum(msg.arg1);

			createNotification();

			// Notify listener if available
			if (btServiceResponseListener == null) {
				return;
			}
			switch (logState) {
			case LOGGER_ACTIVE:
				btServiceResponseListener.onRecordStateChange(true);
				break;

			case LOGGER_INACTIVE:
				btServiceResponseListener.onRecordStateChange(false);
				break;

			default:
			}

		}

		private void handleState(Message msg) {

			if (ServiceState.toEnum(msg.arg1) == null) {
				throw new IllegalStateException("Unknown state reported!");
			}

			serviceState = ServiceState.toEnum(msg.arg1);
			createNotification();

			switch (serviceState) {
			case STATE_CONNECTED:
				break;

			case STATE_DISCONNECTED:
				stopRecord();
				break;

			case STATE_SAMPELS:
				break;

			case STATE_STOPPED:
				stopRecord();
				break;

			case STATE_SYNCS:
				break;

			default:
				break;
			}

			if (btServiceResponseListener != null) {
				btServiceResponseListener.onStateChange(serviceState);
			}

		}

		private void handleResult(Message msg) {

			Parameter param = null;
			Sample sam = null;
			ServiceResult result = ServiceResult.toEnum(msg.arg1);

			// Add parameters and samples to store.
			switch (result) {
			case PARAMETER_RECEIVED:
				param = new Parameter((byte[]) msg.obj);
				sampleStore.add(param);
				break;

			case SAMPLE_RECEIVED:
				sam = new Sample((byte[]) msg.obj);
				sampleStore.add(sam);
				break;

			default:
			}

			// Notify listener if available
			if (btServiceResponseListener == null) {
				return;
			}

			switch (result) {
			case ANS_TXT:
				btServiceResponseListener.onText((CharSequence) msg.obj);
				break;

			case CONNECTION_TIMEOUT:
				btServiceResponseListener.onConnectionTimeout();
				break;

			case PACKAGE_TIMEOUT:
				btServiceResponseListener.onPackageTimeout();
				break;

			case PARAMETER_RECEIVED:
				btServiceResponseListener.onParameterReceived(param);
				break;

			case SAMPLE_RECEIVED:
				btServiceResponseListener.onSampleReceived(sam);
				break;

			default:
				// Unknown result
				throw (new IllegalArgumentException());
			}

		}
	}

	/**
	 * Set service listener. Returns true if a new listener is set.
	 * 
	 * @param listener
	 * @return
	 */
	public boolean setListener(BtServiceListener listener) {
		if (listener == null) {
			return false;
		}
		if (btServiceResponseListener == null) {
			btServiceResponseListener = listener;
			return true;
		}
		if (btServiceResponseListener.getId() == listener.getId()) {
			return false;
		}
		btServiceResponseListener = listener;
		return true;
	}

	public class BtBinder extends Binder {
		public BTService getService() {
			return BTService.this;
		}
	}

}

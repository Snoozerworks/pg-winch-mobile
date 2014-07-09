package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.ConnectAct;
//import skarmflyg.org.gohigh.R.string;
import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.ParameterSet;
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
import android.support.v4.app.NotificationCompat.Builder;
//import android.util.Log;
import android.widget.Toast;

public class BTService extends Service {
	private final BtBinder btBinder = new BtBinder();

	// Bluetooth thread name
	static final private String BT_THREAD_NAME = "WinchThread";

	// Bluetooth thread.
	private static BtThread btThread;

	// Receive bluetooth events.
	private static BroadcastReceiver btBroadcastReceiver;

	private static BtServiceListener btServiceResponseListener;

	private static SampleStoreFile sampleStore;

	// Notifier
	private NotificationManager mNM;

	// Unique Identification Number for the Notification. We use it on
	// Notification start, and to cancel it.
	private int NOTIFICATION = R.string.btservice_started;

	public static final int GRAPH_SAMPEL_COUNT = 50;
	private final int FILE_SAMPEL_COUNT = 1500;

	@Override
	public void onCreate() {
		// Log.i(this.getClass().getSimpleName(), "onCreate");

		// Log.d(BTService.class.getSimpleName(), "BTService construct");

		// Handle message from bluetooth thread.
		Context c = getApplicationContext();
		BtHandlerOnMain h = new BtHandlerOnMain();
		btThread = new BtThread(c, BT_THREAD_NAME, h);
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

				NotificationCompat.Builder builder;
				builder = getNotification();

				if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
					builder.setContentText(getText(R.string.bt_connected));
					mNM.notify(NOTIFICATION, builder.build());

					if (btServiceResponseListener != null) {
						btServiceResponseListener
								.onStatusChange(BtServiceStatus.STATE_CONNECTED);
					}

				} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED
						.equals(action)) {
					builder.setContentText(getText(R.string.bt_disconnected));
					mNM.notify(NOTIFICATION, builder.build());

					sampleStore.stopWrite();

					if (btServiceResponseListener != null) {
						btServiceResponseListener
								.onStatusChange(BtServiceStatus.STATE_DISCONNECTED);
					}

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
		Builder notification = getNotification();
		notification.setContentText(getText(R.string.btservice_started));
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNM.notify(NOTIFICATION, notification.build());

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Log.i(this.getClass().getSimpleName(),
		// "onStartCommand. Received start id " + startId + ": " + intent);

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
		// Log.i(this.getClass().getSimpleName(), "onDestroy");

		// Disconnect bluetooth
		winchSendCommand(BtServiceCommand.DISCONNECT);

		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		unregisterReceiver(btBroadcastReceiver);
		btThread.getLooper().quit();
		btThread = null;

		// Tell the user we stopped.
		Toast.makeText(this, R.string.btservice_stopped, Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private NotificationCompat.Builder getNotification() {
		// Set icon, scrolling text and time stamp
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_stat_notify) //
				.setContentTitle(getText(R.string.app_name)) //
				.setWhen(System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ConnectAct.class), 0);

		// Set the info for the views that show in the notification panel.
		mBuilder.setContentIntent(contentIntent);

		return mBuilder;
	}

	/**
	 * Send a command to the winch to expect a Sample or Parameter back.
	 * 
	 * @param cmd
	 */
	public void winchSendCommand(BtServiceCommand cmd) {
		btThread.sendCommand(cmd);
	}

	/**
	 * Send command to set winsch parameter.
	 * 
	 * @param i
	 *            Parameter index.
	 * @param v
	 *            Parameter value to set.
	 */
	public void winchSendCommand(BtServiceCommand cmd, byte i, short v) {
		btThread.sendCommand(cmd, i, v);
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
	 * Get the parameter set.
	 * 
	 * @return Set of parameters.
	 */
	public ParameterSet getParameterSet() {
		return sampleStore.getParameters();
	}

	/**
	 * Get parameter by index.
	 * 
	 * @param index
	 * @return Parameter or null if index do not exist.
	 */
	public Parameter getParameter(byte index) {
		return sampleStore.getParameter(index);
	}

	private static class BtHandlerOnMain extends Handler {

		public void handleMessage(Message msg) {

			switch (msg.what) {
			case BtServiceStatus.ENUM_TYPE:
				handleStatus(msg);
				break;

			case BtServiceResult.ENUM_TYPE:
				handleResult(msg);
				break;

			default:
				throw new IllegalArgumentException();
			}

		}

		private void handleStatus(Message msg) {

			BtServiceStatus s = BtServiceStatus.toEnum(msg.arg1);

			switch (s) {
			case STATE_CONNECTED:
				break;

			case STATE_DISCONNECTED:
				sampleStore.stopWrite();
				break;

			case STATE_SAMPELS:
				break;

			case STATE_STOPPED:
				sampleStore.stopWrite();
				break;

			case STATE_SYNCS:
				break;

			default:
				break;
			}

			btServiceResponseListener.onStatusChange(s);

		}

		private void handleResult(Message msg) {

			Parameter param = null;
			Sample sam = null;
			CharSequence txt = null;
			BtServiceResult r = BtServiceResult.toEnum(msg.arg1);

			switch (r) {
			case ANS_TXT:
				txt = (CharSequence) msg.obj;
				if (btServiceResponseListener != null) {
					btServiceResponseListener.onText(txt);
				}
				break;

			case CONNECTION_TIMEOUT:
				if (btServiceResponseListener != null) {
					btServiceResponseListener.onConnectionTimeout();
				}
				break;

			case PACKAGE_TIMEOUT:
				if (btServiceResponseListener != null) {
					btServiceResponseListener.onPackageTimeout();
				}
				break;

			case PARAMETER_RECEIVED:
				param = new Parameter((byte[]) msg.obj);
				sampleStore.add(param);
				if (btServiceResponseListener != null) {
					btServiceResponseListener.onParameterReceived(param);
				}
				break;

			case SAMPLE_RECEIVED:
				sam = new Sample((byte[]) msg.obj);
				if (!sampleStore.isWriting()) {
					sampleStore.startWrite();
				}
				sampleStore.add(sam);
				if (btServiceResponseListener != null) {
					btServiceResponseListener.onSampleReceived(sam);
				}
				break;

			default:
				// Unknown result
				throw (new IllegalArgumentException());
			}

		}
	}

	public void setListener(BtServiceListener listener) {
		if (btServiceResponseListener != null) {
			// Log.d(this.getClass().getSimpleName(),
			// "Replacing listener in service");
		}
		btServiceResponseListener = listener;
	}

	public class BtBinder extends Binder {
		public BTService getService() {
			return BTService.this;
		}
	}

}

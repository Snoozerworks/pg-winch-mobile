package skarmflyg.org.gohigh.btservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;
import skarmflyg.org.gohigh.ConnectAct;
//import skarmflyg.org.gohigh.R.string;
import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Command;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

public class BTService extends Service  {
	// Bluetooth stuff
	// static private final String BT_MAC = "00:06:66:43:11:8D"; // <-- ***
	// Change MAC-address here! ***
	final static private String BT_MAC = "00:06:66:43:07:C0";

	// Bluetooth thread name
	final static private String BT_THREAD_NAME = "WinchThread";

	// Bluetooth timeout in milliseconds.
	final static private int BT_PACKAGE_TIMEOUT = 1000;

	// Bluetooth period of reads in milliseconds.
	final static private int BT_PACKAGE_READ_PERIOD = 40;

	// Bluetooth thread.
	final private HandlerThread btThread;

	// Bluetooth UUID. General for serial communication.
	final static private UUID BT_SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Bluetooth socket and streams
	static private BluetoothSocket btSocket;
	static private InputStream btInstream;
	static private OutputStream btOutstream;

	// Receive bluetooth events.
	private final BroadcastReceiver btBroadcastReceiver;

	// Handle thread communication
	final private BluetoothAdapter btAdapter;
	static private BluetoothDevice btDevice;
	static private BtCommandHandler btCommandHandler;

	// Handle messages to client
	static private Messenger clientMessenger;

	// Notifier
	private NotificationManager mNM;

	// Unique Identification Number for the Notification. We use it on
	// Notification start, and to cancel it.
	private int NOTIFICATION = R.string.btservice_started;

	public static final int GRAPH_SAMPEL_COUNT = 50;
	private static final int FILE_SAMPEL_COUNT = 1500;
	private static SampleStoreFile sampleStore;
	BtResponseStoreHandler handler;
	

	
	
	
	BtThread tstthread;

	/**
	 * Constructor
	 */
	public BTService() {
		super();

		Log.d(BTService.class.getSimpleName(), "BTService construct");

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		btThread = new HandlerThread(BT_THREAD_NAME);
		btThread.start();
		btCommandHandler = new BtCommandHandler(btThread.getLooper());

		handler = new BtResponseStoreHandler();

		
		
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

					tellClient(BtServiceResponse.STATE_CONNECTED);

				} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED
						.equals(action)) {
					builder.setContentText(getText(R.string.bt_disconnected));
					mNM.notify(NOTIFICATION, builder.build());
					btSocket = null;
					btInstream = null;
					btOutstream = null;

					tellClient(BtServiceResponse.STATE_DISCONNECTED);

				}

			}
		};

		sampleStore = new SampleStoreFile(GRAPH_SAMPEL_COUNT, FILE_SAMPEL_COUNT);
	}

	@Override
	public void onCreate() {
		logInfo("onCreate");
		
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
		logInfo("onStartCommand. Received start id " + startId + ": " + intent);

		clientMessenger = intent.getParcelableExtra("clientmessenger");

		winchSendCommand(BtServiceCommand.GET_STATE);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		logInfo("onBind");
		return null;
		// return new LocalBinder<BTService>(this);
	}

	@Override
	public void onDestroy() {
		logInfo("onDestroy");

		// Disconnect bluetooth
		btDisconnect();

		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		unregisterReceiver(btBroadcastReceiver);
		btCommandHandler.getLooper().quit();
		btCommandHandler = null;

		// Tell the user we stopped.
		Toast.makeText(this, R.string.btservice_stopped, Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * Connect to bluetooth. Enable adapter and select device if necessary.
	 * Sends message MSG_BT_CONNECTED to client message handler once connected
	 * (also when already connected). Sends MSG_ANS_TXT with accompanied text if
	 * bluetooth is not supported or is not enabled.
	 * 
	 * This method setup the bluetooth socket, input stream and output stream.
	 */
	public void btConnect() {
		if (btIsConnected()) {
			tellClient(BtServiceResponse.STATE_CONNECTED);
			return;
		}

		// Check bluetooth is supported
		if (btAdapter == null) {
			tellClient(BtServiceResponse.ANS_TXT,
					getText(R.string.bt_not_supported));
			return;
		}

		// Check bluetooth is enabled
		if (!btAdapter.isEnabled()) {
			tellClient(BtServiceResponse.ANS_TXT, getText(R.string.bt_disabled));
			return;
		}

		// Try get remote bluetooth device
		btDevice = btAdapter.getRemoteDevice(BT_MAC);

		// Create bluetooth rfcomm socket (serial communication)
		try {
			btSocket = btDevice.createRfcommSocketToServiceRecord(BT_SPP_UUID);
		} catch (IOException e) {
			tellClient(BtServiceResponse.STATE_DISCONNECTED);
			tellClient(BtServiceResponse.ANS_TXT,
					"ERROR! Failed creating RFCOMM socket.");
			btSocket = null;
			return;
		}

		// Always cancel discovery before connecting
		btAdapter.cancelDiscovery();

		// Connect...
		try {
			btSocket.connect();
		} catch (IOException e) {
			tellClient(BtServiceResponse.STATE_DISCONNECTED);
			tellClient(
					BtServiceResponse.ANS_TXT,
					"ERROR! Failed connecting RFCOMM socket.\n"
							+ e.getLocalizedMessage());
			btSocket = null;
			return;
		}

		// Create input and output streams
		try {
			btInstream = btSocket.getInputStream();
			btOutstream = btSocket.getOutputStream();
		} catch (IOException e1) {
			tellClient(BtServiceResponse.STATE_DISCONNECTED);
			tellClient(BtServiceResponse.ANS_TXT,
					"ERROR! Failed creating streams.");
			btInstream = null;
			btOutstream = null;
			e1.printStackTrace();
		}

	}

	// /**
	// * Close bluetooth socket.
	// */
	// public void BTDisconnect() {
	// // SendBtMessage(BtServiceCommand.DISCONNECT);
	// btDisconnect();
	// }

	/**
	 * Close input and output streams and the bluetooth socket.
	 * 
	 * @return
	 */
	public void btDisconnect() {
		if (btInstream != null) {
			try {
				btInstream.close();
			} catch (Exception e) {
			}
			btInstream = null;
		}

		if (btOutstream != null) {
			try {
				btOutstream.close();
			} catch (Exception e) {
			}
			btOutstream = null;
		}

		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (Exception e) {
			}
			btSocket = null;
		}

	}

	/**
	 * Check if a bluetooth connection is established.
	 * 
	 * @return True if connected.
	 */
	public boolean btIsConnected() {
		return (btSocket != null && btSocket.isConnected());
	}

	/**
	 * Short for Log.i(this.getClass().getSimpleName(), s)
	 * 
	 * @param s
	 */
	private void logInfo(String s) {
		Log.i(this.getClass().getSimpleName(), s);
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
	 * Send a response to the client.
	 * 
	 * @param response
	 */
	static private void tellClient(BtServiceResponse response) {
		tellClient(response, null);
	}

	/**
	 * Send a response to the client.
	 * 
	 * @param response
	 * @param obj
	 */
	static private void tellClient(BtServiceResponse response, Object obj) {
		Message m = Message.obtain();
		m.what = response.Value();
		m.obj = obj;
		try {
			clientMessenger.send(m);
			// TODO below does not work...
			// msn.send(m);
		} catch (RemoteException e) {
			Log.e(BTService.class.getSimpleName(),
					"Failed sending message client");
			e.printStackTrace();
		}
	}

	// /**
	// * Close bluetooth socket.
	// */
	// public void BTDisconnect() {
	// // SendBtMessage(BtServiceCommand.DISCONNECT);
	// btDisconnect();
	// }

	/**
	 * Send a command to the winch to expect a Sample or Parameter back.
	 * 
	 * @param cmd
	 */
	public void winchSendCommand(BtServiceCommand cmd) {
		btCommandHandler.obtainMessage(cmd.Value()).sendToTarget();
	}

	/**
	 * Send command to set winsch parameter.
	 * 
	 * @param i
	 *            Parameter index.
	 * @param v
	 *            Parameter value to set.
	 */
	public void winchSendSETP(byte i, short v) {
		btCommandHandler.obtainMessage(BtServiceCommand.SETP.Value(), v, i)
				.sendToTarget();
	}

	/**
	 * Return the storage for samples.
	 * 
	 * @return
	 */
	public SampleStore getSampleStore() {
		return sampleStore;
	}

	static class BtCommandHandler extends Handler {

		// Flag to get last command sent repeated.
		private BtServiceCommand tx_repeat;
		static private byte[] btRxBuffer;
		static private int bytesRead = 0;
		static private int bytesToRead = 0;

		/**
		 * Constructor
		 * 
		 * @param looper
		 */
		public BtCommandHandler(Looper looper) {
			super(looper);
			int buf_size = Math.max(Parameter.BYTE_SIZE, Sample.BYTE_SIZE);
			btRxBuffer = new byte[buf_size];
		}

		/**
		 * Short for Log.i(this.getClass().getSimpleName(), s)
		 * 
		 * @param s
		 */
		private void logInfo(String s) {
			Log.i(this.getClass().getSimpleName(), s);
		}

		/**
		 * Handle messages to the thread.
		 * 
		 * BtServiceCommand given in msg.what can be
		 * <ul>
		 * <li>DISCONNECT : Close streams and bluetooth socket.</li>
		 * <li>GET_PARAMETER : Send a single SET command.</li>
		 * <li>GET_PARAMETERS : Send a SET command and a new SET after each
		 * sample received.</li>
		 * <li>SET : See GET_PARAMETER.</li>
		 * <li>GET_SAMPLE : Send a single GET command.</li>
		 * <li>GET_SAMPLES : Send a GET command and a new GET after each sample
		 * received.</li>
		 * <li>KILL : Close streams and bluetooth socket. Interrupt thread.</li>
		 * <li>TIMEOUT : Used from inside thread to issue a timeout.</li>
		 * <li>_READ : Read bytes from input stream.</li>
		 * </ul>
		 * 
		 * The timeout in milliseconds should be specified in msg.arg1. All
		 * messages with commands except DISCONNECT, KILL and TIMEOUT will be
		 * ignored until a sample or parameter has been successfully received or
		 * there was a time out.
		 * 
		 * @param msg
		 *            Message where msg.obj=BtServiceCommand and
		 *            msg.arg1=timeout.
		 */
		public void handleMessage(Message msg) {
			if (Thread.currentThread().isInterrupted()) {
				// btDisconnect();
				return;
			}

			BtServiceCommand msg_command = BtServiceCommand.get(msg.what);

			switch (msg_command) {
			case KILL:
				// Interrupt thread and close socket.
				Log.d(this.getClass().getSimpleName(),
						"KILL thread "
								+ Long.toString(Thread.currentThread().getId()));

				getLooper().quit();
				Thread.currentThread().interrupt();

			case DISCONNECT:
				// TODO Correct?
				// bytesToRead = 0;
				return;

			case TIMEOUT:
				// Waited too long for data. Stop asking for more data.
				onTimeoutCmd();
				return;

			case STOP:
				// Stop any ongoing transaction of data.
				onStopCmd();
				return;

			case GET_STATE:
				// Send current service state to client. Does not send anything
				// to the winsch.
				onGetStateCmd();
				return;

			case _READ:
				// Read data from bluetooth.
				btReadStream();
				break;

			default:
				if (bytesToRead == 0) {
					// No pending transfer. Ready to send a new command with
					// timeout.
					poll(msg);
				}
			}

			if (bytesToRead == bytesRead) {
				// All data to received.
				sendResponse(bytesRead);
				bytesToRead = 0;
				bytesRead = 0;
				this.removeCallbacksAndMessages(null);
				if (tx_repeat != null) {
					this.sendEmptyMessage(tx_repeat.Value());
				} else {
					tellClient(BtServiceResponse.STATE_STOPPED);
				}

			} else {
				// More data to receive.
				this.sendEmptyMessageDelayed(BtServiceCommand._READ.Value(),
						BT_PACKAGE_READ_PERIOD);

			}

		}

		/**
		 * Sends, in order, PACKAGE_TIMEOUT and STATE_STOPPED to client.
		 * 
		 * Set bytesToRead to 0.
		 */
		private void onTimeoutCmd() {
			logInfo("Package timeout.");
			bytesToRead = 0;
			this.removeCallbacksAndMessages(null);
			tellClient(BtServiceResponse.PACKAGE_TIMEOUT);
			tellClient(BtServiceResponse.STATE_STOPPED);
		}

		/**
		 * Stops polling the winch for more data. Send STATE_STOPPED to client.
		 */
		private void onStopCmd() {
			tx_repeat = null;
			bytesToRead = 0;
			this.removeCallbacksAndMessages(null);
			tellClient(BtServiceResponse.STATE_STOPPED);
		}

		/**
		 * Send current state of service to client.
		 * 
		 * States can be
		 * <ul>
		 * <li>STATE_DISCONNECTED - No bluetooth connection.</li>
		 * <li>STATE_SAMPELS - Getting winch samples {@link Sample}.</li>
		 * <li>STATE_SYNCS - Getting winch parameters {@link Parameter}</li>
		 * <li>STATE_STOPPED</li>
		 * <ul/>
		 */
		private void onGetStateCmd() {
			if (btSocket == null || !btSocket.isConnected()) {
				tellClient(BtServiceResponse.STATE_DISCONNECTED);

			} else if (bytesToRead == Sample.BYTE_SIZE) {
				tellClient(BtServiceResponse.STATE_SAMPELS);

			} else if (bytesToRead == Parameter.BYTE_SIZE) {
				tellClient(BtServiceResponse.STATE_SYNCS);

			} else {
				tellClient(BtServiceResponse.STATE_STOPPED);

			}
		}

		/**
		 * Poll winch for a {@link Parameter} or {@link Sample}.
		 * 
		 * Commands can be
		 * <ul>
		 * <li>GET_PARAMETERS - Get parameters until service receives
		 * BtServiceCommand.STOP.</li>
		 * <li>SET - Get next parameter. Same as set button on winch.</li>
		 * <li>SETP - Set parameter value in arg1 by specifying parameter index
		 * in arg2.</li>
		 * <li>GET_PARAMETER - Same as SET</li>
		 * <li>GET_SAMPLES - Get samples until service receives
		 * BtServiceCommand.STOP.</li>
		 * <li>GET_SAMPLE - Get a single sample</li>
		 * <li>DOWN - Same as down button on winch.</li>
		 * <li>UP - Same as down button on winch.</li>
		 * <li>NOCMD - Empty command. Do nothing.</li>
		 * </ul>
		 * 
		 * Skips all bytes in bluetooth input stream by calling btSkipStream()
		 * before sending the new command.
		 * 
		 * @param msg_command
		 */
		private void poll(Message msg) {
			byte[] tx_bytes = new byte[4];

			BtServiceCommand msg_command = BtServiceCommand.get(msg.what);

			// Skip all bytes in stream
			btSkipStream();

			// Command winschCmd = Command.NOCMD;
			tx_bytes[0] = Command.NOCMD.getByte();
			bytesRead = 0;
			bytesToRead = 0;

			switch (msg_command) {
			case SETP:
				bytesToRead = Parameter.BYTE_SIZE;
				tx_bytes[0] = Command.SETP.getByte();
				tx_bytes[1] = (byte) (msg.arg2);
				tx_bytes[2] = (byte) ((msg.arg1 & 0xFF) >> 8);
				tx_bytes[3] = (byte) (msg.arg1 & 0xFF);
				break;

			case SELECT:
				bytesToRead = Parameter.BYTE_SIZE;
				tx_bytes[0] = Command.SET.getByte();
				break;

			case GET_PARAMETERS:
				tx_repeat = msg_command;
				tellClient(BtServiceResponse.STATE_SYNCS);
			case GET_PARAMETER:
				bytesToRead = Parameter.BYTE_SIZE;
				tx_bytes[0] = Command.SETP.getByte();
				break;

			case GET_SAMPLES:
				tx_repeat = msg_command;
				tellClient(BtServiceResponse.STATE_SAMPELS);
			case GET_SAMPLE:
				bytesToRead = Sample.BYTE_SIZE;
				tx_bytes[0] = Command.GET.getByte();
				break;

			case DOWN:
				bytesToRead = Parameter.BYTE_SIZE;
				tx_bytes[0] = Command.DOWN.getByte();
				break;

			case UP:
				bytesToRead = Parameter.BYTE_SIZE;
				tx_bytes[0] = Command.UP.getByte();
				break;

			default:
				tx_bytes[0] = Command.NOCMD.getByte();
				return;
			}

			this.sendEmptyMessageDelayed(BtServiceCommand.TIMEOUT.Value(),
					BT_PACKAGE_TIMEOUT);

			// Send command and set a timeout
			logInfo("Send command: " + Command.values()[tx_bytes[0]].toString());

			// tx_bytes[0] = winschCmd.getByte();
			if (msg_command == BtServiceCommand.SETP) {
				btWrite(tx_bytes);
			} else {
				btWrite(tx_bytes, 1);
			}

		}

		/**
		 * Send response SAMPLE_RECEIVED or PACKAGE_RECEIVED to client.
		 * 
		 * @param bytesRead
		 */
		private void sendResponse(int bytesRead) {
			byte[] bytes = Arrays.copyOf(btRxBuffer, bytesRead);

			if (bytesRead == Sample.BYTE_SIZE) {
				logInfo("Sample received.");
				tellClient(BtServiceResponse.SAMPLE_RECEIVED, bytes);
			} else if (bytesRead == Parameter.BYTE_SIZE) {
				logInfo("Parameter received.");
				tellClient(BtServiceResponse.PARAMETER_RECEIVED, bytes);
			} else {
				Log.e(this.getClass().getSimpleName(),
						"Corrupted data received.");
			}

		}

		/**
		 * Skip all bytes in bluetooth input stream.
		 */
		private void btSkipStream() {
			int bytesAvailable;
			long skipped = 0;

			try {
				bytesAvailable = btInstream.available();
				while (bytesAvailable > 0) {
					skipped += btInstream.skip(bytesAvailable);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
					bytesAvailable = btInstream.available();
				}

			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(),
						"IOException. Failed reading bluetooth instream. Check stack trace.");
				e.printStackTrace();
			}

			if (skipped > 0) {
				Log.d("btSkipInput",
						String.format("Skipped %d bytes.", skipped));
			}

		}

		/**
		 * Read serial data from the bluetooth.
		 * 
		 * Method attempt to place bytesToRead number of bytes into btRxBuffer.
		 * If available byte in the input stream > bytesToRead then excessive
		 * bytes are skipped from the start of the stream before filling
		 * btRxBuffer.
		 * 
		 * @return True when the number of bytes fill data package raw buffer.
		 */
		private boolean btReadStream() {
			int bytesAvailable;
			int remains = bytesToRead - bytesRead;

			try {
				bytesAvailable = btInstream.available();
				bytesRead += btInstream.read(btRxBuffer, bytesRead,
						Math.min(bytesAvailable, remains));

			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(),
						"IOException. Failed reading bluetooth instream. Check stack trace.");
				e.printStackTrace();
				return false;
			}

			return (bytesRead == bytesToRead);
		}

		/**
		 * Write byte array to bluetooth.
		 * 
		 * @param tx_bytes
		 *            Byte array.
		 * @param len
		 *            Number of bytes to write.
		 * @return False if there was an IO exception..
		 */
		public boolean btWrite(byte[] tx_bytes, int len) {
			try {
				btOutstream.write(tx_bytes, 0, len);
				btOutstream.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		/**
		 * Write bytes to bluetooth.
		 * 
		 * @param tx_bytes
		 * @return False if there was an IO exception..
		 */
		public boolean btWrite(byte[] tx_bytes) {
			return btWrite(tx_bytes, tx_bytes.length);
		}

		/**
		 * Write a single byte to bluetooth.
		 * 
		 * @param tx_bytes
		 * @return False if there was an IO exception.
		 */
		public boolean btWrite(byte tx_bytes) {
			return btWrite(new byte[] { tx_bytes }, 1);
		}

	};

	static private class BtResponseStoreHandler extends Handler {
		public void handleMessage(Message msg) {

			// TODO Messages from bt service should be passed to BaseAct too...
			// This is ugly.
			BtServiceResponse reported_state = BtServiceResponse.get(msg.what);

			// logTxt("State: " + reported_state.toString());

			switch (reported_state) {
			case STATE_SAMPELS:
				break;
			case STATE_SYNCS:
			case STATE_CONNECTED:
			case STATE_DISCONNECTED:
			case PACKAGE_TIMEOUT:
			case PARAMETER_RECEIVED:
			case ANS_TXT:
				// case HANDLER_SET:
				// case HANDLER_UNSET:
				break;
			case STATE_STOPPED:
				sampleStore.stopWrite();
				break;

			case SAMPLE_RECEIVED:
				if (!sampleStore.isWriting()) {
					sampleStore.startWrite();
				}

				Sample sam = new Sample();
				sam.LoadBytes((byte[]) msg.obj);

				sampleStore.add(sam);

				break;
			}
		}
	}

}

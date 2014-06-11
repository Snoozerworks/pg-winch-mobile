package skarmflyg.org.gohigh.btservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;
import skarmflyg.org.gohigh.ConnectAct;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class BTService extends Service {
	// Bluetooth stuff
	// static private final String BT_MAC = "00:06:66:43:11:8D"; // <-- ***
	// Change MAC-address here! ***
	static private final String BT_MAC = "00:06:66:43:07:C0"; // <-- *** Change
																// MAC-address
																// here! ***
	private final static int BT_PACKAGE_TIMEOUT = 1000;	// Timeout in milliseconds
	private final static int BT_PACKAGE_READ_PERIOD = 40;	// Period of reads in milliseconds

	static private BluetoothSocket bt_socket;
	static private InputStream bt_instream;
	static private OutputStream bt_outstream;
	static private final String BT_THREAD_NAME = "WinchThread";
	static private final UUID BT_SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	static private BTThreadHandler bt_thread_handler;

	private final HandlerThread bt_thread = new HandlerThread(BT_THREAD_NAME);

	static private final BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
	static private BluetoothDevice bt_device;

	private NotificationManager mNM;
	static private Handler clientHandler;

	// Unique Identification Number for the Notification. We use it on
	// Notification start, and to cancel it.
	private int NOTIFICATION = R.string.btservice_started;


	/**
	 * Short for Log.i(this.getClass().getSimpleName(), s)
	 * 
	 * @param s
	 */
	private void logInfo(String s) {
		Log.i(this.getClass().getSimpleName(), s);
	}


	@Override
	public void onCreate() {
		logInfo("onCreate");
		registerReceiver(broadcast_receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
		registerReceiver(broadcast_receiver, new IntentFilter(
				BluetoothDevice.ACTION_ACL_DISCONNECTED));

		if (!bt_thread.isAlive()) {
			bt_thread.start();
		}

		bt_thread_handler = new BTThreadHandler(bt_thread.getLooper());

		// Display a notification about us starting. We put an icon in the
		// status bar.
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNM.notify(NOTIFICATION, getNotification().build());
	}


	@Override
	public void onDestroy() {
		logInfo("onDestroy");

		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);
		bt_thread.interrupt();
		// Tell the user we stopped.
		Toast.makeText(this, R.string.btservice_stopped, Toast.LENGTH_SHORT).show();
		unregisterReceiver(broadcast_receiver);
	}


	/**
	 * Check if a bluetooth connection is established.
	 * 
	 * @return True if connected.
	 */
	static public boolean isConnected() {
		return (bt_socket != null && bt_socket.isConnected());
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logInfo("onStartCommand. Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}


	@Override
	public IBinder onBind(Intent intent) {
		logInfo("onBind");
		return new LocalBinder<BTService>(this);
	}


	/**
	 * Called by client to set a message handler receiving messages from this service. Note that if
	 * a handler is already set it must be unset calling unsetClientHandler() first.
	 * 
	 * @param h
	 *            Message handler.
	 */
	static public void setClientHandler(Handler h) {
		if (clientHandler != null) {
			return;
		}
		clientHandler = h;

		SendClientMessage(BtServiceResponse.HANDLER_SET);
		if (BTService.isConnected()) {
			SendClientMessage(BtServiceResponse.STATE_CONNECTED);
		}
	}


	/**
	 * Call to unset any client message handlers
	 */
	static public void unsetClientHandler() {
		if (clientHandler != null) {
			SendClientMessage(BtServiceResponse.HANDLER_UNSET);
			clientHandler = null;
		}
	}

	/**
	 * Broadcast receiver used to listen for when the bluetooth connects and disconnects. When the
	 * bluetooth is connected/disconnected the MSG_BT_CONNECTED/MSG_BT_DISCONNECTED is sent to the
	 * client message handler.
	 */
	private final BroadcastReceiver broadcast_receiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				// is_connected = true;
				mNM.notify(NOTIFICATION,
						getNotification().setContentText(getText(R.string.bt_connected)).build());
				SendClientMessage(BtServiceResponse.STATE_CONNECTED);

			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				// is_connected = false;
				mNM.notify(NOTIFICATION,
						getNotification().setContentText(getText(R.string.bt_disconnected)).build());
				bt_socket = null;
				bt_instream = null;
				bt_outstream = null;
				SendClientMessage(BtServiceResponse.STATE_DISCONNECTED);

			}

		}
	};


	/**
	 * Show a notification while this service is running.
	 */
	private NotificationCompat.Builder getNotification() {
		// Set the icon, scrolling text and time stamp
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat_notify) //
				.setContentText(getText(R.string.btservice_started)) //
				.setContentTitle(getText(R.string.app_name)) //
				.setWhen(System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				ConnectAct.class), 0);

		// Set the info for the views that show in the notification panel.
		mBuilder.setContentIntent(contentIntent);

		return mBuilder;
	}


	static private boolean SendClientMessage(BtServiceResponse what) {
		try {
			clientHandler.obtainMessage(what.Value()).sendToTarget();
		} catch (Exception e) {
			return false;
		}
		return true;
	}


	static private boolean SendClientMessage(BtServiceResponse what, byte[] obj) {
		try {
			clientHandler.obtainMessage(what.Value(), obj).sendToTarget();
		} catch (Exception e) {
			return false;
		}
		return true;
	}


	static private boolean SendClientMessage(BtServiceResponse what, CharSequence obj) {
		try {
			clientHandler.obtainMessage(what.Value(), obj).sendToTarget();
		} catch (Exception e) {
			return false;
		}
		return true;
	}


	private boolean SendBtMessage(BtServiceCommand message) {
		try {
			bt_thread_handler.sendEmptyMessage(message.Value());
		} catch (Exception e) {
			return false;
		}
		return true;
	}


	/**
	 * Connect to bluetooth. Enable adapter and select device if necessary. Sends message
	 * MSG_BT_CONNECTED to client message handler once connected (also when already connected).
	 * Sends MSG_ANS_TXT with accompanied text if bluetooth is not supported or is not enabled.
	 * 
	 * This method setup the bluetooth socket, input stream and output stream.
	 */
	public void BTConnect() {
		if (BTService.isConnected()) {
			SendClientMessage(BtServiceResponse.STATE_CONNECTED);
			return;
		}

		// Check bluetooth is supported
		if (bt_adapter == null) {
			SendClientMessage(BtServiceResponse.ANS_TXT, getText(R.string.bt_not_supported));
			return;
		}

		// Check bluetooth is enabled
		if (!bt_adapter.isEnabled()) {
			SendClientMessage(BtServiceResponse.ANS_TXT, getText(R.string.bt_disabled));
			return;
		}

		// Try get remote bluetooth device
		bt_device = bt_adapter.getRemoteDevice(BT_MAC);

		try {
			bt_socket = bt_device.createRfcommSocketToServiceRecord(BT_SPP_UUID);
		} catch (IOException e) {
			SendClientMessage(BtServiceResponse.ANS_TXT, "ERROR! Failed creating RFCOMM socket.");
			bt_socket = null;
			return;
		}
		try {
			bt_socket.connect();
		} catch (IOException e) {
			SendClientMessage(BtServiceResponse.ANS_TXT,
					"ERROR! Failed connecting RFCOMM socket.\n" + e.getLocalizedMessage());
			bt_socket = null;
			return;
		}
		try {
			bt_instream = bt_socket.getInputStream();
			bt_outstream = bt_socket.getOutputStream();
		} catch (IOException e1) {
			SendClientMessage(BtServiceResponse.ANS_TXT, "ERROR! Failed creating streams.");
			bt_instream = null;
			bt_outstream = null;
			e1.printStackTrace();
		}

	}


	/**
	 * Close bluetooth socket.
	 */
	public void BTDisconnect() {
		SendBtMessage(BtServiceCommand.DISCONNECT);
	}


	/**
	 * Send a command to the winch to expect a Sample or Parameter back.
	 * 
	 * @param cmd
	 */
	public void winchSendCommand(BtServiceCommand cmd) {
		bt_thread_handler.obtainMessage(cmd.Value()).sendToTarget();
	}

	static class BTThreadHandler extends Handler {

		// private boolean tx_repeat = false; // Flag to get last command sent repeated.
		private BtServiceCommand tx_repeat; // Flag to get last command sent repeated.
		static private byte[] btRxBuffer;
		static private int bytesRead = 0;
		static private int bytesToRead = 0;


		/**
		 * Constructor
		 * 
		 * @param looper
		 */
		public BTThreadHandler(Looper looper) {
			super(looper);
			btRxBuffer = new byte[Math.max(Parameter.BYTE_SIZE, Sample.BYTE_SIZE)];
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
		 * <li>GET_PARAMETERS : Send a SET command and a new SET after each sample received.</li>
		 * <li>SET : See GET_PARAMETER.</li>
		 * <li>GET_SAMPLE : Send a single GET command.</li>
		 * <li>GET_SAMPLES : Send a GET command and a new GET after each sample received.</li>
		 * <li>KILL : Close streams and bluetooth socket. Interrupt thread.</li>
		 * <li>TIMEOUT : Used from inside thread to issue a timeout.</li>
		 * <li>_READ : Read bytes from input stream.</li>
		 * </ul>
		 * 
		 * The timeout in milliseconds should be specified in msg.arg1. All messages with commands
		 * except DISCONNECT, KILL and TIMEOUT will be ignored until a sample or parameter has been
		 * successfully received or there was a time out.
		 * 
		 * @param msg
		 *            Message where msg.obj=BtServiceCommand and msg.arg1=timeout.
		 */
		public void handleMessage(Message msg) {
			if (Thread.currentThread().isInterrupted()) {
				onDisconnectCmd();
				return;
			}

			BtServiceCommand msg_command = BtServiceCommand.get(msg.what);

			switch (msg_command) {
			case KILL:
				// Interrupt thread and close socket.
				Thread.currentThread().interrupt();

			case DISCONNECT:
				// Close sockets.
				onDisconnectCmd();
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
				// Send current state to client.
				onGetStateCmd();
				return;

			case _READ:
				// Read data from bluetooth.
				btReadStream();
				break;

			default:
				if (bytesToRead == 0) {
					// No pending transfer. Ready to send a new command with timeout.
					poll(msg_command);
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
					SendClientMessage(BtServiceResponse.STATE_STOPPED);
				}

			} else {
				// More data to receive.
				this.sendEmptyMessageDelayed(BtServiceCommand._READ.Value(), BT_PACKAGE_READ_PERIOD);

			}

		}


		/**
		 * Reset input and output streams and make sure socket is closed. This method will be used
		 * during shutdown() to ensure that the connection is properly closed.
		 * 
		 * @return
		 */
		static private void onDisconnectCmd() {
			bytesToRead = 0;
			if (bt_instream != null) {
				try {
					bt_instream.close();
				} catch (Exception e) {
				}
				bt_instream = null;
			}

			if (bt_outstream != null) {
				try {
					bt_outstream.close();
				} catch (Exception e) {
				}
				bt_outstream = null;
			}

			if (bt_socket != null) {
				try {
					bt_socket.close();
				} catch (Exception e) {
				}
				bt_socket = null;
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
			SendClientMessage(BtServiceResponse.PACKAGE_TIMEOUT);
			SendClientMessage(BtServiceResponse.STATE_STOPPED);
		}


		/**
		 * Stops polling the winch for more data. Send STATE_STOPPED to client.
		 */
		private void onStopCmd() {
			tx_repeat = null;
			bytesToRead = 0;
			this.removeCallbacksAndMessages(null);
			SendClientMessage(BtServiceResponse.STATE_STOPPED);
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
			if (bt_socket == null || !bt_socket.isConnected()) {
				SendClientMessage(BtServiceResponse.STATE_DISCONNECTED);

			} else if (bytesToRead == Sample.BYTE_SIZE) {
				SendClientMessage(BtServiceResponse.STATE_SAMPELS);

			} else if (bytesToRead == Parameter.BYTE_SIZE) {
				SendClientMessage(BtServiceResponse.STATE_SYNCS);

			} else {
				SendClientMessage(BtServiceResponse.STATE_STOPPED);

			}
		}


		/**
		 * Poll winch for a {@link Parameter} or {@link Sample}.
		 * 
		 * Commands can be
		 * <ul>
		 * <li>GET_PARAMETERS - Get parameters until service receives BtServiceCommand.STOP.</li>
		 * <li>SET - Get next parameter. Same as set button on winch.</li>
		 * <li>GET_PARAMETER - Same as SET</li>
		 * <li>GET_SAMPLES - Get samples until service receives BtServiceCommand.STOP.</li>
		 * <li>GET_SAMPLE - Get a single sample</li>
		 * <li>DOWN - Same as down button on winch.</li>
		 * <li>UP - Same as down button on winch.</li>
		 * <li>NOCMD - Empty command. Do nothing.</li>
		 * </ul>
		 * 
		 * Skips all bytes in bluetooth input stream by calling btSkipStream() before sending the
		 * new
		 * command.
		 * 
		 * @param msg_command
		 */
		private void poll(BtServiceCommand msg_command) {
			btSkipStream();
			Command winschCmd = Command.NOCMD;
			bytesRead = 0;
			bytesToRead = 0;

			switch (msg_command) {
			case GET_PARAMETERS:
				tx_repeat = msg_command;
			case SET:
			case GET_PARAMETER:
				bytesToRead = Parameter.BYTE_SIZE;
				winschCmd = Command.SET;
				SendClientMessage(BtServiceResponse.STATE_SYNCS);
				break;

			case GET_SAMPLES:
				tx_repeat = msg_command;
			case GET_SAMPLE:
				bytesToRead = Sample.BYTE_SIZE;
				winschCmd = Command.GET;
				SendClientMessage(BtServiceResponse.STATE_SAMPELS);
				break;

			case DOWN:
				bytesToRead = Parameter.BYTE_SIZE;
				winschCmd = Command.DOWN;
				break;

			case UP:
				bytesToRead = Parameter.BYTE_SIZE;
				winschCmd = Command.UP;
				break;

			default:
				winschCmd = Command.NOCMD;
				return;
			}

			this.sendEmptyMessageDelayed(BtServiceCommand.TIMEOUT.Value(), BT_PACKAGE_TIMEOUT);

			// Send command and set a timeout
			logInfo("Send command: " + winschCmd.toString());
			btWrite(winschCmd.getByte());
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
				SendClientMessage(BtServiceResponse.SAMPLE_RECEIVED, bytes);
			} else if (bytesRead == Parameter.BYTE_SIZE) {
				logInfo("Parameter received.");
				SendClientMessage(BtServiceResponse.PARAMETER_RECEIVED, bytes);
			} else {
				Log.e(this.getClass().getSimpleName(), "Corrupted data received.");
			}

		}


		/**
		 * Skip all bytes in bluetooth input stream.
		 */
		private void btSkipStream() {
			int bytesAvailable;
			long skipped = 0;

			try {
				bytesAvailable = bt_instream.available();
				while (bytesAvailable > 0) {
					skipped += bt_instream.skip(bytesAvailable);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
					bytesAvailable = bt_instream.available();
				}

			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(),
						"IOException. Failed reading bluetooth instream. Check stack trace.");
				e.printStackTrace();
			}

			if (skipped > 0) {
				Log.d("btSkipInput", String.format("Skipped %d bytes.", skipped));
			}

		}


		/**
		 * Read serial data from the bluetooth.
		 * 
		 * Method attempt to place bytesToRead number of bytes into btRxBuffer.
		 * If available byte in the input stream > bytesToRead then excessive bytes are skipped from
		 * the start of the stream before filling btRxBuffer.
		 * 
		 * @return True when the number of bytes fill data package raw buffer.
		 */
		private boolean btReadStream() {
			int bytesAvailable;
			int remains = bytesToRead - bytesRead;

			try {
				bytesAvailable = bt_instream.available();
				bytesRead += bt_instream.read(btRxBuffer, bytesRead,
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
		 * Write bytes to bluetooth.
		 * 
		 * @param tx_bytes
		 * @return False if there was an IO exception..
		 */
		public boolean btWrite(byte[] tx_bytes) {
			try {
				bt_outstream.write(tx_bytes);
				bt_outstream.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}


		/**
		 * Write a single byte to bluetooth.
		 * 
		 * @param tx_bytes
		 * @return False if there was an IO exception.
		 */
		public boolean btWrite(byte tx_bytes) {
			return btWrite(new byte[] { tx_bytes });
		}

	};

}

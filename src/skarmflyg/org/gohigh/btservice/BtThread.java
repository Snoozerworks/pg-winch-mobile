package skarmflyg.org.gohigh.btservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;
import skarmflyg.org.gohigh.R;
import skarmflyg.org.gohigh.arduino.Command;
import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

//import android.util.Log;

public class BtThread extends HandlerThread {
	public final static byte MSG_STATE = 0;
	public final static byte MSG_RESULT = 1;
	private final static BluetoothAdapter btAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private static BluetoothDevice btDevice;
	private static Handler mainHandler;
	private static Context context;

	private WorkerHandler workerHandler;

	/**
	 * 
	 * @param name
	 */
	public BtThread(Context c, String name, Handler h) {
		super(name);
		mainHandler = h;
		context = c;
	}

	/**
	 * 
	 */
	@Override
	public synchronized void start() {
		super.start();

		// Create handler associated to this (worker) thread.
		workerHandler = new WorkerHandler();
		mainHandler.obtainMessage(ServiceResult.ANS_TXT.toInt(),
				"Hello main thread!");
	}

	/**
	 * Send a SETP command to get or set a parameter.
	 * <ul>
	 * <li>Get next parameter; If data is null, same as SELECT.</li>
	 * <li>Get a parameter; If data.length is 1, data[0] is parameter index.</li>
	 * <li>Set a parameter; If data.length is 3, data[0] is parameter index and
	 * data[1..2] is parameter value.</li>
	 * <ul>
	 * 
	 * @param cmd
	 *            Command BtServiceCommand.SETP
	 * @param data
	 *            Data to send
	 * @return False if thread is not started.
	 */
	public boolean sendCommand(BtServiceCommand cmd, byte[] data) {
		if (workerHandler == null) {
			Log.w(this.getClass().getSimpleName(),
					"Attempt to use null handler");
			return false;
		}
		workerHandler.obtainMessage(cmd.toInt(), data).sendToTarget();
		return true;
	}

	/**
	 * 
	 * @author markus
	 * 
	 */
	static class WorkerHandler extends Handler {
		// Change MAC-address here! ***
		final private String BT_MAC = "00:06:66:43:07:C0";
		// Bluetooth timeout in milliseconds.
		final private int BT_PACKAGE_TIMEOUT = 1000;
		// Bluetooth period of reads in milliseconds.
		final private int BT_PACKAGE_READ_PERIOD = 40;
		// Bluetooth UUID. General for serial communication.
		final private UUID BT_SPP_UUID = UUID
				.fromString("00001101-0000-1000-8000-00805F9B34FB");

		// Bluetooth socket and streams
		private BluetoothSocket btSocket;
		private InputStream btInstream;
		private OutputStream btOutstream;

		// Flag to get last command sent repeated.
		private BtServiceCommand tx_repeat;
		private byte[] btRxBuffer = new byte[Math.max(Parameter.BYTE_SIZE,
				Sample.BYTE_SIZE)];;
		private int bytesRead = 0;
		private int bytesToRead = 0;

		private int firstParamIndex = -1;

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

			BtServiceCommand msg_command = BtServiceCommand.toEnum(msg.what);

			switch (msg_command) {
			case DISCONNECT:
				// TODO Correct?
				onDisconnect();
				return;

			case STOP:
				// Stop any ongoing transaction of data.
				if (ServiceResult.toEnum(msg.arg1) == ServiceResult.PACKAGE_TIMEOUT
						|| ServiceResult.toEnum(msg.arg1) == ServiceResult.CONNECTION_TIMEOUT) {
					onTimeout();
				} else {
					onStop();
				}
				return;

			case GET_STATE:
				// Send current service state to client.
				onGetState();
				return;

			case CONNECT:
				onConnect();
				return;

			case _READ:
				// Read data from bluetooth. Only used by thread itself
				btReadStream();
				break;

			case DOWN:
			case GET_PARAMETERS:
			case GET_SAMPLE:
			case GET_SAMPLES:
			case SELECT:
			case SETP:
			case UP:
			default:
				if (bytesToRead == 0) {
					// No pending transfer. Ready to send a new command with
					// timeout.
					poll(msg);
				}
			}

			if (bytesToRead == bytesRead) {
				// Received all bytes requested
				byte[] bytes = Arrays.copyOf(btRxBuffer, bytesRead);

				if (bytesRead == Sample.BYTE_SIZE) {
					// logInfo("Sample received.");
					tellClient(ServiceResult.SAMPLE_RECEIVED, bytes);

				} else if (bytesRead == Parameter.BYTE_SIZE) {
					// logInfo("Parameter received.");
					tellClient(ServiceResult.PARAMETER_RECEIVED, bytes);

					int currentParamIndex = Parameter.getIndex(btRxBuffer);
					if (firstParamIndex < 0) {
						// Store first parameter index when syncronizing.
						firstParamIndex = currentParamIndex;
					} else if (firstParamIndex == currentParamIndex) {
						// All parameters syncronized.
						firstParamIndex = -1;
						tx_repeat = null;
					}

				} else {
					// Log.e(this.getClass().getSimpleName(),
					// "Corrupt data received.");
				}

				// Reset bytes read and to be read.
				bytesToRead = 0;
				bytesRead = 0;
				// this.removeCallbacksAndMessages(null);
				this.removeMessages(BtServiceCommand.STOP.toInt(),
						ServiceResult.PACKAGE_TIMEOUT.toInt());
				if (tx_repeat != null) {
					this.sendEmptyMessage(tx_repeat.toInt());
				} else {
					tellClient(ServiceState.STATE_STOPPED);
				}

			} else {
				// More data to receive.
				this.sendEmptyMessageDelayed(BtServiceCommand._READ.toInt(),
						BT_PACKAGE_READ_PERIOD);

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
			Command winschCmd = Command.NOCMD;
			byte[] tx_bytes = null;

			BtServiceCommand serviceCommand = BtServiceCommand.toEnum(msg.what);

			// Skip all bytes in stream
			btSkipStream();

			bytesRead = 0;
			bytesToRead = 0;

			switch (serviceCommand) {
			case GET_PARAMETERS:
				if (tx_repeat == null) {
					tx_repeat = serviceCommand;
				}
				tellClient(ServiceState.STATE_SYNCS);
			case SETP:
				winschCmd = Command.SETP;
				bytesToRead = Parameter.BYTE_SIZE;
				if (msg.obj != null) {
					tx_bytes = (byte[]) msg.obj;
				}
				break;

			case SELECT:
				winschCmd = Command.SET;
				bytesToRead = Parameter.BYTE_SIZE;
				break;

			case GET_SAMPLES:
				if (tx_repeat != serviceCommand) {
					tx_repeat = serviceCommand;
					tellClient(ServiceState.STATE_SAMPELS);
				}
			case GET_SAMPLE:
				bytesToRead = Sample.BYTE_SIZE;
				winschCmd = Command.GET;
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

			// Send a delayed message to handle timeout.
			Message m = this.obtainMessage(BtServiceCommand.STOP.toInt(),
					ServiceResult.PACKAGE_TIMEOUT.toInt());
			this.sendMessageDelayed(m, BT_PACKAGE_TIMEOUT);

			// Send command and set a timeout
			// Log.i(this.getClass().getSimpleName(),
			// "Send command: " + Command.values()[tx_bytes[0]].toString());

			// Send command and data (possibly null) to winsch.
			btWrite(winschCmd, tx_bytes);
		}

		/**
		 * Connect to bluetooth. Enable adapter and select device if necessary.
		 * Sends message MSG_BT_CONNECTED to client message handler once
		 * connected (also when already connected). Sends MSG_ANS_TXT with
		 * accompanied text if bluetooth is not supported or is not enabled.
		 * 
		 * This method setup the bluetooth socket, input stream and output
		 * stream.
		 */
		private void onConnect() {
			if (btIsConnected()) {
				tellClient(ServiceState.STATE_CONNECTED);
				return;
			}

			// Check bluetooth is supported
			if (btAdapter == null) {
				tellClient(ServiceResult.ANS_TXT,
						context.getText(R.string.bt_not_supported));
				return;
			}

			// Check bluetooth is enabled
			if (!btAdapter.isEnabled()) {
				tellClient(ServiceResult.ANS_TXT,
						context.getText(R.string.bt_disabled));
				return;
			}

			// Always cancel discovery before connecting
			btAdapter.cancelDiscovery();

			// Try get remote bluetooth device
			btDevice = btAdapter.getRemoteDevice(BT_MAC);

			// Create bluetooth rfcomm socket (serial communication)
			try {
				btSocket = btDevice
						.createRfcommSocketToServiceRecord(BT_SPP_UUID);
			} catch (IOException e) {
				tellClient(ServiceState.STATE_DISCONNECTED);
				tellClient(ServiceResult.ANS_TXT,
						"ERROR! Failed creating RFCOMM socket.");
				btSocket = null;
				return;
			}

			// Connect...
			try {
				btSocket.connect();
			} catch (IOException e) {
				tellClient(ServiceState.STATE_DISCONNECTED);
				tellClient(
						ServiceResult.ANS_TXT,
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
				tellClient(ServiceState.STATE_DISCONNECTED);
				tellClient(ServiceResult.ANS_TXT,
						"ERROR! Failed creating streams.");
				btInstream = null;
				btOutstream = null;
				e1.printStackTrace();
			}

		}

		/**
		 * Close input and output streams and the bluetooth socket.
		 * 
		 * @return
		 */
		private void onDisconnect() {
			onStop();

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

			btDevice = null;

			// Note: No need to send BtServiceStatus.STATE_DISCONNECTED. Will be
			// catched in the broadcast receiver in BTService listening got
			// bluetooth
			// connection/disconnection.
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
		private void onGetState() {

			if (!btIsConnected()) {
				tellClient(ServiceState.STATE_DISCONNECTED);

			} else if (bytesToRead == Sample.BYTE_SIZE) {
				tellClient(ServiceState.STATE_SAMPELS);

			} else if (bytesToRead == Parameter.BYTE_SIZE) {
				tellClient(ServiceState.STATE_SYNCS);

			} else {
				tellClient(ServiceState.STATE_STOPPED);

			}
		}

		/**
		 * Stops polling the winch for more data. Send STATE_STOPPED to client.
		 */
		private void onStop() {
			this.removeCallbacksAndMessages(null);
			tx_repeat = null;
			bytesToRead = 0;
			tellClient(ServiceState.STATE_STOPPED);
		}

		/**
		 * Sends, in order, PACKAGE_TIMEOUT and STATE_STOPPED to client.
		 * 
		 * Set bytesToRead to 0.
		 */
		private void onTimeout() {
			// Log.i(this.getClass().getSimpleName(), "Package timeout.");
			bytesToRead = 0;
			this.removeCallbacksAndMessages(null);
			tellClient(ServiceState.STATE_STOPPED);
			tellClient(ServiceResult.PACKAGE_TIMEOUT);
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
				// Log.d(this.getClass().getSimpleName(),
				// "IOException. Failed reading bluetooth instream. Check stack trace.");
				e.printStackTrace();
			}

			if (skipped > 0) {
				// Log.d("btSkipInput",
				// String.format("Skipped %d bytes.", skipped));
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
				// Log.d(this.getClass().getSimpleName(),
				// "IOException. Failed reading bluetooth instream. Check stack trace.");
				e.printStackTrace();
				return false;
			}

			return (bytesRead == bytesToRead);
		}

		/**
		 * Write command byte cmd and data array tx_bytes to bluetooth.
		 * 
		 * 
		 * @param cmd
		 *            Winsch command.
		 * @param tx_bytes
		 *            Data bytes.
		 * @return False if there was an IO exception.
		 */
		private boolean btWrite(Command cmd, byte[] tx_bytes) {
			try {
				btOutstream.write(cmd.getByte());
				if (tx_bytes != null) {
					btOutstream.write(tx_bytes);
				}
				btOutstream.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		/**
		 * Check if a bluetooth connection is established.
		 * 
		 * @return True if connected.
		 */
		private boolean btIsConnected() {
			return (btSocket != null && btSocket.isConnected());
		}

	};

	static private void tellClient(ServiceState r) {
		mainHandler.obtainMessage(ServiceState.ENUM_TYPE, r.toInt(), 0)
				.sendToTarget();
	}

	static private void tellClient(ServiceResult r) {
		mainHandler.obtainMessage(ServiceResult.ENUM_TYPE, r.toInt(), 0)
				.sendToTarget();
	}

	static private void tellClient(ServiceResult r, Object o) {
		mainHandler.obtainMessage(ServiceResult.ENUM_TYPE, r.toInt(), 0, o)
				.sendToTarget();
	}

}

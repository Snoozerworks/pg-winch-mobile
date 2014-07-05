package skarmflyg.org.gohigh.btservice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import skarmflyg.org.gohigh.arduino.Sample;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

public class SampleStoreFile extends SampleStore {
	private final String LOG_TAG = "SampleStoreFile";
	static int MAX_SAVE_SAMPLES;

	private int count = 0;
	private boolean write_en = false;
	private FileWriter fw;

	public SampleStoreFile(int size, int max_in_file) {
		super(size);
		MAX_SAVE_SAMPLES = max_in_file;
	}

	public void startWrite() {
		write_en = false;
		if (isExternalStorageWritable()) {
			File f = getFileStorageDir(createFileName());
			try {
				fw = new FileWriter(f);
				write_en = true;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public void stopWrite() {
		write_en = false;
		count = 0;
		if (fw == null) {
			return;
		}
		try {
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isWriting() {
		return write_en;
	}

	@Override
	public void add(Sample s) {
		super.add(s);

		if (count > MAX_SAVE_SAMPLES) {
			stopWrite();
			return;
		}

		if (write_en) {
			try {
				if (count == 0) {
					fw.append(Sample.csvHeaders());
				}
				fw.append(s.toCsv());
				count++;
			} catch (IOException e) {
				stopWrite();
				e.printStackTrace();
			}
		}

	};

	/**
	 * Checks if external storage is available for read and write
	 */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public File getFileStorageDir(String albumName) {
		// Get the directory for the user's public pictures directory.
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/Winsch");

		boolean folder_exist = true;
		if (!folder.exists()) {
			folder_exist = folder.mkdir();
		}

		if (folder_exist) {
			File file = new File(folder, albumName);
			return file;
		} else {
			Log.e(LOG_TAG, "Directory not created");
		}
		return null;

	}

	/**
	 * Creates a file name string ending with .csv
	 * 
	 * @return
	 */
	static private String createFileName() {
		Time today = new Time(Time.getCurrentTimezone());
		today.setToNow();
		return "rec_" + today.format3339(false) + ".csv";
	}

}

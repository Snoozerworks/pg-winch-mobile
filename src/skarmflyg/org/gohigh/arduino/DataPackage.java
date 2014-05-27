package skarmflyg.org.gohigh.arduino;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Abstract class defining a base class for the different types of data packages received from the Arduino.
 * 
 * @author Markus
 * 
 */
abstract public class DataPackage implements Parcelable {
	public Mode mode;
	public byte[] raw;
	final private byte byteSize;


	// final private Pattern formatstring;

	public DataPackage(byte byte_size) {
		byteSize = byte_size;
		raw = new byte[byteSize];
		// formatstring = Pattern.compile("{([^}]+)}");
	}


	abstract public void LoadBytes();


	public void LoadBytes(byte[] bytearr) {
		if (bytearr.length != byteSize) {
			return;
		}
		raw = Arrays.copyOf(bytearr, byteSize);
		LoadBytes();
	}


	@Override
	abstract public String toString();


	static public short byte2short(byte b1) {
		return (short) (b1 & 0xFF);
	}


	static public short byte2short(byte b1, byte b2) {
		return (short) (((b1 & 0xFF) << 8) | (b2 & 0xFF));
	}


	static public long byte2long(byte b1, byte b2, byte b3, byte b4) {
		return ((b1 & 0xFF) << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | (b4 & 0xFF);
	}


	public int describeContents() {
		return 0;
	}


	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByteArray(raw);
	}

}

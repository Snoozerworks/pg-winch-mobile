package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;

public interface BtServiceListener {
	public void onStateChange(ServiceState status);

	public void onPackageTimeout();

	public void onConnectionTimeout();

	public void onSampleReceived(Sample s);

	public void onParameterReceived(Parameter p);

	public void onText(CharSequence s);

	public void onRecordStateChange(boolean is_recording);

	/**
	 * Return a unique string for the instance.
	 * 
	 * @return A unique string
	 */
	public String getId();
}

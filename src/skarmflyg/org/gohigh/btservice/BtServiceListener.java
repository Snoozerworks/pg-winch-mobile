package skarmflyg.org.gohigh.btservice;

import skarmflyg.org.gohigh.arduino.Parameter;
import skarmflyg.org.gohigh.arduino.Sample;

public interface BtServiceListener {

	public void onStatusChange(BtServiceStatus status);

	// public void onBtResult(BtServiceResult response);

	public void onPackageTimeout();

	public void onConnectionTimeout();

	public void onSampleReceived(Sample s);

	public void onParameterReceived(Parameter p);

	public void onText(CharSequence s);

}

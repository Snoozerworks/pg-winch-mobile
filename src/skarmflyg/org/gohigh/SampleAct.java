package skarmflyg.org.gohigh;

import skarmflyg.org.gohigh.R.id;
import skarmflyg.org.gohigh.widgets.Meter;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class SampleAct extends Activity implements OnClickListener {
	static private Meter meter;
	private short pos = 0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sample);

		meter = (Meter) findViewById(id.meter1);

		// findViewById(id.FrameLayout1).setOnClickListener(this);
	}


	public void onClick(View v) {
		int delta = 80;

		// meter.setRawValue(pos);
		// pos += delta;
		// if (pos >= 160 || pos <= 0) {
		// delta *= -1;
		// }

	}
}

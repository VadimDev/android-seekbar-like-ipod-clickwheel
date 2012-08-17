package ru.clickwheel;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TestActivity extends Activity implements WheelModel.Listener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ClickWheel wheel = (ClickWheel) findViewById(R.id.wheel);
        wheel.getModel().addListener(this);
    }

	@Override
	public void onDialPositionChanged(WheelModel sender, int nicksChanged) {
		TextView text = (TextView) findViewById(R.id.text);
		text.setText(sender.getCurrentNick() + "");
	}
}
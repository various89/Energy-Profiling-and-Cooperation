package com.mc13.powprofile;
// icon: http://www.veryicon.com/icons/object/electric/green-battery.html

import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends Activity {
	private ToggleButton serviceToggle;
	private TextView currentBatteryVoltage;
	private TextView loggingInterval;
	boolean serviceEnabled;
	BroadcastReceiver localBroadcastReceiver;
	BroadcastReceiver broadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		serviceEnabled = isPowServiceRunning();
		currentBatteryVoltage = (TextView) findViewById(R.id.currentBatteryLevel);
		serviceToggle = (ToggleButton) findViewById(R.id.serviceToggle);
		loggingInterval = (TextView) findViewById(R.id.loggingInterval);


		
		serviceToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleService();
			}
		});
	}
	
	private boolean isPowServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (PowService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	protected void updateStats() {
		Time now = new Time();
		now.setToNow();
		currentBatteryVoltage.setText(now.format2445());
	}
	
	protected void toggleService() {
		if (!isPowServiceRunning()) {
			int interval = Integer.parseInt(loggingInterval.getText().toString());
			if ((interval < 1) || (interval > 60)) {
				Toast.makeText(this, "Invalid Logging Interval", Toast.LENGTH_LONG).show();
				return;
			}
			Intent serviceIntent = new Intent(this, PowService.class);
			serviceIntent.putExtra("loggingInterval", interval);
			startService(serviceIntent);
		} else {
			stopService(new Intent(this, PowService.class));
		}
		serviceToggle.setChecked(isPowServiceRunning());
		loggingInterval.setEnabled(!isPowServiceRunning());
	}

/*	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
*/
}

/**
 * 
 */
package com.mc13.powprofile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Christoph
 *
 */
public class PowService extends Service {

	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	private static final String TAG = "PowService";
	private IntentFilter ifilter;
	private Intent batteryStatus;
	private BroadcastReceiver batteryReceiver;
	private ScheduledExecutorService scheduleTaskExecutor;
	private HashMap<String,String> readings;
	private int serviceInterval;
	private ResultReceiver resultReceiver;
	private long startTimeStamp;
	private int startVoltage;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onCreate() {
		Toast.makeText(this, "PowService created", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onCreate");	
		ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	}	
	
	@Override	
	public void onDestroy() {	
		Toast.makeText(this, "PowService stopped", Toast.LENGTH_LONG).show();
		if (scheduleTaskExecutor != null) scheduleTaskExecutor.shutdownNow();
		//unregisterReceiver(batteryReceiver);
		Log.d(TAG, "onDestroy");
	}	
	
	@Override
	public void onStart(Intent intent, int startid) {	
		Log.d(TAG, "onStart");
		Bundle extras = intent.getExtras();
		resultReceiver = intent.getParcelableExtra("resultReceiver");
		if (extras == null) {
			serviceInterval = 1;
		} else {
			serviceInterval = extras.getInt("loggingInterval");
		}
		scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
		batteryStatus = registerReceiver(null, ifilter);
		
		startTimeStamp = System.currentTimeMillis();
		startVoltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
		
		scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
		        int scale = -1;
		        int level = -1;
		        int voltage = -1;
		        int temp = -1;
		        scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		        level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
				temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
		        readings = new HashMap<String,String> ();
		        readings.put("scale", Integer.toString(scale));
		        readings.put("level", Integer.toString(level));
		        readings.put("voltage", Integer.toString(voltage));
		        readings.put("voltageDelta", Integer.toString(startVoltage - voltage));
		        readings.put("temp", Integer.toString(temp));
		        long elapsedTimeSecs = (System.currentTimeMillis() - startTimeStamp) / 1000;
		        readings.put("elapsedSeconds", String.valueOf(elapsedTimeSecs));
		        Time now = new Time();
				now.setToNow();
				readings.put("time", now.format2445());
		        Bundle resultData = new Bundle();
		        for (Map.Entry<String, String> entry: readings.entrySet()) {
		        	resultData.putString(entry.getKey(), entry.getValue());
		        }
		        resultReceiver.send(0, resultData);
				Log.d(TAG, "voltage is "+voltage);
			}
		}, 0, serviceInterval, TimeUnit.SECONDS);
		Toast.makeText(this, "PowService started, Interval: " + String.valueOf(serviceInterval), Toast.LENGTH_LONG).show();
	}
}

package com.mc13.powprofile;
// icon: http://www.veryicon.com/icons/object/electric/green-battery.html

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
// import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends Activity {
	private ToggleButton serviceToggle;
	private TextView currentBatteryVoltage;
	private TextView loggedEvents;
	private TextView loggingInterval;
	private TextView secondsToLog;
	private TextView elapsedSeconds;
	private TextView lastUpdate;
	private TextView voltageDelta;
	private Button writeLog;
	private Button flushData;
	private long startTimestamp=0;
	private long runningTimeMS=1000 * 600;
	private List<Bundle> readingsList;
	private final String csvColumns[] = {"elapsedSeconds", "voltage", "voltageDelta", "scale", "level", "temp", "time"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		currentBatteryVoltage = (TextView) findViewById(R.id.currentBatteryLevel);
		loggedEvents = (TextView) findViewById(R.id.loggedEvents);
		voltageDelta = (TextView) findViewById(R.id.voltageDelta);
		serviceToggle = (ToggleButton) findViewById(R.id.serviceToggle);
		loggingInterval = (TextView) findViewById(R.id.loggingInterval);
		lastUpdate = (TextView) findViewById(R.id.lastUpdate);
		elapsedSeconds = (TextView) findViewById(R.id.elapsedSeconds);
		secondsToLog = (TextView) findViewById(R.id.secondsToLog);
		writeLog = (Button) findViewById(R.id.writeLog);
		writeLog.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				writeLogfile();
				
			}
		});
		
		flushData = (Button) findViewById(R.id.flushData);
		flushData.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				reset();
				
			}
		});

		serviceToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleService();
			}
		});
		
		reset();
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
	
	protected void toggleService() {
		if (!isPowServiceRunning()) {
			reset();
			int interval = Integer.parseInt(loggingInterval.getText().toString());
			startTimestamp = System.currentTimeMillis();
			runningTimeMS = Integer.parseInt(secondsToLog.getText().toString()) * 1000;
			
			if ((interval < 1) || (interval > 60)) {
				Toast.makeText(this, "Invalid Logging Interval", Toast.LENGTH_LONG).show();
				return;
			}
			if ((runningTimeMS < 1000) || (runningTimeMS > (1000 * 60 * 60 * 12))) {
				Toast.makeText(this, "Invalid Max Logging Time", Toast.LENGTH_LONG).show();
				return;
			}
			Intent serviceIntent = new Intent(this, PowService.class);
			serviceIntent.putExtra("loggingInterval", interval);
			serviceIntent.putExtra("resultReceiver", new ResultReceiver(new Handler()) {
				@Override
				protected void onReceiveResult(int resultCode, Bundle resultData) {
					readingsList.add(resultData);
					boolean stopService = false;
					if ((System.currentTimeMillis() - startTimestamp) >= runningTimeMS) {
						stopService = true;
					}
					redrawUI(stopService);
				}
			});
			startService(serviceIntent);
		} else {
			stopService(new Intent(this, PowService.class));
		}
		serviceToggle.setChecked(isPowServiceRunning());
		loggingInterval.setEnabled(!isPowServiceRunning());
		secondsToLog.setEnabled(!isPowServiceRunning());
		writeLog.setEnabled(!isPowServiceRunning());
		flushData.setEnabled(!isPowServiceRunning());
	}
	
	protected void reset() {
		readingsList = new ArrayList<Bundle>();
		redrawUI(false);
	}
	
	protected void redrawUI(boolean stopService) {
		if (readingsList != null) {
			// get last (newest) element
			if (!readingsList.isEmpty()) {
				Bundle lastBundle = readingsList.get(readingsList.size() - 1);
				currentBatteryVoltage.setText(lastBundle.getString("voltage"));
				elapsedSeconds.setText(lastBundle.getString("elapsedSeconds"));
				voltageDelta.setText(lastBundle.getString("voltageDelta"));
				lastUpdate.setText(lastBundle.getString("time"));
			} else {
				currentBatteryVoltage.setText("-");
				elapsedSeconds.setText("-");
				voltageDelta.setText("-");
				lastUpdate.setText("-");
			}
			loggedEvents.setText(String.valueOf(readingsList.size()));
		}
		if (stopService && isPowServiceRunning()) {
			toggleService();
		}
	}
	
	private void writeLogfile() {
		if (readingsList.isEmpty()) {
			Toast.makeText(this, "No log data yet ", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// we can write to the external storage
			String root = Environment.getExternalStorageDirectory().toString();
			File myDir = new File(root + "/pow_profile_logs");
			myDir.mkdirs();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			String ts = sdf.format(Calendar.getInstance().getTime());
			String fname = "powlog-" + ts + ".csv";
			Log.d("PowProfile", "filename is " + fname);
			File file = new File(myDir, fname);
			try {
				file.createNewFile();
				BufferedWriter buf = new BufferedWriter(new FileWriter(file,
						true));
				
				for(int i=0; i< csvColumns.length; i++) {
					buf.append(csvColumns[i]);
					buf.append(";");
				}
				buf.newLine();
				for (Bundle bundle : readingsList) {
					for(int i=0; i< csvColumns.length; i++) {
						buf.append(bundle.getString(csvColumns[i]));
						buf.append(";");
					}
					buf.newLine();
				}
				buf.newLine();
				buf.close();
				Toast.makeText(this, "Wrote /sdcard/pow_profile_logs/" + fname,
						Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this, "External Storage unavailable ",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onBackPressed() {
	    new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle("Closing PowProfile")
	        .setMessage("Are you sure you want to quit?")
	        .setPositiveButton("Yes", new DialogInterface.OnClickListener()
	    {
	        @Override
	        public void onClick(DialogInterface dialog, int which) {
	        	if (isPowServiceRunning()) {
	        		toggleService();
	        	}
	            finish();    
	        }

	    })
	    .setNegativeButton("No", null)
	    .show();
	}

/*	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
*/
}

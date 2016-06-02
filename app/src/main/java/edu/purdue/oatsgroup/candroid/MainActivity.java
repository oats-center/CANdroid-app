package edu.purdue.oatsgroup.candroid;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.util.Log;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;

import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;
import org.isoblue.can.CanSocketJ1939.Filter;

public class MainActivity extends Activity {

	// J1939 sockets for two interfaces
	private CanSocketJ1939 mSocket0;
	private CanSocketJ1939 mSocket1;

	// two asynctasks for streaming messages
	private MsgLoggerTask mCan0StreamTask;
	private MsgLoggerTask mCan1StreamTask;

	// message adapter
	private MsgAdapter mLog;

	// pop-up dialog fragments
	private FilterDialogFragment mFilterDialog;
	private WarningDialogFragment mWarningDialog;
	private FragmentManager mFm = getFragmentManager();

	// listviews for displaying filters and messages
	private ListView mMsgList;
	private ListView mFilterList;

	private boolean mIsCandroidServiceRunning;
	private boolean mSaveFiltered = false;

	// stuff needed for filters
	public static Filter mFilter;
	public static MsgAdapter mFilterItems;
	public static ArrayList<Filter> mFilters = new ArrayList<Filter>();

	private static final String can0 = "can0";
	private static final String can1 = "can1";

	private static final String TAG = "CandroidActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.d(TAG, "in onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initCandroid();
	}

	@Override
	protected void onResume() {

		super.onResume();
		registerReceiver(broadcastReceiver, new IntentFilter(CandroidService.BROADCAST_ACTION));
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {

		Log.d(TAG, "in onSaveInstanceState()");

		String[] values = mFilterItems.getValues();
		savedInstanceState.putStringArray("filtersList", values);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		super.onRestoreInstanceState(savedInstanceState);

		Log.d(TAG, "in onRestoreInstanceState()");
		String[] values = savedInstanceState.getStringArray("filtersList");
		if (values != null) {
			mFilterItems.addArray(values);
		}
	}

	@Override
	protected void onDestroy() {

		Log.d(TAG, "in onDestroy()");
		stopTask();
		closeCanSocket();
		super.onDestroy();
	}

	@Override
	protected void onPause() {

		Log.d(TAG, "in onPause()");
		unregisterReceiver(broadcastReceiver);
		super.onPause();
	}

	@Override
	protected void onStart() {

		Log.d(TAG, "in onStart()");
		if (isServiceRunning(CandroidService.class)) {
			ToggleButton b = (ToggleButton) findViewById(R.id.streamToggle);
			b.setChecked(true);
			onStreamGo();
		}
		super.onStart();
	}

	@Override
	protected void onStop() {

		Log.d(TAG, "in onStop()");
		stopTask();
		closeCanSocket();
		Log.d(TAG, "socket closed, task stopped");
		super.onStop();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.save_option).setChecked(mSaveFiltered);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();
		switch (id) {
			case R.id.add_filters:
				if (isServiceRunning(CandroidService.class)) {
					mWarningDialog.mWarningMsg = getResources().getString(R.string.new_filters);
					mWarningDialog.show(mFm, "warning");
				} else {
					mFilterDialog.show(mFm, "filter");
				}
				return true;
			case R.id.save_option:
				if (isServiceRunning(CandroidService.class)) {
					mWarningDialog.mWarningMsg = getResources().getString(R.string.change_log_opt);
					mWarningDialog.show(mFm, "warning");
				}
				if (item.isChecked()) {
					mSaveFiltered = false;
					item.setChecked(false);
				} else {
					mSaveFiltered = true;
					item.setChecked(true);
				}
				return true;
			case R.id.view_old_logs:
				Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/Log/");
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(selectedUri, "resource/folder");
				if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
					startActivity(intent);
				} else {
					Toast.makeText(this, "No file manager app found", Toast.LENGTH_LONG).show();
				}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/* callback for streamToggle */
	public void toggleListener(View view) throws IOException {

		ToggleButton b = (ToggleButton) view;

		if (b.isChecked()) {
			onStreamGo();
		} else {
			mIsCandroidServiceRunning =
				isServiceRunning(CandroidService.class);
			if (mIsCandroidServiceRunning) {
				mWarningDialog.mWarningMsg = getResources().getString(R.string.stop_logging);
				mWarningDialog.show(mFm, "warning");
			}
		}
	}

	/* function to check whether service is running */
	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager)
			getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service :
				manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
				}
		return false;
	}


	// receive CandroidService broadcast data to render old options
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "in OnReceive()");
			renderOldView(intent);
		}
	};

	private void renderOldView(Intent intent) {

		Bundle b = intent.getBundleExtra("serviceBundle");
		mSaveFiltered = b.getBoolean("saveOption");
		mFilters = (ArrayList<Filter>) b.getSerializable("filters");
		ArrayList<String> filterStrList = new ArrayList<String>();

		for (Filter f: mFilters) {
			filterStrList.add("Filtering on " + f.toString());
		}

		String[] filterStr = new String[filterStrList.size()];
		filterStr = filterStrList.toArray(filterStr);
		if (mFilterItems.isEmpty()) {
			mFilterItems.addArray(filterStr);
		}
	}

	public void initCandroid() {

		Log.d(TAG, "initializing parameters in initCandroid()");

		mLog = new MsgAdapter(this, 100);
		mFilterItems = new MsgAdapter(this, 20);
		mFilterDialog = new FilterDialogFragment();
		mWarningDialog = new WarningDialogFragment();

		mMsgList = (ListView) findViewById(R.id.msglist);
		mFilterList = (ListView) findViewById(R.id.filterlist);

		mMsgList.setAdapter(mLog);
		mFilterList.setAdapter(mFilterItems);
	}

	/* callback for adding new filters */
	public void onAddNewFilter() {

		onStreamStop();
		mFilterItems.clear();
	}

	/* callback for stop everything */
	public void onStreamStop() {

		stopTask();
		closeCanSocket();
		stopForegroundService();

		ToggleButton b = (ToggleButton) findViewById(R.id.streamToggle);
		b.setChecked(false);
	}

	/* callback for starting the logger */
	public void onStreamGo() {

		startTask();
		Log.d(TAG, "isServiceRunning: " + isServiceRunning(CandroidService.class));
		startForegroundService();
	}

	private void closeCanSocket() {

		if (mSocket0 != null) {
			try {
				mSocket0.close();
				mSocket0 = null;
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket0");
			}
		}
		if (mSocket1 != null) {
			try {
				mSocket1.close();
				mSocket1 = null;
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket1");
			}
		}
	}

	private void startTask() {

		Log.d(TAG, "in startTask(), start AsyncTask");
		if (mCan0StreamTask == null) {
			mCan0StreamTask = new MsgLoggerTask(can0);
			mCan0StreamTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSocket0);
		}
		if (mCan1StreamTask == null) {
			mCan1StreamTask = new MsgLoggerTask(can1);
			mCan1StreamTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSocket1);
		}
	}

	private void stopTask() {

		Log.d(TAG, "in stopTask(), cancel AsyncTask");
		if (mCan0StreamTask != null) {
			mCan0StreamTask.cancel(true);
			SystemClock.sleep(100);
			mCan0StreamTask = null;
		}
		if (mCan1StreamTask != null) {
			mCan1StreamTask.cancel(true);
			SystemClock.sleep(100);
			mCan1StreamTask = null;
		}
	}

	private void startForegroundService() {

		// start log service
		Intent logIntent = new Intent(CandroidService.FOREGROUND_START);
		logIntent.putExtra("save_option", mSaveFiltered);
		logIntent.putExtra("filter_list", mFilters);
		logIntent.setClass(MainActivity.this, CandroidService.class);
		startService(logIntent);

		// start kafka service
		Intent kafkaIntent = new Intent(KafkaService.FOREGROUND_START);
		kafkaIntent.setClass(MainActivity.this, KafkaService.class);
		startService(kafkaIntent);
	}

	private void stopForegroundService() {

		// stop log service
		Intent stopLogIntent = new Intent(CandroidService.FOREGROUND_STOP);
		stopLogIntent.setClass(MainActivity.this, CandroidService.class);
		stopService(stopLogIntent);

		// start log service
		Intent stopKafkaIntent = new Intent(KafkaService.FOREGROUND_STOP);
		stopKafkaIntent.setClass(MainActivity.this, KafkaService.class);
		stopService(stopKafkaIntent);
	}

	private class MsgLoggerTask extends AsyncTask
		<CanSocketJ1939, J1939Message, Void> {

			private String canInterface;

			private MsgLoggerTask(String canInterface) {
				this.canInterface = canInterface;
			}

			@Override
			protected Void doInBackground(CanSocketJ1939... socket) {
				try {
					socket[0] = new CanSocketJ1939(canInterface);
					socket[0].setPromisc();
					socket[0].setTimestamp();
					socket[0].setJ1939Filter(mFilters);
				} catch (IOException e) {
					Log.e(TAG, "socket creation on " + canInterface + " failed");
				}

				try {
					while (!isCancelled()) {
						if (socket[0] != null) {
							if (socket[0].select(1) == 0) {
								J1939Message msg = socket[0].recvMsg();
								publishProgress(msg);
							}
							if(isCancelled()) {
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			protected void onProgressUpdate(J1939Message... msg) {
				mLog.add(msg[0].toString());
			}

			protected void onPostExecute(Void Result) {
				// Do nothing
			}
		}
}

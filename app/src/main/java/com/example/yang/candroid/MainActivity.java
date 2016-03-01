package com.example.yang.candroid;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.util.Log;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;

import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;
import org.isoblue.can.CanSocketJ1939.Filter;

public class MainActivity extends Activity {
	private CanSocketJ1939 mSocket;
	private J1939Message mMsg;
	private MsgLoggerTask mMsgLoggerTask;
	private MsgAdapter mLog;
	private FilterDialogFragment mFilterDialog;
	private WarningDialogFragment mWarningDialog;
	private FragmentManager mFm = getFragmentManager();
	private ListView mMsgList;
	private ListView mFilterList;
	private boolean mIsCandroidServiceRunning;
	private boolean mSaveFiltered = true;
	public static Filter mFilter;
	public static MsgAdapter mFilterItems;
	public static ArrayList<Filter> mFilters = new ArrayList<Filter>();
	private static final String CAN_INTERFACE = "can0";
	private static final String TAG = "Candroid";
	private static final String msgFilter = "Adding new filter(s) will stop " +
		"current logging, do you wish to continue?";
	private static final String msgStop = "Stop logging and Candroid Service?";
	private static final String msgLogOpt = "Change log options will stop " +
		"current logging, do you wish to continue?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		mLog = new MsgAdapter(this, 100);
		mFilterItems = new MsgAdapter(this, 20);
		mMsgList = (ListView) findViewById(R.id.msglist);
		mFilterList = (ListView) findViewById(R.id.filterlist);
		mMsgList.setAdapter(mLog);
		mFilterList.setAdapter(mFilterItems);
		mFilterDialog = new FilterDialogFragment();
		mWarningDialog = new WarningDialogFragment();
    }

	@Override
	protected void onDestroy() {
		stopTask();
		closeCanSocket();
		super.onDestroy();
		Log.d(TAG, "socket closed, task stopped");
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
				mIsCandroidServiceRunning =
					isServiceRunning(CandroidService.class);
				if (mIsCandroidServiceRunning) {
					mWarningDialog.mWarningMsg = msgFilter;
					mWarningDialog.show(mFm, "warning");
				} else {
					mFilterDialog.show(mFm, "filter");
				}
				return true;
			case R.id.save_option:
				mIsCandroidServiceRunning =
					isServiceRunning(CandroidService.class);
				if (mIsCandroidServiceRunning) {
					mWarningDialog.mWarningMsg = msgLogOpt;
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	/* callback for streamToggle */
	public void toggleListener(View view) throws IOException {
		ToggleButton b = (ToggleButton) view;
		if (b.isChecked()) {
			onGo();
		} else {
			mIsCandroidServiceRunning =
				isServiceRunning(CandroidService.class);
			if (mIsCandroidServiceRunning) {
				mWarningDialog.mWarningMsg = msgStop;
				mWarningDialog.show(mFm, "warning");
			}
		}
	}

	/* callback for adding new filters */
	public void onAddNewFilter() {
		onStreamStop();
		mFilterItems.clear();
		mFilterDialog.show(mFm, "filter");
	}

	/* callback for stop everything */
	public void onStreamStop() {
		stopTask();
		closeCanSocket();
		mIsCandroidServiceRunning =
			isServiceRunning(CandroidService.class);
		if (mIsCandroidServiceRunning) {
			stopForegroundService();
		}
		ToggleButton b = (ToggleButton) findViewById(R.id.streamToggle);
		b.setChecked(false);
	}

	/* callback for starting the logger */
	public void onGo() {
		setupCanSocket();
		startTask();
		mIsCandroidServiceRunning =
			isServiceRunning(CandroidService.class);
		Log.d(TAG, "isServiceRunning: " + mIsCandroidServiceRunning);
		if (!mIsCandroidServiceRunning) {
			startForegroundService();
		}
	}

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

	private void setupCanSocket() {
		try {
			mSocket = new CanSocketJ1939(CAN_INTERFACE);
			mSocket.setPromisc();
			mSocket.setTimestamp();
			mSocket.setfilter(mFilters);
		} catch (IOException e) {
			Log.e(TAG, "socket creation on " + CAN_INTERFACE + " failed");
		}
	}

	private void closeCanSocket() {
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
	}

	private void startTask() {
		mMsgLoggerTask = new MsgLoggerTask();
		mMsgLoggerTask.execute(mSocket);
	}

	private void stopTask() {
		mMsgLoggerTask.cancel(true);
		mMsgLoggerTask = null;
	}

	private void startForegroundService() {
		Intent startForegroundIntent = new Intent(
				CandroidService.FOREGROUND_START);
		startForegroundIntent.putExtra("save_option", mSaveFiltered);
		startForegroundIntent.putExtra("filter_list", mFilters);
		startForegroundIntent.setClass(
				MainActivity.this, CandroidService.class);
		startService(startForegroundIntent);
	}

	private void stopForegroundService() {
		Intent stopForegroundIntent = new Intent(
				CandroidService.FOREGROUND_STOP);
		stopForegroundIntent.setClass(
				MainActivity.this, CandroidService.class);
		stopService(stopForegroundIntent);
	}

	private class MsgLoggerTask extends AsyncTask
		<CanSocketJ1939, J1939Message, Void> {
        @Override
        protected Void doInBackground(CanSocketJ1939... socket) {
            try {
                while (true) {
					if (socket[0] != null) {
						if (socket[0].select(10) == 0) {
							mMsg = socket[0].recvMsg();
							publishProgress(mMsg);
						} else {
							Log.i(TAG, "no J1939 msgs in past 10 seconds");
						}
						if(isCancelled()){
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

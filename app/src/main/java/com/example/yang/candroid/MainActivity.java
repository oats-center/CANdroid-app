package com.example.yang.candroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.util.Log;

import java.io.IOException;

import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;

public class MainActivity extends Activity {
	private CanSocketJ1939 mSocket;
	private J1939Message mMsg;
	private MsgLoggerTask mMsgLoggerTask;
	private MsgAdapter mLog;
	private ListView mMsgList;
	private static final String CAN_INTERFACE = "can0";
	private static final String TAG = "Candroid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		mLog = new MsgAdapter(this, 100);
		mMsgList = (ListView) findViewById(R.id.msglist);
		mMsgList.setAdapter(mLog);
		setupCanSocket();
		startTask();
    }

	@Override
	protected void onDestroy() {
		stopTask();
		closeCanSocket();
		super.onDestroy();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	private void setupCanSocket() {
		try {
			mSocket = new CanSocketJ1939(CAN_INTERFACE);
			mSocket.setPromisc();
			mSocket.setTimestamp();
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

	private class MsgLoggerTask extends AsyncTask
		<CanSocketJ1939, J1939Message, Void> {
        @Override
        protected Void doInBackground(CanSocketJ1939... socket) {
            try {
                while (true) {
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

package com.example.yang.candroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;

import org.apache.commons.io.input.TeeInputStream;
import static android.os.Environment.getExternalStorageDirectory;

import org.isoblue.can.CanSocket;
import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.Message;

public class MainActivity extends Activity {
	private CanSocketJ1939 mSocket;
	private Message mMsg;
	private MsgLoggerTask mMsgLoggerTask;
	private ArrayAdapter<String> mLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
            case R.id.action_delete_log:
                return true;
            case R.id.action_email_log:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleOnOff(View view) throws IOException {
        ToggleButton toggleButton = (ToggleButton) view;

        if(toggleButton.isChecked()){
			mSocket = new CanSocketJ1939("can0");
			mSocket.setPromisc();
			mSocket.setTimestamp();

			mLog = new ArrayAdapter<String>(this, R.layout.message);
			ListView listView = (ListView) findViewById(R.id.mylist);
			listView.setAdapter(mLog);
			mMsgLoggerTask = new MsgLoggerTask();
			mMsgLoggerTask.execute(mSocket);
        } else {
			mMsgLoggerTask.cancel(true);
			mMsgLoggerTask = null;
        	mSocket.close();
		}
    }

	private class MsgLoggerTask extends AsyncTask<CanSocketJ1939, Message, Void> {
        @Override
        protected Void doInBackground(CanSocketJ1939... socket) {
            try {
                while (true) {
					if (socket[0].select(10) == 0) {
						mMsg = socket[0].recvMsg();
                    	publishProgress(mMsg);
					} else {
						System.out.println("\nthere is no data");
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

        protected void onProgressUpdate(Message... msg) {
			mLog.add(msg[0].toString()); 
        }

        protected void onPostExecute(Void Result) {
            // Do nothing
        }
	}	
}

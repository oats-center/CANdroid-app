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

public class MainActivity extends Activity {

    private FileOutputStream mFos;
    private StartLogger mStartLogger;
    private ArrayAdapter<String> mTerminalArray;

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
                File dir = new File(Environment.getExternalStorageDirectory() + "/Log/");
                if (dir.isDirectory())
                {
                    String[] children = dir.list();
                    for (int i = 0; i < children.length; i++)
                    {
                        new File(dir, children[i]).delete();
                    }
                }
                return true;
            case R.id.action_email_log:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleOnOff(View view) throws IOException {
        ToggleButton toggleButton = (ToggleButton) view;

        if(mFos != null) {
            mFos.close();
        }

        if(toggleButton.isChecked()){
            Process p = null;
            try {
                p = (Process) Runtime.getRuntime().exec("su -c sh /data/local/can/candroid-up.sh");
            } catch (Exception e){
                e.printStackTrace();
            }
            long unixtime = System.currentTimeMillis() / 1000L;
            String timestamp = Long.toString(unixtime);
            String filename = timestamp + ".txt";
            try {
                mFos = new FileOutputStream(getExternalStorageDirectory() + "/Log/" + filename);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            TeeInputStream tis = new TeeInputStream(p.getInputStream(),mFos);

            mTerminalArray = new ArrayAdapter<String>(this, R.layout.message);

            mStartLogger = new StartLogger();
            mStartLogger.execute(tis);

        } else {
            mStartLogger.cancel(true);
            mStartLogger = null;
            ListView LstView = (ListView) findViewById(R.id.mylist);

            Process p1 = Runtime.getRuntime().exec("su -c sh /data/local/can/candroid-down.sh");
            InputStream is = p1.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                Log.w("candroid", line);
                mTerminalArray.add(line);
                LstView.setAdapter(mTerminalArray);
            }
        }
    }

    private class StartLogger extends AsyncTask<InputStream, String, Void> {
        @Override
        protected Void doInBackground(InputStream... params) {
            InputStreamReader isr = new InputStreamReader(params[0]);
            BufferedReader br = new BufferedReader(isr);
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    publishProgress(line);
                    if(isCancelled()){
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            ListView LstView = (ListView) findViewById(R.id.mylist);
            mTerminalArray.add(progress[0]);
            if (mTerminalArray.getCount() > 80) {
                mTerminalArray.clear();
            }
            LstView.setAdapter(mTerminalArray);
        }

        protected void onPostExecute(Void Result) {
            // Do nothing
        }
    }
}

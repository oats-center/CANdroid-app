package com.example.yang.candroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;

import org.apache.commons.io.input.TeeInputStream;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends Activity {

    private FileOutputStream mFos;
    private StartLogger mStartLogger;

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

    public void toggleOnOff(View view) throws IOException {
        ToggleButton toggleButton = (ToggleButton) view;

        if(mFos != null) {
            mFos.close();
        }

        if(toggleButton.isChecked()){
            Process p = null;
            try {
                p = (Process) Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-up.sh");
            } catch (Exception e){
                e.printStackTrace();
            }
            long unixtime = System.currentTimeMillis() / 1000L;
            String timestamp = Long.toString(unixtime);
            String filename = timestamp + ".log";
            try {
                mFos = new FileOutputStream(getExternalStorageDirectory() + "/" + filename);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            TeeInputStream tis = new TeeInputStream(p.getInputStream(),mFos);

            mStartLogger = new StartLogger();
            mStartLogger.execute(tis);

        } else {
            mStartLogger.cancel(true);
            mStartLogger = null;
            Process p1 = Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-down.sh");
            InputStream is = p1.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            TextView TxtView = (TextView) findViewById(R.id.terminal);
            ScrollView ScrView = (ScrollView) findViewById(R.id.scrollview);

            while ((line = br.readLine()) != null) {
                Log.w("candroid", line);
                TxtView.append(line + "\n");
                ScrView.fullScroll(ScrollView.FOCUS_DOWN);
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
            TextView TxtView = (TextView) findViewById(R.id.terminal);
            ScrollView ScrView = (ScrollView) findViewById(R.id.scrollview);
            TxtView.append(progress[0] + "\n");
            ScrView.fullScroll(ScrollView.FOCUS_DOWN);
        }

        protected void onPostExecute(Void Result) {
            // Do nothing
        }
    }
}

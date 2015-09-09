package com.example.yang.candroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.output.TeeOutputStream;

public class MainActivity extends Activity {

    private ToggleButton toggleButton1;
    private TextView txtview;
    private ScrollView scrollview;
    private Process p1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addListenerOnButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void addListenerOnButton() {

		toggleButton1 = (ToggleButton) findViewById(R.id.toggleButton);
        scrollview = (ScrollView) findViewById(R.id.scrollview);

		toggleButton1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

                try {
                    p1 = (Process) Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-up.sh");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Runnable txtviewlogStart = new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputStream is = p1.getInputStream();
                                    InputStreamReader isr = new InputStreamReader(is);
                                    BufferedReader br = new BufferedReader(isr);
                                    String line;

                                    txtview = (TextView) findViewById(R.id.terminal);

                                    while ((line = br.readLine()) != null) {
                                        Log.w("candroid", line);
                                        txtview.append(line + "\n");
                                        scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };

                Thread txtviewlogThread = new Thread(txtviewlogStart);

                Runnable savelog = new Runnable() {
                    @Override
                    public void run() {
                        String filename = new SimpleDateFormat("yyyyMMddhhmm'.log'").format(new Date());
                        FileOutputStream fos = openFileOutput(filename, MODE_WORLD_READABLE);
                    }
                }

                try {
                    if(toggleButton1.isChecked()){
                        txtviewlogThread.start();
                    } else {
                        try {
                            txtviewlogThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Process p = Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-down.sh");
                        InputStream is = p.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);
                        String line;

                        txtview = (TextView) findViewById(R.id.terminal);

                        while ((line = br.readLine()) != null) {
                            Log.w("candroid", line);
                            txtview.append(line + "\n");
                            scrollview.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
			}
		});

	}
}

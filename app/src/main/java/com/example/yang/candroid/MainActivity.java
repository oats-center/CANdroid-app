package com.example.yang.candroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ToggleButton;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private ToggleButton toggleButton1;

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

		toggleButton1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
                try {
                    if(toggleButton1.isChecked()){
                        Process p = Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-up.sh");
                        InputStream is = p.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);
                        String line;

                        while ((line = br.readLine()) != null) {
                            Log.w("candroid", line);
                        }
                    } else {
                        Process p = Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-down.sh");
                        InputStream is = p.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);
                        String line;

                        while ((line = br.readLine()) != null) {
                            Log.w("candroid", line);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
			}
		});

	}
}

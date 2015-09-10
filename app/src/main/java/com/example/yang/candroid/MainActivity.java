package com.example.yang.candroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_delete_log:
                File dir = new File(Environment.getExternalStorageDirectory() + "/log/");
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
                zipFolder(getExternalStorageDirectory() + "/log/", getExternalStorageDirectory().toString());
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"email@example.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "subject here");
                intent.putExtra(Intent.EXTRA_TEXT, "body text");
                File root = Environment.getExternalStorageDirectory();
                File file = new File(root, "log.zip");
                if (!file.exists() || !file.canRead()) {
                    Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
                    finish();
                    return false;
                }
                Uri uri = Uri.fromFile(file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send email..."));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static void zipFolder(String inputFolderPath, String outZipPath) {
    try {
        FileOutputStream fos = new FileOutputStream(outZipPath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        File srcFile = new File(inputFolderPath);
        File[] files = srcFile.listFiles();
        Log.w("", "Zip directory: " + srcFile.getName());
        for (int i = 0; i < files.length; i++) {
            Log.w("", "Adding file: " + files[i].getName());
            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(files[i]);
            zos.putNextEntry(new ZipEntry(files[i].getName()));
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
            fis.close();
        }
        zos.close();
    } catch (IOException ioe) {
        ioe.printStackTrace();
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
                p = (Process) Runtime.getRuntime().exec("su -c sh /data/local/tmp/scripts/candroid-up.sh");
            } catch (Exception e){
                e.printStackTrace();
            }
            long unixtime = System.currentTimeMillis() / 1000L;
            String timestamp = Long.toString(unixtime);
            String filename = timestamp + ".log";
            try {
                mFos = new FileOutputStream(getExternalStorageDirectory() + "/log/" + filename);
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

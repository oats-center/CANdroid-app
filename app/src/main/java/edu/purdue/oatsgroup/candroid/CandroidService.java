package edu.purdue.oatsgroup.candroid;

import android.app.Notification;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.support.v4.app.NotificationCompat.Builder;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;
import org.isoblue.can.CanSocketJ1939.Filter;

public class CandroidService extends Service {
	public static final String FOREGROUND_STOP =
		"edu.purdue.oatsgroup.candroid.CandroidService.FOREGROUND.stop";
	public static final String FOREGROUND_START =
		"edu.purdue.oatsgroup.candroid.CandroidService.FOREGROUND.start";
	public static final String BROADCAST_ACTION =
		"edu.purdue.oatsgroup.candroid.CandroidService.broadcast";
	public static final int NOTIFICATION_ID = 102;

	public CanSocketJ1939 mSocket0;
	public CanSocketJ1939 mSocket1;

	private ArrayList<Filter> mFilters = new ArrayList<Filter>();

	private boolean mSaveFiltered = false;

	private FileOutputStream mFos;
	private OutputStreamWriter mOsw;
	private Intent bcIntent;

	private logThread mT0;
	private logThread mT1;

	private static final String can0 = "can0";
	private static final String can1 = "can1";

	private static final String TAG = "CandroidService";

	@Override
	public void onCreate() {

		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}

	@Override
	public void onDestroy() {

		if (mT0 != null) {
			mT0.stop();
			mT0 = null;
		}

		if (mT1 != null) {
			mT1.stop();
			mT1 = null;
		}

		super.onDestroy();
		Log.d(TAG, "in onDestroy(), destroy " + TAG);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		postOldData();

		if (mSocket0 == null && mSocket1 == null) {
			if (FOREGROUND_START.equals(intent.getAction())) {
				Log.i(TAG, "in onStartCommmand(), start " + TAG);
				startForeground(NOTIFICATION_ID, getCompatNotification());
			}

			mFilters = (ArrayList<Filter>) intent.getSerializableExtra("filter_list");
			mSaveFiltered = intent.getExtras().getBoolean("save_option");

			mT0 = new logThread(can0, mSocket0);
			mT1 = new logThread(can1, mSocket1);

			mT0.start();
			mT1.start();
		}

		return START_STICKY;
	}

	private void postOldData() {

		Log.d(TAG, "in postOldData()");
		bcIntent = new Intent(BROADCAST_ACTION);
		Bundle b = new Bundle();
		b.putBoolean("saveOption", mSaveFiltered);
		b.putSerializable("filters", mFilters);
		bcIntent.putExtra("serviceBundle", b);
		sendBroadcast(bcIntent);
	}

	private void createFile() {

		long unixtime = System.currentTimeMillis() / 1000L;
		String timestamp = Long.toString(unixtime);
		String filename = timestamp + ".log";
		try {
			mFos = new FileOutputStream("/sdcard/Log/" + filename);
			mOsw = new OutputStreamWriter(mFos);
		} catch (Exception e) {
			Log.e(TAG, "cannot create fd");
			return;
		}
	}

	private Notification getCompatNotification() {

		Builder builder = new Builder(this);
		builder.setSmallIcon(R.drawable.computer)
				.setContentTitle("CANdroid logging messages ...")
				.setContentText("Click to return")
				.setWhen(System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(
				this, 0, notificationIntent, 0);
		builder.setContentIntent(contentIntent);
		Notification notification = builder.build();

		return notification;
	}

	public class logThread implements Runnable {

		Thread logThread;

		public String canInterface;
		public CanSocketJ1939 socket;

		public logThread(String canInterface, CanSocketJ1939 socket) {
			this.canInterface = canInterface;
			this.socket = socket;
		}

		public void start() {
			if (logThread == null) {
				logThread = new Thread(this);
				logThread.start();
			}
		}

		public void run() {
			try {
				socket = new CanSocketJ1939(canInterface);
				socket.setPromisc();
				socket.setTimestamp();
			} catch (IOException e) {
				Log.e(TAG, "socket creation on " + canInterface + " failed.");
			}

			while (!logThread.interrupted()) {
				try {
					if (socket.select(1) == 0) {
						J1939Message msg = socket.recvMsg();
						if (mOsw == null) {
							createFile();
						}
						try {
							mOsw.append(msg.toString() + "\n");
						} catch (Exception e) {
							Log.e(TAG, "cannot append to file");
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "cannot select on socket");
				}
			}
		}

		public void stop() {
			if (logThread != null) {
				logThread.interrupt();
			}
			
			try {
				if (mOsw != null) {
					mOsw.close();
				}
				if (mFos != null) {
					mFos.close();
				}
			} catch (IOException e) {
				Log.e(TAG, "cannot close fd");
			}

			if (socket != null) {
				try {
					socket.close();
					socket = null;
				} catch (IOException e) {
					Log.e(TAG, "cannot close socket");
				}
			}
		}
	}

}

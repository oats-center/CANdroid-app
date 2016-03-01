package com.example.yang.candroid;

import android.app.Notification;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
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
			"com.example.yang.candroid.CandroidService.FOREGROUND.stop";
	public static final String FOREGROUND_START =
	"com.example.yang.candroid.CandroidService.FOREGROUND.start";
	public static final int NOTIFICATION_ID = 101;
	private static final String TAG = "CandroidService";
	private static final String CAN_INTERFACE = "can0";
	private CanSocketJ1939 mSocket;
	private ArrayList<Filter> mFilters = new ArrayList<Filter>();
	public J1939Message mMsg;
	private FileOutputStream mFos;
	private OutputStreamWriter mOsw;
	private Handler msgHandler;
	private recvThread mThread;

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
		mThread.stop();
		super.onDestroy();
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
				.setContentTitle("Candroid Logging Started")
				.setWhen(System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(
			this, 0, notificationIntent, 0);
		builder.setContentIntent(contentIntent);
		Notification notification = builder.build();
		return notification;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "in onStartCommmand...");
		if (FOREGROUND_START.equals(intent.getAction())) {
			Log.i(TAG, "Starting CandroidService");
			startForeground(NOTIFICATION_ID,
							getCompatNotification());
		}

		msgHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
			}
		};
		mFilters = (ArrayList<Filter>) intent.getSerializableExtra("filter_list");

		setupCanSocket();
		mThread = new recvThread();
		mThread.start();

		return START_STICKY;
	}

	public class recvThread implements Runnable {
		Thread recvThread;

		public void start() {
			if (recvThread == null) {
				recvThread = new Thread(this);
				recvThread.start();
			}
		}

		public void run() {
			while (!recvThread.interrupted()) {
				try {
					if (mSocket.select(10) == 0) {
						mMsg = mSocket.recvMsg();
						if (mOsw == null) {
							createFile();
						}
						try {
							mOsw.append(mMsg.toString() + "\n");
						} catch (Exception e) {
							Log.e(TAG, "cannot append to file");
						}
					} else {
						msgHandler.sendEmptyMessage(0);
					}
				} catch (IOException e) {
					Log.e(TAG, "cannot select on socket");
				}
			}
		}

		public void stop() {
			if (recvThread != null) {
				recvThread.interrupt();
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
			closeCanSocket();
		}
	}

}

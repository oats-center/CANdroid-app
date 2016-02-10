package com.example.yang.candroid;

import android.app.Notification;
import android.app.Service;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.support.v4.app.NotificationCompat.Builder;

public class CandroidService extends Service {
	public static final String FOREGROUND_START =
	"com.example.yang.candroid.CandroidService.FOREGROUND.start";
	public static final String FOREGROUND_STOP =
	"com.example.yang.candroid.CandroidService.FOREGROUND.stop";
	public static final int NOTIFICATION_ID = 101;
	private static final String TAG = "CandroidService";

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private Notification getCompatNotification() {
		Builder builder = new Builder(this);
		builder.setSmallIcon(R.drawable.computer)
				.setContentTitle("Candroid Logging Started")
				.setTicker("Logging...")
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

		return START_STICKY;
	}

}

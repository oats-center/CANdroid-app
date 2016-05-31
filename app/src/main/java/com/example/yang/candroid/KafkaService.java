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
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;

public class KafkaService extends Service {

	public static final String FOREGROUND_STOP =
		"com.example.yang.candroid.KafkaService.FOREGROUND.stop";
	public static final String FOREGROUND_START =
		"com.example.yang.candroid.KafkaService.FOREGROUND.start";
	public static final String BROADCAST_ACTION =
		"com.example.yang.candroid.KafkaService.broadcast";
	public static final int NOTIFICATION_ID = 101;

	public CanSocketJ1939 mSocket0;
	public CanSocketJ1939 mSocket1;

	public Properties mConfig;
	public ProducerRecord<String, String> mProducerRecord;
	public static Producer<String, String> mProducer;

	public kafkaThread mT0;
	public kafkaThread mT1;

	private static final String can0 = "can0";
	private static final String can1 = "can1";

	private static final String can0Topic = "raw-can0";
	private static final String can1Topic = "raw-can1";

	private static final String candroidID = "candroid0";

	private static final String TAG = "KafkaService";

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

		if (mSocket0 == null && mSocket1 == null) {
			if (FOREGROUND_START.equals(intent.getAction())) {
				Log.i(TAG, "in onStartCommmand(), start " + TAG);
				startForeground(NOTIFICATION_ID,
						getCompatNotification());
			}

			setupKafkaProducer();

			mT0 = new kafkaThread(can0Topic, candroidID, can0, mSocket0);
			mT1 = new kafkaThread(can1Topic, candroidID, can1, mSocket1);
			mT0.start();
			mT1.start();
		}

		return START_STICKY;
	}

	private Notification getCompatNotification() {

		Builder builder = new Builder(this);
		builder.setSmallIcon(R.drawable.computer)
				.setContentTitle("Kafka pushing messages ...")
				.setWhen(System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(
				this, 0, notificationIntent, 0);
		builder.setContentIntent(contentIntent);
		Notification notification = builder.build();

		return notification;
	}

	private void setupKafkaProducer() {

		mConfig = new Properties();
		mConfig.put("bootstrap.servers", "vip4.ecn.purdue.edu:9092");
		mConfig.put("acks", "all");
		mConfig.put("retries", 0);
		mConfig.put("batch.size", 16384);
		mConfig.put("linger.ms", 1);
		mConfig.put("buffer.memory", 33554432);
		mConfig.put("key.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		mConfig.put("value.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");

	}

	public class kafkaThread implements Runnable {

		Thread kafkaThread;

		public String topic;
		public String candroidKey;
		public String canInterface;
		public CanSocketJ1939 socket;

		public kafkaThread(String topic, String candroidKey, String canInterface,
				CanSocketJ1939 socket) {
			this.topic = topic;
			this.candroidKey = candroidKey;
			this.canInterface = canInterface;
			this.socket = socket;
		}

		public void start() {
			if (kafkaThread == null) {
				kafkaThread = new Thread(this);
				kafkaThread.start();
			}
		}

		public void run() {
			if (mProducer == null) {
				mProducer = new KafkaProducer<>(mConfig);
			}

			try {
				socket = new CanSocketJ1939(canInterface);
				socket.setPromisc();
				socket.setTimestamp();
			} catch (IOException e) {
				Log.e(TAG, "socket creation on " + canInterface + " failed.");
			}

			while (!kafkaThread.interrupted()) {
				try {
					if (socket.select(1) == 0) {
						J1939Message msg = socket.recvMsg();
						mProducer.send(new ProducerRecord<String, String>(topic, candroidKey, msg.toString()));
					}
				} catch (IOException e) {
					Log.e(TAG, "cannot select on socket");
				}
			}
		}

		public void stop() {
			if (kafkaThread != null) {
				kafkaThread.interrupt();
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

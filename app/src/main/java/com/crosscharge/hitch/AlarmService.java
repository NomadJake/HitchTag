package com.crosscharge.hitch;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends Service {

    private static String TAG = AlarmService.class.getName();
    int cnt = 10;

    MediaPlayer p;
    Vibrator v;

    public AlarmService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Thread alarmThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(cnt > 0){
                    Log.d("Alarm!!!", cnt + "");
                    // play alarm here
                    v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);

                    p = MediaPlayer.create(getApplicationContext(), R.raw.beep);
                    p.start();

                    try {
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cnt--;
                }
                stopAlarmService();
            }
        });
        alarmThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.NOTIFICATION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.NOTIFICATION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Intent previousIntent = new Intent(this, AlarmService.class);
            previousIntent.setAction(Constants.NOTIFICATION.PREV_ACTION);
            PendingIntent ppreviousIntent = PendingIntent.getService(this, 0, previousIntent, 0);

            Intent playIntent = new Intent(this, AlarmService.class);
            playIntent.setAction(Constants.NOTIFICATION.PLAY_ACTION);
            PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);

            Intent nextIntent = new Intent(this, AlarmService.class);
            nextIntent.setAction(Constants.NOTIFICATION.NEXT_ACTION);
            PendingIntent pnextIntent = PendingIntent.getService(this, 0, nextIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setTicker("Tag Lost")
                    .setContentText("Alarm")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_previous,    "Previous",     ppreviousIntent)
                    .addAction(android.R.drawable.ic_media_play,        "Play",         pplayIntent)
                    .addAction(android.R.drawable.ic_media_next,        "Next",         pnextIntent).build();
            startForeground(Constants.NOTIFICATION.ALARM_SERVICE, notification);
        }
        else if (intent.getAction().equals(Constants.NOTIFICATION.PREV_ACTION)) {
            Log.i(TAG, "Clicked Previous");
            cnt = 0;
        }
        else if (intent.getAction().equals(Constants.NOTIFICATION.PLAY_ACTION)) {
            Log.i(TAG, "Clicked Play");
            cnt = 0;
        }
        else if (intent.getAction().equals(Constants.NOTIFICATION.NEXT_ACTION)) {
            Log.i(TAG, "Clicked Next");
            cnt = 0;
        }
        else if (intent.getAction().equals(Constants.NOTIFICATION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent - alarm");
            cnt = 0;
            stopAlarmService();
        }
        return START_STICKY;
    }

    private void stopAlarmService(){
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "In onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}

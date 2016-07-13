package com.crosscharge.hitch;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;

/**
 * Created by nomad on 12/7/16.
 */
public class fService extends Service{

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice device;
    private BluetoothGatt deviceGatt;
    Context context;
    HitchTag thisTag;
    Boolean keepTracking = true;
    Boolean kill = false;
    String deviceaddress;
    Boolean trackingModeLong;


    String TAG = "Service";
    String mode;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

        super.onCreate();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();

        context = getApplicationContext();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        device =(BluetoothDevice) intent.getExtras().get("device");
        if(device == null){
            Log.d("service","stopping service : device null");
            super.stopSelf();
        }
//        deviceaddress = (String)intent.getExtras().get("deviceaddress");
        trackingModeLong = (Boolean)intent.getExtras().getBoolean("trackingModeLong");
        if(trackingModeLong){
            mode = "Wide coverage";
        }else {
            mode = "Short coverage";
        }

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notificationSound);
        r.play();


        if (intent.getAction().equals("fore")) {

            if(deviceGatt == null){
                connect();
            }

            if (trackingModeLong) {
                new TrackHitchTagLongThread().start();
                Log.d("thread","started tracking thread long mode");



            } else {
                new TrackThread().start();
                Log.d("thread","started tracking thread near mode");

            }
            Log.i("trackingService", "Received Start Foreground Intent ");


            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction("main");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);


            Intent startTrackingIntent = new Intent(this, fService.class);
            startTrackingIntent.putExtra("device",device);
            startTrackingIntent.putExtra("trackingModeLong",trackingModeLong);
            startTrackingIntent.setAction("start");
            PendingIntent pstartTrackingIntent = PendingIntent.getService(this, 0,
                    startTrackingIntent, 0);

            Intent stopTrackingIntent = new Intent(this, fService.class);
            stopTrackingIntent.putExtra("device",device);
            stopTrackingIntent.putExtra("trackingModeLong",trackingModeLong);
            stopTrackingIntent.setAction("fore");
            PendingIntent pstopTrackingIntent = PendingIntent.getService(this, 0,
                    stopTrackingIntent, 0);

            Intent killService = new Intent(this, fService.class);
            stopTrackingIntent.setAction("kill");
            PendingIntent pkillService = PendingIntent.getService(this, 0,
                    killService, 0);



            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.hitchlogo);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Hitch")
                    .setTicker("Hitching")
                    .setContentText("Tracking Mode : " + mode)
                    .setSmallIcon(R.drawable.hitchinlogo)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 200, 200, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_play, "start",
                            pstartTrackingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "stop",
                            pstopTrackingIntent)
                    .addAction(android.R.drawable.ic_lock_power_off, "kill",
                            pkillService).build();
            startForeground(101,
                    notification);
        } else if (intent.getAction().equals("start")) {
            Log.i("trackingService", "Clicked start tracking");
            keepTracking = true;

        } else if (intent.getAction().equals("stop")) {
            Log.i("trackingService", "Received Stop Foreground Intent");
            stopAlarm();
            keepTracking = false;
//            kill = true;

        }else if (intent.getAction().equals("kill")) {
            Log.d("kill", " kill service intent by user");
            stopSelf();
        }
        return START_STICKY;
    }

//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        public void onReceive (Context context, Intent intent) {
//            String action = intent.getAction();
//
//            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
//                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
//                        == BluetoothAdapter.STATE_OFF) {
//
//                }
//            }
//
//        }
//
//    };

    private boolean enableBLE(){
        boolean ret = true;
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {

            Log.d(TAG, "BLE disabled.");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            ret = false;
        }
        return ret;
    }

    public boolean deviceAvailable(){
        return this.device != null;
    }

    public boolean connected(){
        return this.deviceGatt != null;
    }

    public void connect(){
            deviceGatt = device.connectGatt(context, false, masterCallBack);
    }

    public void disconnect(){
        if(deviceGatt != null){
            deviceGatt.disconnect();
        }
    }

    private final BluetoothGattCallback masterCallBack = new BluetoothGattCallback() {

        int trackCnt = 4;

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // connected - discover services now
                gatt.discoverServices();
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED){
                // disconnected
                deviceGatt = null;
            }
            if (status == BluetoothGatt.GATT_FAILURE) {
                // failure
                playAlarm();
                disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("service", gatt.toString() + ", " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

            super.onReadRemoteRssi(gatt, rssi, status);
            if(false){
                Log.d("FIND", "rssi = " + rssi);
                int bars = 0;
                if(rssi < -80 || rssi == 0){
                    bars = 0;
                }
                else if(rssi < -65){
                    bars = 1;
                }
                else if(rssi < -50){
                    bars = 2;
                }
                else if(rssi < -35){
                    bars = 3;
                }
                else{
                    bars = 4;
                }
            }
            else if(true){
                Log.d("TRACK", "rssi = " + rssi);
                if(rssi < -90 || rssi == 0){
                    trackCnt --;
                    if(trackCnt == 0){
                        playAlarm();
                        try {
                            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                            r.play();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else if(trackCnt < -8){
                        stopAlarm();
                        disconnect();
                    }
                }
                else{
                    trackCnt = 4;
                    stopAlarm();
                }
                Log.d("TRACK", "trackCnt = " + trackCnt);
            }
        }
    };

    public void stopAlarm(){

        Intent stopIntent = new Intent(context, AlarmService.class);
        stopIntent.setAction(Constants.NOTIFICATION.STOPFOREGROUND_ACTION);
        context.startService(stopIntent);
    }

    public class TrackHitchTagLongThread extends Thread{
        @Override
        public void run() {
            super.run();

            while(keepTracking){
                Log.d("service", "i am tracking ... ");
                if(!connected()){
                    playAlarm();

                }else {
                    stopAlarm();
                }
                if (kill == true) stopAlarm();
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    public class TrackThread extends Thread{

        @Override
        public void run() {
            super.run();

            while(deviceGatt != null){
                deviceGatt.readRemoteRssi();
                try {
                    Thread.currentThread().sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    public void playAlarm(){
        Intent startIntent = new Intent(context, AlarmService.class);
        startIntent.setAction(Constants.NOTIFICATION.STARTFOREGROUND_ACTION);
        context.startService(startIntent);
    }

}

package com.crosscharge.hitch;

import android.app.Notification;
import android.app.NotificationManager;
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
import android.content.IntentFilter;
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
    Boolean keepTracking = true;
    Boolean kill = false;
    Boolean trackingModeLong;
    NotificationManager notificationManager;
    Notification stateHolderNotification;
    NotificationCompat.Builder builder;
    TrackThread persistentThread;



    String TAG = "fServiceLOG";
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
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notificationSound);
        r.play();


        if (intent.getAction().equals("fore")) {

            keepTracking = true;

            device =(BluetoothDevice) intent.getExtras().get("device");
            if(device == null){
                Log.d(TAG,"stopping service : device null");
                super.stopSelf();
            }
            trackingModeLong = (Boolean)intent.getExtras().getBoolean("trackingModeLong");
            if(trackingModeLong){
                mode = "Wide coverage";
            }else {
                mode = "Short coverage";
            }

            if(deviceGatt == null){
                connect();
            }

            if (trackingModeLong) {
                Log.d(TAG,"started tracking thread long mode");

                IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
                IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                this.registerReceiver(mReceiver, filter1);
                this.registerReceiver(mReceiver, filter2);
                this.registerReceiver(mReceiver, filter3);



            } else {
                persistentThread = new TrackThread();
                persistentThread.start();
                Log.d(TAG,"started tracking thread near mode");

            }
            Log.d(TAG, "Received Start Foreground Intent ");


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
                    startTrackingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent stopTrackingIntent = new Intent(this, fService.class);
            stopTrackingIntent.putExtra("device",device);
            stopTrackingIntent.putExtra("trackingModeLong",trackingModeLong);
            stopTrackingIntent.setAction("stop");
            PendingIntent pstopTrackingIntent = PendingIntent.getService(this, 0,
                    stopTrackingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent killService = new Intent(this, fService.class);
            killService.putExtra("device",device);
            killService.setAction("kill");
            PendingIntent pkillService = PendingIntent.getService(this, 0,
                    killService, PendingIntent.FLAG_UPDATE_CURRENT);


            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.hitchlogo);

            builder = new NotificationCompat.Builder(context);

            builder.setContentTitle("Hitch");
            builder.setContentText("Tracking Mode : " + mode);
            builder.setSmallIcon(R.drawable.hitchinlogo);
            builder.setLargeIcon(Bitmap.createScaledBitmap(icon, 200, 200, false));
            builder.setContentIntent(pendingIntent);
            builder.setOngoing(true);
            builder.addAction(android.R.drawable.ic_media_play,"start",pstartTrackingIntent);
            builder.addAction(android.R.drawable.ic_media_pause, "stop",
                    pstopTrackingIntent);
            builder.addAction(android.R.drawable.ic_lock_power_off, "kill",
                    pkillService);

            builder.setPriority(Notification.PRIORITY_HIGH);
            stateHolderNotification = builder.build();



            startForeground(101,
                    stateHolderNotification);
        } else if (intent.getAction().equals("start")) {
            Log.d(TAG, "restart service clicked");
            keepTracking = true;
            builder.setContentText("Tracking Mode : " + mode);
            IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            this.registerReceiver(mReceiver, filter1);
            this.registerReceiver(mReceiver, filter2);
            this.registerReceiver(mReceiver, filter3);

            if(!trackingModeLong){
                new TrackThread().start();
            }

            notificationManager.notify(101,builder.build());



        } else if (intent.getAction().equals("stop")) {
            Log.d(TAG, "pause tracking clicked");

            device =(BluetoothDevice) intent.getExtras().get("device");
            trackingModeLong = (Boolean)intent.getExtras().getBoolean("trackingModeLong");


            stopAlarm();
            if (persistentThread != null) {
                persistentThread.interrupt();
            }
            try {
                persistentThread.stop();
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
//            stopForeground(true);
            keepTracking = false;
            String pausedText = "Tracking Paused";
            builder.setContentText(pausedText);
            notificationManager.notify(101,builder.build());

        }else if (intent.getAction().equals("kill")) {
            Log.d(TAG, " kill service intent by user");
            disconnect();
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }




    private boolean enableBLE(){
        boolean ret = true;

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {

            Log.d(TAG, "BLE disabled.");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            ret = false;
        }
        return ret;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect

            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected

                if (trackingModeLong) {
                    playAlarm();
                }

            }
        }
    };

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
            Log.d(TAG, gatt.toString() + ", " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

            super.onReadRemoteRssi(gatt, rssi, status);
            if(false){
                Log.d(TAG, "rssi = " + rssi);
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
            else if(keepTracking){
                Log.d(TAG, "rssi = " + rssi);
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
                Log.d(TAG, "trackCnt = " + trackCnt);
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
                Log.d(TAG, "i am tracking ... ");
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

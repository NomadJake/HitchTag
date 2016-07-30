package com.crosscharge.hitch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;

import java.util.UUID;

/**
 * Created by nomad on 12/7/16.
 */
public class fService extends Service {

    BluetoothDevice device;
    Context context;
    public Boolean keepTracking = true;
    Boolean kill = false;
    Boolean trackingModeLong;
    NotificationManager notificationManager;
    Notification stateHolderNotification;
    NotificationCompat.Builder builder;
    TrackThread persistentThread;
    Boolean trackConnected = true;
    Boolean alarmTriggered = false;
    Boolean restartConnectedFlag = false;
    int statusForActivity = 1; //1 : tracking ; 2 : stopped tracking; 3 : Lost; 4 : not found

    public PendingIntent pstartTrackingIntent;
    public PendingIntent pstopTrackingIntent;
    public PendingIntent pkillService;
    public PendingIntent pLostIntent;
    public PendingIntent pMiddleIntent;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt deviceGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    String TAG = "fServiceLOG";
    String mode;
    private boolean keepTrackerThread = true;

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

        keepTrackerThread = true;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notificationSound);
        r.play();

        if( intent==null || intent.getAction().equals("lost")){

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction("main");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.hitchinlogo);

            statusForActivity = 3;

            Intent killService = new Intent(this, fService.class);
            killService.putExtra("device", device);
            killService.setAction("kill");
            PendingIntent pkillService = PendingIntent.getService(this, 0,
                    killService, PendingIntent.FLAG_UPDATE_CURRENT);

            builder = new NotificationCompat.Builder(context);

            builder.setContentTitle("Hitch");
            builder.setContentText("Lost Hitch tag !");
            builder.setSmallIcon(R.drawable.hitchinlogo);
            builder.setLargeIcon(Bitmap.createScaledBitmap(icon, 300, 300, false));
            builder.addAction(android.R.drawable.ic_lock_power_off, "close",
                    pkillService);
            builder.setColor(getResources().getColor(R.color.holo_red_dark));
            builder.setContentIntent(pendingIntent);
            builder.setOngoing(true);

            builder.setPriority(Notification.PRIORITY_HIGH);
            stateHolderNotification = builder.build();


            startForeground(101,
                    stateHolderNotification);

        } else if (intent.getAction().equals("fore")) {

            keepTracking = true;
//            restartConnectedFlag = true;
//            sendMessage(1);
            new TrackingStatusThread().start();
            device = (BluetoothDevice) intent.getExtras().get("device");
            mBluetoothDeviceAddress = device.getAddress();
            if (device == null) {
                Log.d(TAG, "stopping service : device null");
                super.stopSelf();
            }
            trackingModeLong = (Boolean) intent.getExtras().getBoolean("trackingModeLong");
            if (trackingModeLong) {
                mode = "Wide coverage";
            } else {
                mode = "Short coverage";
            }

            if (deviceGatt == null) {
                connect(mBluetoothDeviceAddress);
            }

            if (trackingModeLong) {
                Log.d(TAG, "started tracking thread long mode");

                IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
                IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
                IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                this.registerReceiver(mReceiver, filter1);
                this.registerReceiver(mReceiver, filter2);
                this.registerReceiver(mReceiver, filter3);


            } else {
                persistentThread = new TrackThread();
                persistentThread.start();
                Log.d(TAG, "started tracking thread near mode");
                restartConnectedFlag = true;

            }
            Log.d(TAG, "Received Start Foreground Intent ");
            statusForActivity = 1;

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction("main");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);


            Intent startTrackingIntent = new Intent(this, fService.class);
            startTrackingIntent.putExtra("device", device);
            startTrackingIntent.putExtra("trackingModeLong", trackingModeLong);
            startTrackingIntent.setAction("start");
            pstartTrackingIntent = PendingIntent.getService(this, 0,
                    startTrackingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent middleIntent = new Intent(this, fService.class);
            middleIntent.putExtra("device", device);
            middleIntent.putExtra("trackingModeLong", trackingModeLong);
            middleIntent.setAction("middle");
            pMiddleIntent = PendingIntent.getService(this, 0,
                    middleIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent lostIntent = new Intent(this, fService.class);
            lostIntent.putExtra("device", device);
            lostIntent.putExtra("trackingModeLong", trackingModeLong);
            lostIntent.setAction("lost");
            pLostIntent = PendingIntent.getService(this, 0,
                    lostIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent stopTrackingIntent = new Intent(this, fService.class);
            stopTrackingIntent.putExtra("device", device);
            stopTrackingIntent.putExtra("trackingModeLong", trackingModeLong);
            stopTrackingIntent.setAction("stop");
            pstopTrackingIntent = PendingIntent.getService(this, 0,
                    stopTrackingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent killService = new Intent(this, fService.class);
            killService.putExtra("device", device);
            killService.setAction("kill");
            pkillService = PendingIntent.getService(this, 0,
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
//            builder.addAction(android.R.drawable.ic_media_play, "start", pMiddleIntent);
            builder.addAction(android.R.drawable.ic_media_pause, "Pause",
                    pstopTrackingIntent);
            builder.addAction(android.R.drawable.ic_lock_power_off, "Close",
                    pkillService);

            builder.setPriority(Notification.PRIORITY_HIGH);
            stateHolderNotification = builder.build();


            startForeground(101,
                    stateHolderNotification);
        }else if(intent.getAction().equals("middle")){

            builder.setContentText("Restarting Tracking ... ");
            notificationManager.notify(101, builder.build());
            Intent intentJ = new Intent();
            try {
                pstartTrackingIntent.send(context, 0, intentJ);

            } catch (PendingIntent.CanceledException e) {
                // the stack trace isn't very helpful here
                Log.d(TAG, "restart tracking failed");
            }

        }else if (intent.getAction().equals("start")) {
            Log.d(TAG, "restart service clicked");
            try {
                Boolean restartConnectedFlag2 = connect(mBluetoothDeviceAddress);
            } catch (Exception e) {
                mode = "device not found !";
                statusForActivity = 4;
                e.printStackTrace();
            }

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (deviceGatt == null) {
                        mode = "device not found !";
                        restartConnectedFlag = false;
                        statusForActivity = 4;
                    } else {
                        writeCharasLevel(Constants.UUIDS.LINK_LOSS, Constants.ALERT_HIGH);
                        restartConnectedFlag = true;
                        sendMessage(1);
                        if (trackingModeLong) {
                            mode = "Wide coverage";
                        } else {
                            mode = "Short coverage";
                        }
                    }
                    if (trackConnected == false) {
                        mode = "device not found !";
                        restartConnectedFlag = false;
                        statusForActivity = 4;
                    }


                    keepTracking = true;
                    builder.mActions.clear();
                    builder.addAction(android.R.drawable.ic_media_pause, "Pause",
                            pstopTrackingIntent);
                    builder.addAction(android.R.drawable.ic_lock_power_off, "Close",
                            pkillService);
                    builder.setContentText("Tracking Mode : " + mode);


                    if (!trackingModeLong) {
                        new TrackThread().start();
                    }

                    notificationManager.notify(101, builder.build());
                }
            }, 5000);


        } else if (intent.getAction().equals("stop")) {
            Log.d(TAG, "pause tracking clicked");

            device = (BluetoothDevice) intent.getExtras().get("device");
            trackingModeLong = (Boolean) intent.getExtras().getBoolean("trackingModeLong");

            writeCharasLevel(Constants.UUIDS.LINK_LOSS, Constants.ALERT_LOW);

            stopAlarm();
            if (persistentThread != null) {
                persistentThread.interrupt();
            }
            try {

            } catch (Exception e) {
                e.printStackTrace();
            }
            statusForActivity = 2;
            sendMessage(2);
            keepTracking = false;
            String pausedText = "Tracking Paused";

            try {
                disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }

            builder.mActions.clear();
            builder.addAction(android.R.drawable.ic_media_play, "start", pMiddleIntent);
            builder.addAction(android.R.drawable.ic_lock_power_off, "Close",
                    pkillService);
            builder.setContentText(pausedText);
            notificationManager.notify(101, builder.build());

        } else if (intent.getAction().equals("kill")) {
            Log.d(TAG, " kill service intent by user");
            if (persistentThread != null) {
                persistentThread.interrupt();
            }

            stopAlarm();

            keepTrackerThread = false;
            Intent mIntent = new Intent("service-active");
            mIntent.putExtra("status", "stopped");
            LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
            statusForActivity = 2;
            keepTracking = false;
            disconnect();
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
                try {
                    connect(mBluetoothDeviceAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                trackConnected = true;
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
                trackConnected = false;

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected

                trackConnected = false;
                if (trackingModeLong) {
                    Log.d(TAG, "ACTION_ACL_DISCONNECTED");
                    if (keepTracking) {
                        playAlarm();
                        statusForActivity = 3;
                        Intent intentJ = new Intent();
                        try {
                            pLostIntent.send(context, 0, intentJ);

                        } catch (PendingIntent.CanceledException e) {
                            // the stack trace isn't very helpful here
                            Log.d(TAG, "restart tracking failed");
                        }
                    }
                }

            }
        }
    };


    private final BluetoothGattCallback masterCallBack = new BluetoothGattCallback() {

        int trackCnt = 3;
        int trackStopAlarmCount = 3;

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // connected - discover services now
                gatt.discoverServices();
                trackConnected = true;
                mConnectionState = STATE_CONNECTED;
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected
                deviceGatt = null;
                trackConnected = false;
                if (keepTracking) {
                    if (!trackingModeLong) {
                        if (restartConnectedFlag) {
                            playAlarm();
                            statusForActivity = 3;
                            Intent intent = new Intent();
                            try {
                                pLostIntent.send(context, 0, intent);

                            } catch (PendingIntent.CanceledException e) {
                                // the stack trace isn't very helpful here
                                Log.d(TAG, "restart tracking failed");
                            }
                        }
                    }
                }
            }
            if (status == BluetoothGatt.GATT_FAILURE) {
                // failure
                playAlarm();
                trackConnected = false;
                disconnect();
                Intent intent = new Intent();
                try {
                    pLostIntent.send(context, 0, intent);

                } catch (PendingIntent.CanceledException e) {
                    // the stack trace isn't very helpful here
                    Log.d(TAG, "restart tracking failed");
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, gatt.toString() + ", " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

            super.onReadRemoteRssi(gatt, rssi, status);
            if (false) {
                Log.d(TAG, "rssi = " + rssi);
                int bars = 0;
                if (rssi < -80 || rssi == 0) {
                    bars = 0;
                } else if (rssi < -65) {
                    bars = 1;
                } else if (rssi < -50) {
                    bars = 2;
                } else if (rssi < -35) {
                    bars = 3;
                } else {
                    bars = 4;
                }
            } else if (keepTracking) {
                Log.d(TAG, "rssi = " + rssi);
                if (rssi < -90 || rssi == 0) {
                    trackCnt--;
                    if (trackCnt == 0) {
                        Log.d(TAG, "rssi check");
                        playAlarm();
                        statusForActivity = 3;
                        alarmTriggered = true;
                        try {
                            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                            r.play();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent intent = new Intent();
                        try {
                            pLostIntent.send(context, 0, intent);

                        } catch (PendingIntent.CanceledException e) {
                            // the stack trace isn't very helpful here
                            Log.d(TAG, "restart tracking failed");
                        }
                    } else if (trackCnt < -8) {
                        stopAlarm();
                        disconnect();
                    }
                } else {
                    trackCnt = 3;
//                    if (alarmTriggered) {
//                        trackStopAlarmCount--;
//                        if (trackStopAlarmCount == 0) {
//                            stopAlarm();
//                            trackStopAlarmCount = 3;
//                            Intent intent = new Intent();
//                            try {
//                                pstartTrackingIntent.send(context, 0, intent);
//
//                            } catch (PendingIntent.CanceledException e) {
//                                // the stack trace isn't very helpful here
//                                Log.d(TAG, "restart tracking failed");
//                            }
//
//                        }
//                    }
                }
                Log.d(TAG, "trackCnt = " + trackCnt);
            }
        }
    };

    public void stopAlarm() {


        Intent stopIntent = new Intent(context, AlarmService.class);
        stopIntent.setAction(Constants.NOTIFICATION.STOPFOREGROUND_ACTION);
        context.startService(stopIntent);

    }


    public class TrackThread extends Thread {

        @Override
        public void run() {
            super.run();

            while (deviceGatt != null) {
                deviceGatt.readRemoteRssi();
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    public class TrackingStatusThread extends Thread {

        @Override
        public void run() {
            super.run();

            while (keepTrackerThread) {

                switch (statusForActivity){
                    case 1: sendMessage(1);
                        break;
                    case 2: sendMessage(2);
                        break;
                    case 3: sendMessage(3);
                        break;
                    case 4: sendMessage(4);
                        break;
                    default:sendMessage(2);
                }
//                if(statusForActivity == 3){
//                    sendMessage(3);
//                }else if(keepTracking){
//                    sendMessage(1);
//                }else {
//                    sendMessage(2);
//                }
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void playAlarm() {
        Intent startIntent = new Intent(context, AlarmService.class);
        startIntent.setAction(Constants.NOTIFICATION.STARTFOREGROUND_ACTION);
        context.startService(startIntent);
    }

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && deviceGatt != null) {
            Log.d(TAG, "Trying to use an existing deviceGatt for connection.");
            if (deviceGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        deviceGatt = device.connectGatt(this, false, masterCallBack);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || deviceGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        deviceGatt.disconnect();
        Log.d(TAG, "disconnect called");

    }

    public void close() {
        if (deviceGatt == null) {
            return;
        }
        deviceGatt.close();
        deviceGatt = null;
    }

    private void sendMessage(int i) {
        Log.d("sender", "Broadcasting service started");
        Intent intent = new Intent("service-active");
        if (i == 3){
            intent.putExtra("status", "lost");
        }else if (i == 4){
            intent.putExtra("status", "notfound");
        }else if (i == 1 || keepTracking) {
            intent.putExtra("status", "active");
        } else if(i == 2 || !keepTracking) {
            intent.putExtra("status", "stopped");
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void writeCharasLevel(UUID serviceUUID, int level) {
        if (deviceGatt == null) {
            Log.d("TAG", "no device connected");
            return;
        }
        BluetoothGattService alertService = deviceGatt.getService(serviceUUID);
        if (alertService == null) {
            Log.d("TAG", "service not found!");
            return;
        }
        BluetoothGattCharacteristic alertLevel = alertService.getCharacteristic(Constants.UUIDS.ALERT_LEVEL);
        if (alertLevel == null) {
            Log.d("TAG", "Alert Level charateristic not found!");
            return;
        }
        alertLevel.setValue(level, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        deviceGatt.writeCharacteristic(alertLevel);
        try {
            Thread.currentThread().sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
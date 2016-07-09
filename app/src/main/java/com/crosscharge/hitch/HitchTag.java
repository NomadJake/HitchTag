package com.crosscharge.hitch;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import java.util.UUID;

/**
 * Created by YASH on 05/04/2016.
 *
 */
public class HitchTag {
    Context context;
    Message message;

    private BluetoothGatt deviceGatt;
//    private BluetoothGattCallback trackCallBack;
    private BluetoothDevice device;

    private String name, address;
    private int userConnStatus;
    private int userSetRange;

    public HitchTag(Context context, BluetoothDevice device){
        this.context = context;
        this.device = device;
        this.name = device.getName();
        this.address = device.getAddress();
        this.setUserConnStatus(0);
        this.setUserSetRange(0);

    }

    public HitchTag(Context context, String name, String address){
        this.context = context;
        this.name = name;
        this.address = address;
        this.device = null;
        this.setUserConnStatus(0);
        this.setUserSetRange(0);

    }

    public BluetoothDevice getDevice() {
        return this.device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getAddress() {
        return this.address;
    }

    public int getUserConnStatus(){
        return this.userConnStatus;
    }

    public  void setUserConnStatus(int u){
        this.userConnStatus = u;
    }

    public int getUserSetRange(){
        return this.userSetRange;
    }

    public  void setUserSetRange(int u){
        this.userSetRange = u;
    }




    public String getName(){
        if(deviceAvailable()){
            return this.device.getName();
        }
        return this.name;
    }

    public boolean deviceAvailable(){
        return this.device != null;
    }

    public boolean connected(){
        return this.deviceGatt != null;
    }

    public void connect(){
        if(connected()){
            disconnect();
        }
        else{
            deviceGatt = device.connectGatt(context, false, masterCallBack);
        }
    }

    public void disconnect(){
        if(deviceGatt != null){
            deviceGatt.disconnect();
        }
    }

    public void playAlarm(){
        sendStringMessage(Constants.STRINGMESSAGE.ALARM, "a");

        Intent startIntent = new Intent(context, AlarmService.class);
        startIntent.setAction(Constants.NOTIFICATION.STARTFOREGROUND_ACTION);
        context.startService(startIntent);
    }
    public void stopAlarm(){
        sendStringMessage(Constants.STRINGMESSAGE.ALARM, "b");

        Intent stopIntent = new Intent(context, AlarmService.class);
        stopIntent.setAction(Constants.NOTIFICATION.STOPFOREGROUND_ACTION);
        context.startService(stopIntent);
    }


    public void notifyg(UUID serviceUUID)
    {
        BluetoothGattService button2=deviceGatt.getService(serviceUUID);
        BluetoothGattCharacteristic ch1 = button2.getCharacteristic(Constants.UUIDS.BUTTON_2_CUSTOM_CHAR);

        deviceGatt.setCharacteristicNotification(ch1, true);

        BluetoothGattDescriptor descriptor = ch1.getDescriptor(Constants.UUIDS.CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR);
        descriptor.setValue(new byte[] { 0x00, 0x00 });
        deviceGatt.writeDescriptor(descriptor);
    }

    public void writeAlertLevel(UUID serviceUUID, int level) {
        if(deviceGatt == null){
            Log.d("TAG", "no device connected");
            return;
        }
        BluetoothGattService alertService = deviceGatt.getService(serviceUUID);
        if(alertService == null) {
            Log.d("TAG", "service not found!");
            return;
        }
        BluetoothGattCharacteristic alertLevel = alertService.getCharacteristic(Constants.UUIDS.ALERT_LEVEL);
        if(alertLevel == null) {
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


    // send message
    void sendStringMessage(int what, String s){
        message = Message.obtain(MainActivity.handler, what, s);
        message.sendToTarget();
    }

    void sendIntMessage(int what, int i){
        message = Message.obtain(MainActivity.handler, what);
        message.obj = i;
        message.sendToTarget();
    }

    private int lastOp = 0;

    public void findHitchTag(){
        lastOp = Constants.INTMESSAGE.FIND;
        writeAlertLevel(Constants.UUIDS.IMMEDIATE_ALERT, Constants.ALERT_HIGH);
        // thread
        new FindThread().start();
    }

    public void trackHitchTag(){
        lastOp = Constants.INTMESSAGE.TRACK;
        // thread
        new TrackThread().start();
    }

    public void trainHitchTag(){
        lastOp = Constants.INTMESSAGE.TRAIN;
        writeAlertLevel(Constants.UUIDS.IMMEDIATE_ALERT, Constants.ALERT_MILD);
    }

    private final BluetoothGattCallback masterCallBack = new BluetoothGattCallback() {

        int trackCnt = 4;

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            lastOp = 0;
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
            if(lastOp == Constants.INTMESSAGE.FIND){
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
                sendIntMessage(Constants.INTMESSAGE.FIND, bars);
            }
            else if(lastOp == Constants.INTMESSAGE.TRACK){
                Log.d("TRACK", "rssi = " + rssi);
                sendIntMessage(Constants.INTMESSAGE.TRACK, rssi);
                if(rssi < -75 || rssi == 0){
                    trackCnt --;
                    if(trackCnt == 0){
                        playAlarm();
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
            else if(lastOp == Constants.INTMESSAGE.TRAIN){

            }
        }
    };

    public class FindThread extends Thread{
        @Override
        public void run() {
            super.run();

            while(deviceGatt != null){
                deviceGatt.readRemoteRssi();
                try {
                    Thread.currentThread().sleep(3000);
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

//    Ignore everything below this for now
//
//    private final BluetoothGattCallback trackCallBack = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
//                // connected - discover services now
//                gatt.discoverServices();
//            }
//            if (newState == BluetoothProfile.STATE_DISCONNECTED){
//                // disconnected
//                if(getUserConnStatus()==1)
//                {
//                playAlarm();}
//                deviceGatt = null;
//            }
//            if (status == BluetoothGatt.GATT_FAILURE) {
//                // failure
//                playAlarm();
//                disconnect();
//            }
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            // check if immd alert and link loss services available
//            if (gatt.getService(Constants.UUID_IMMEDIATE_ALERT) == null) {
//                sendStringMessage(Constants.TOAST, "Immediate Alert Service missing. Disconnecting...");
//                Log.d("TAG", "Immediate Alert Service missing. Disconnecting...");
//                disconnect();
//                return;
//            } else if (gatt.getService(Constants.UUID_LINK_LOSS) == null) {
//                sendStringMessage(Constants.TOAST, "Link Loss Service missing. Disconnecting...");
//                Log.d("TAG", "Link Loss Service missing. Disconnecting...");
//                disconnect();
//                return;
//            }
//            else if (gatt.getService(Constants.UUID_BUTTON_2) == null) {
//                sendStringMessage(Constants.TOAST, "Button 2 Service missing. Disconnecting...");
//                Log.d("TAG", "Button 2 Service missing. Disconnecting...");
//                disconnect();
//                return;
//            }
//
//            writeAlertLevel(Constants.UUID_LINK_LOSS, Constants.ALERT_HIGH);
//            deviceGatt.readRemoteRssi();
//        }
//
//
//        @Override
//        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//            if(getUserSetRange()==0){
//
//            Log.d("RSSI", ":" + rssi);
//            sendStringMessage(Constants.RSSI, rssi + "");
//
//            if(rssi < -80){
//                track--;
//            }
//            else{
//                track = 3;
//                stopAlarm();
//            }
//
//            if(track == 0){
//                setUserConnStatus(0);
//                playAlarm();
//
//                writeAlertLevel(Constants.UUID_IMMEDIATE_ALERT, Constants.ALERT_HIGH);
//                try {
//                    Thread.currentThread().sleep(6000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                disconnect();
//            }
//
//            if(deviceGatt != null){
//                try {
//                    Thread.currentThread().sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                gatt.readRemoteRssi();
//            }
//        }}
//    };
//
//    int track = 3;
//
//    private final BluetoothGattCallback findCallBack = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
//                // connected - discover services now
//                gatt.discoverServices();
//            }
//            if (newState == BluetoothProfile.STATE_DISCONNECTED){
//                // disconnected
//                deviceGatt = null;
//            }
//            if (status == BluetoothGatt.GATT_FAILURE) {
//                // failure
//                disconnect();
//            }
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            gatt.readRemoteRssi();
//        }
//
//        @Override
//        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//            int bars = 0;
//            if(rssi < -90){
//                bars = 1;
//            }
//            else if(rssi < -75){
//                bars = 2;
//            }
//            else if(rssi < -60){
//                bars = 3;
//            }
//            else{
//                bars = 4;
//            }
//            sendStringMessage(Constants.FIND, bars + " " + gatt.getDevice().getName());
//
//            if(deviceGatt != null){
//                try {
//                    Thread.currentThread().sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                gatt.readRemoteRssi();
//            }
//        }
//    };
//
//    private final BluetoothGattCallback trainCalBack = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
//                // connected - discover services now
//                gatt.discoverServices();
//
//            }
//            if (newState == BluetoothProfile.STATE_DISCONNECTED){
//                // disconnected
//                deviceGatt = null;
//            }
//            if (status == BluetoothGatt.GATT_FAILURE) {
//                // failure
//                disconnect();
//            }
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
//        }
//    };
//
//    private final BluetoothGattCallback connectCallBack = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
//                // connected - discover services now
//                gatt.discoverServices();
//
//            }
//            if (newState == BluetoothProfile.STATE_DISCONNECTED){
//                // disconnected
//                deviceGatt = null;
//            }
//            if (status == BluetoothGatt.GATT_FAILURE) {
//                // failure
//                disconnect();
//            }
//        }
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic)
//        {
//            Log.d("I LIKE IT",characteristic.getValue().toString());
//
//
//        }
//
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            notifyg(Constants.UUID_BUTTON_2);
//            Log.d("Services", "Discovered");
//        }
//    };


}

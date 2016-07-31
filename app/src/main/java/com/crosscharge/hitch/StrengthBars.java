package com.crosscharge.hitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.daasuu.ahp.AnimateHorizontalProgressBar;

public class StrengthBars extends AppCompatActivity {

    public ProgressBar progressBar;
    BluetoothGatt deviceGatt;
    BluetoothDevice device;
    public String TAG = "findTag";
    int bars;
    private double total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strength_bars);
        device =(BluetoothDevice) getIntent().getExtras().get("device");
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        if(deviceGatt == null){
            connect();
        }

        progressBar.setMax(6);
        progressBar.setProgress(3);
        new TrackThread().start();

    }


    public void connect(){
        deviceGatt = device.connectGatt(getApplicationContext(), false, masterCallBack);
    }

    public void disconnect(){
        if(deviceGatt != null){
            deviceGatt.disconnect();
        }
    }

    private final BluetoothGattCallback masterCallBack = new BluetoothGattCallback() {

        int trackCnt = 4;
        int trackStopAlarmCount = 4;

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
//                playAlarm();

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
            if(true){
                Log.d(TAG, "rssi = " + rssi);
                bars = 0;
                if(rssi < -100|| rssi == 0){
                    bars = 1;
                } else if(rssi < -95 && rssi >= -100 ){
                    bars = 2;
                }else if(rssi < -80 && rssi >= -95 ){
                    bars = 3;
                }
                else if(rssi < -65 && rssi >= -80){
                    bars = 4;
                }
                else if(rssi < -50 && rssi >= -65 ){
                    bars = 5;
                }
                else if( rssi < -35 && rssi >= -50){
                    bars = 6;
                }else {
                    bars = 5;
                }

                Log.d(TAG, "bars = " + bars);

                progressBar.setProgress(bars);

            }

        }
    };

//    public static int movingMode(int a[]) {
//        int maxValue, maxCount;
//
//        for (int i = 0; i < a.length; ++i) {
//            int count = 0;
//            for (int j = 0; j < a.length; ++j) {
//                if (a[j] == a[i]) ++count;
//            }
//            if (count > maxCount) {
//                maxCount = count;
//                maxValue = a[i];
//            }
//        }
//
//        return maxValue;
//    }


    public class TrackThread extends Thread{

        @Override
        public void run() {
            super.run();

            while(deviceGatt != null){
                deviceGatt.readRemoteRssi();
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void backPressed(View v)
    {
        onBackPressed();
    }

}

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;

public class StrengthBars extends AppCompatActivity {

    IconRoundCornerProgressBar progress1;
    ProgressBar progressBarSimple;
    BluetoothGatt deviceGatt;
    BluetoothDevice device;
    public String TAG = "findTag";
    int bars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strength_bars);
        device =(BluetoothDevice) getIntent().getExtras().get("device");
        progress1 = (IconRoundCornerProgressBar) findViewById(R.id.progress_2);
        progress1.setProgressColor(Color.parseColor("#56d2c2"));
        progress1.setProgressBackgroundColor(Color.parseColor("#757575"));
        progress1.setIconBackgroundColor(Color.parseColor("#38c0ae"));
        progressBarSimple = (ProgressBar)findViewById(R.id.progressBar2);
        progressBarSimple.setProgress(3);
        if(deviceGatt == null){
            connect();
        }
        progress1.setMax(4);
        progress1.setProgress(3);
        progress1.setIconImageResource(R.drawable.hitchlogo);
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
                progress1.setMax(4);
                progress1.setProgress(bars);
                progressBarSimple.setProgress(bars);

            }

        }
    };




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




}

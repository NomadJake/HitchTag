package com.crosscharge.hitch;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by nomad on 10/7/16.
 */
public class CrowdService extends Service {

    public String URL;
    BluetoothAdapter mBluetoothAdapter;
    BroadcastReceiver mReceiver;
    ArrayList<String> deviceList;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.startDiscovery();
            mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    //Finding devices
                    if (BluetoothDevice.ACTION_FOUND.equals(action))
                    {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Add the name and address to an array adapter to show in a ListView
                        deviceList.add(device.getName() + "\n" + device.getAddress());
                    }
                }
            };

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);

        }

        return super.onStartCommand(intent, flags, startId);

    }

}

package com.crosscharge.hitch;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by YASH on 04/04/2016.
 * ArrayAdapter for ScanForDevicesActivity
 */

public class ScanAdapter extends BaseAdapter {

    Context context;
    ArrayList<BluetoothDevice> scanList;


    public ScanAdapter(Context applicationContext, ArrayList<BluetoothDevice> scanList) {
        super();
        this.context = applicationContext;
        this.scanList = scanList;
    }

    @Override
    public int getCount() {
        return scanList.size();
    }

    @Override
    public Object getItem(int i) {
        if(getCount() > 0){
            return scanList.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    // TextView
    TextView deviceNameTextView, deviceAddressTextView;
    BluetoothDevice device;

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_scan, null);
        }

        device = (BluetoothDevice) getItem(i);

        deviceNameTextView = (TextView) view.findViewById(R.id.deviceNameTextView);
        deviceNameTextView.setText(device.getName());

        deviceAddressTextView = (TextView) view.findViewById(R.id.deviceAddressTextView);
        deviceAddressTextView.setText(device.getAddress());

        return view;
    }
}

package com.crosscharge.hitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ViewStubCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity {

    String TAG = ScanActivity.class.getName();
    //ViewFlipper
    ViewFlipper viewFlipper;
    ViewSwitcher viewSwitcher;
    // Buttons
    ImageView scanButton;
    // ListView
    ListView scanListView;
    ArrayList<BluetoothDevice> scanArrayList;
    ArrayList<String> savedAddressArrayList;
    ScanAdapter scanAdapter;
    // Ble
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt deviceGatt;
    RippleBackground rippleBackground;
    Handler handler;

    // Database
    DbHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        rippleBackground=(RippleBackground)findViewById(R.id.content);
        viewFlipper=(ViewFlipper)findViewById(R.id.viewFlipper);
        viewSwitcher=(ViewSwitcher)findViewById(R.id.viewSwitcher);
        viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this,R.anim.fade_out));
        viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this,R.anim.fade_in));
        viewSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this,R.anim.fade_out));
        viewSwitcher.setInAnimation(AnimationUtils.loadAnimation(this,R.anim.fade_in));
        handler = new Handler();

        helper = new DbHelper(this);

        scanButton = (ImageView) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();

               scanClicked();
            }
        });

        savedAddressArrayList = helper.getHitchTagAddresses();
        scanListView = (ListView) findViewById(R.id.scanListView);
        scanArrayList = new ArrayList<>();
        scanAdapter = new ScanAdapter(this, scanArrayList);
        scanListView.setAdapter(scanAdapter);
        scanListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                connect(scanArrayList.get(i));
                Toast.makeText(ScanActivity.this, "Loading...", Toast.LENGTH_SHORT).show();

            }
        });

        init();
        enableBLE();
    }

    private boolean enableBLE(){
        boolean ret = true;
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {

            Log.d(TAG, "BLE disabled.");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.BLE.REQUEST_ENABLE_BT);
            ret = false;
        }
        return ret;
    }

    private void scanClicked(){
        scanArrayList.clear();
        scanAdapter.notifyDataSetChanged();
        scanLeDevice(true);
        viewFlipper.setDisplayedChild(3);
    }

    // scan callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Log.d(TAG, device.toString());
                    if (!scanArrayList.contains(device)) {
                        if (!savedAddressArrayList.contains(device.getAddress())) {
                            if (rssi > -65 && device.getName()!=null) {
                                if(device.getName().equalsIgnoreCase("Hitch tag"))
                                {scanArrayList.add(device);
                                scanAdapter.notifyDataSetChanged();}
                            }
                            //

                       }
                    }
                    else
                    {
                        //no device detected
                        //
                    }
                }
            });
        }
    };

    private void init(){
        // This method initialises BluetoothManager and BluetoothAdapter

        Log.d(TAG, "Initialise BluetoothAdapter");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                    Log.d("size", scanArrayList.size() + "");
                    rippleBackground.stopRippleAnimation();
                    if(scanArrayList.size()==0){
                        viewFlipper.setDisplayedChild(1);
                    }
                    else
                    {
                        viewSwitcher.setDisplayedChild(1);
                    }
                }
            }, Constants.BLE.SCAN_PERIOD);
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public void connect(BluetoothDevice device){
        deviceGatt = device.connectGatt(this, false, scanCallBack);
    }

    public void disconnect(){
        deviceGatt.disconnect();
    }

    private final BluetoothGattCallback scanCallBack = new BluetoothGattCallback() {
        @Override
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
                disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // check if immd alert and link loss services available
            if (gatt.getService(Constants.UUIDS.IMMEDIATE_ALERT) == null) {
                Log.d("TAG", "Immediate Alert Service missing. Not Hitch Tag...");
                disconnect();
                return;
            } else if (gatt.getService(Constants.UUIDS.LINK_LOSS) == null) {
                Log.d("TAG", "Link Loss Service missing. Not Hitch Tag...");
                disconnect();
                return;
            }

            // valid hitch tag. add to database
            helper.addNewHitchTag(gatt.getDevice());
            //record the fact that user has added atleast one tag
            SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
            settings.edit().putInt("user_first_time",1).apply();
            Log.d("count",helper.getHitchTagCount()+"");
            disconnect();
            Intent intent=new Intent(getApplicationContext(),TagSettingsActivity.class);
            intent.putExtra("tag",helper.getHitchTagCount()-1+"");
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
            //disconnect();


        }
    };

    @Override
    protected void onDestroy() {
        scanLeDevice(false);

        super.onDestroy();
    }
    public void backPressed(View v)
    {
        onBackPressed();

    }
    public void rescanPressed(View v)
    {   viewFlipper.setDisplayedChild(0);
        viewSwitcher.setDisplayedChild(0);

    }
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }

}

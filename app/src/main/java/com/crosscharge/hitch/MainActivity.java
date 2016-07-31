package com.crosscharge.hitch;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.kyleduo.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;


public class MainActivity extends AppCompatActivity {

    TourGuide mTourGuideHandler;
    public Boolean trackingModeLong = true;
    public ImageView pawImage;
    public Button startTrackingServiceButton;
    private Button stopAll;
    private String TAG = MainActivity.class.getName();
    // tab related
    public TextView tagName, tagStatus;
    CircleImageView tagImage;
    ImageView tagStatusImage;
    private TabLayout tabLayout;
    private SwitchButton tagRange;
    private LinearLayout bottomLinearLayout;
    private int selectedTag=0;
    int pos = 0;
    private final int GALLERY_ACTIVITY_CODE=200;
    private final int RESULT_CROP = 400;
    //Buttons for theme change VAZE
    private FloatingActionButton findButton,trackButton,trainButton,refreshButton,tagSettingsButton;
    private ImageView hitchLogoTop;
    public boolean tabSelectedFlag = false;

    //Animations
    private Animation fab_open,fab_close;

    // bluetooth
    BluetoothAdapter bluetoothAdapter;
    BroadcastReceiver bleStateReceiver;

//    HitchTag tag;
    ArrayList<HitchTag> tagList;

    // others
    static StringMessageHandler handler;
    DbHelper helper;

    //Tag Images path
    public String pet_dp;

    //Shared Preferences
    SharedPreferences settings;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {




        helper = new DbHelper(this);
        setStartTheme(Integer.parseInt(helper.getHitchTagThemeColors().get(0)));
        //setTheme(R.style.AppTheme_L_ThemePink);
        super.onCreate(savedInstanceState);
        stopAll = (Button)findViewById(R.id.buttonStopAll);

        setContentView(R.layout.activity_main);


        //Tag Images create Directory


        bottomLinearLayout=(LinearLayout)findViewById(R.id.bottomLinearLayout);
        pawImage=(ImageView)findViewById(R.id.pawImage);
        pawImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                // here for refresh button onclick to do
                if(tagStatus.getText().toString().equals("N.A."))
                {
                    Toast.makeText(MainActivity.this,"Tag not nearby", Toast.LENGTH_LONG).show();}
                else  if(tagStatus.getText().toString().equals("Nearby"))
                {

                    if (!tagList.isEmpty() &&!tagList.get(tagList.size() - 1 ).connected()){
                        tagList.get(tagList.size() - 1 ).connect();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                animateFabs();
                                tagStatus.setText("Connected");
                                tagStatusImage.setVisibility(View.GONE);
                                findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
                                findViewById(R.id.avConnectedView).setVisibility(View.VISIBLE);

                            }
                        });




                    }
                }
                else if(tagStatus.getText().toString().equals("Connected"))
                {
                    Toast.makeText(MainActivity.this,"Tag already connected", Toast.LENGTH_LONG).show();
                }
                if(tagStatus.getText().equals("Connected"))
                {
                    if(settings.getInt("user_first_time",2)==2)

                    {
                    mTourGuideHandler.cleanUp();
                    ToolTip toolTip = new ToolTip()
                            .setTitle("Track your pet.")
                            .setDescription("Tap on the Track button to start tracking")
                            .setTextColor(Color.parseColor("#bdc3c7"))
                            .setBackgroundColor(Color.parseColor("#e74c3c"))
                            .setShadow(true)
                            .setGravity(Gravity.TOP);
                    mTourGuideHandler.setToolTip(toolTip)
                            .setOverlay(new Overlay())
                            .playOn(trackButton);}

                }



            }
        });

        // tabs
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        tagList = helper.getHitchTagList();


       //tagName = (TextView) findViewById(R.id.tagName);
        tagRange = (SwitchButton) findViewById(R.id.rangeSwitch);
        tagStatus = (TextView) findViewById(R.id.tagStatusText);
        tagStatusImage = (ImageView) findViewById(R.id.tagStatusImage);
        hitchLogoTop=(ImageView)findViewById(R.id.toolbar_logo) ;
        findButton=(FloatingActionButton)findViewById(R.id.findbutton);
        trackButton=(FloatingActionButton)findViewById(R.id.trackbutton);
        trainButton=(FloatingActionButton)findViewById(R.id.trainbutton);


        //Animations
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_closed);

        animateFabs();

        refreshButton=(FloatingActionButton)findViewById(R.id.refresh);
        tagSettingsButton=(FloatingActionButton)findViewById(R.id.tagSettings);


//        tagRange=(SwitchButton)findViewById(R.id.rangeSwitch);

        //change 3 to the number of tags added
        //if only 1 hitch tag then do not add Tab else add
        //tabLayout.addTab(tabLayout.newTab().setText("Daisy"));
       // tabLayout.addTab(tabLayout.newTab().setText("Silvy"));

        for(int i = 0; i < tagList.size(); i++) {
            tabLayout.addTab(tabLayout.newTab().setText(tagList.get(i).getName()));
           // tagName.setText(tagList.get(i).getName());
        }

        tagRange.setChecked(true);
        tagRange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HitchTag.longRange = false;
                trackingModeLong = false;
                if(isChecked){
                    HitchTag.longRange = true;
                    trackingModeLong = true;
                }

            }
        });

       tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //pawImage.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
                //tagImage.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
                // get position of current tab and change textview and imageview content
                pos = tab.getPosition();
                Log.d("pos", pos + "");
                Toast.makeText(getApplicationContext(), "pos " + pos + "", Toast.LENGTH_SHORT).show();

                pos = tab.getPosition();
                selectedTag=pos;
                tabSelectedFlag = true;

                setCurrentTheme(Integer.parseInt(helper.getHitchTagThemeColors().get(selectedTag)));
                if (!tagList.get(pos).connected()) {
                    tagList.get(pos).connect();
                    tagList.get(pos).writeCharasLevel(Constants.UUIDS.LINK_LOSS, Constants.ALERT_HIGH);
                }
                // Log.d("Theme color",tagList.get(pos).getThemeColor()+"");
               /*
*/



            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
               // tagList.get(tab.getPosition()).disconnect();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                try {
                    pos = tab.getPosition();
                   tagList.get(pos).connect();
                } catch (NullPointerException e){
                    Toast.makeText(MainActivity.this, "This device is not available", Toast.LENGTH_SHORT).show();
                    scanLeDevice(true);
                }
           }
        });


        tagImage=(CircleImageView)findViewById(R.id.tag_image);
        SharedPreferences sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        String imageResource = sharedPreferences.getString("pet_image_name",null);
        if(imageResource != null){
            pet_dp = imageResource;
        }
        FileInputStream fis = null;

        if(pet_dp != null){
            File file = new File(pet_dp);
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Bitmap myBitmap = BitmapFactory.decodeStream(fis);
            tagImage.setImageBitmap(myBitmap);

        }
        tagImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectImage();


            }
        });

        handler = new StringMessageHandler(this);

        init();
        enableBLE();

//        Toast.makeText(this, tabLayout.getFocusedChild() + "", Toast.LENGTH_SHORT).show();

        bleStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            enableBLE();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            break;
                        case BluetoothAdapter.STATE_ON:
                            scanLeDevice(true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tagStatus.setText("Searching");
                                    findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
                                    tagStatusImage.setVisibility(View.GONE);
                                }
                            });
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            break;
                    }
                }
            }
        };



        IntentFilter bleStateFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        bleStateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bleStateReceiver, bleStateFilter); // Don't forget to unregister during onDestroy
//        startTrackingServiceButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent serviceIntent = new Intent(getApplication(),fService.class);
//                serviceIntent.putExtra("device",tagList.get(pos).getDevice());
//                serviceIntent.putExtra("deviceaddress",tagList.get(pos).getAddress());
//                serviceIntent.setAction("trackingService");
//                startService(serviceIntent);
//            }
//        });

    settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        if(settings.getInt("user_first_time",2)==2)

        {
            ToolTip toolTip = new ToolTip()
                    .setTitle("Welcome!")
                    .setDescription("Refresh the tag status, keep your Hitch nearby")
                    .setTextColor(Color.parseColor("#bdc3c7"))
                    .setBackgroundColor(Color.parseColor("#e74c3c"))
                    .setShadow(true)
                    .setGravity(Gravity.RIGHT);
             mTourGuideHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
                    .setPointer(new Pointer())
                    .setToolTip(toolTip)
                    .setOverlay(new Overlay())
                    .playOn(refreshButton);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("service-active"));


//        mHandler = new Handler();
//        startRepeatingTask();

    }

//    Runnable mStatusChecker = new Runnable() {
//        @Override
//        public void run() {
//            try {
//            } finally {
//                if(!isMyServiceRunning(fService.class)){
//                    tagStatus.setText("stopped");
//                }
//                mHandler.postDelayed(mStatusChecker, 3000);
//            }
//        }
//    };

//    void startRepeatingTask() {
//        mStatusChecker.run();
//    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("status");
            if(message.equals("active")){
                tagStatus.setText("tracking");
                tagStatusImage.setVisibility(View.VISIBLE);
                tagStatusImage.setImageResource(R.drawable.nearby_statusicon);
            }else if(message.equals("stopped")){
                tagStatus.setText("stopped");
                tagStatusImage.setVisibility(View.VISIBLE);
                tagStatusImage.setImageResource(R.drawable.na_status);
                findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
            }else if (message.equals("lost")){
                tagStatus.setText("lost");
                tagStatusImage.setVisibility(View.VISIBLE);
                tagStatusImage.setImageResource(R.drawable.na_status);
                findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
            }else if (message.equals("notfound")){
                tagStatus.setText("not found");
                tagStatusImage.setVisibility(View.VISIBLE);
                tagStatusImage.setImageResource(R.drawable.na_status);
                findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
            }
            Log.d("receiver", "Got message: " + message);
        }
    };

   @Override
    protected void onStart() {
      /*  if(getIntent().hasExtra("tagStatus"))
        {
            tagStatus.setText(getIntent().getStringExtra("tagStatus"));
        }*/


            setCurrentTheme((Integer.parseInt(helper.getHitchTagThemeColors().get(selectedTag))));

       if(!tagStatus.getText().equals("Connected"))
       {
       runOnUiThread(new Runnable() {
           @Override
           public void run() {
               tagStatus.setText("Searching");
               findViewById(R.id.avConnectedView).setVisibility(View.GONE);
               findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
               tagStatusImage.setVisibility(View.GONE);
           }
       });
       scanLeDevice(true);}
        /*switch (tagStatus.getText().toString())
        {
            case "Connected":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateFabs();
                        tagStatusImage.setImageResource(R.drawable.connected_statusicon);
                    }
                });

                break;
            case "Nearby":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       animateFabs();
                        tagStatusImage.setImageResource(R.drawable.nearby_statusicon);
                    }
                });

                break;
            case "N.A.":
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateFabs();
                        tagStatusImage.setImageResource(R.drawable.na_status);
                    }
                });

                break;


        }*/
            //BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
           // tagImage.setImageBitmap(bitmap);
            tabLayout.getTabAt(selectedTag).setText(helper.getHitchTagList().get(selectedTag).getName());
        //Log.d(TAG, );
        super.onStart();
    }

    private void init(){
        // This method initialises BluetoothManager and BluetoothAdapter

        Log.d(TAG, "Initialise BluetoothAdapter");

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
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

//    public boolean writeCharacteristic(){
//
//        BluetoothGatt mBluetoothGatt;
//        mBluetoothGatt = tagList.get(pos).getDeviceGatt();
//
//
//        if (mBluetoothGatt == null) {
//            Log.e(TAG, "lost connection");
//            return false;
//        }
//        BluetoothGattService Service = mBluetoothGatt.getService(UUID_ALARM);
//        if (Service == null) {
//            Log.e(TAG, "service not found!");
//            return false;
//        }
//        BluetoothGattCharacteristic charac = Service
//                .getCharacteristic(UUID_ALARM);
//        if (charac == null) {
//            Log.e(TAG, "char not found!");
//            return false;
//        }
//
//        byte[] value = new byte[1];
//        value[0] = (byte) (21 & 0xFF);
//        charac.setValue(value);
//        boolean status = mBluetoothGatt.writeCharacteristic(charac);
//        return status;
//    }

    private void scanLeDevice(final boolean enable) {

        // true: start scan for 'SCAN_PERIOD' seconds
        // false: stop scan

        if (enable) {


            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    bluetoothAdapter.stopLeScan(scanCallback);

                    if(tagStatus.getText().equals("Searching"))
                    {

                        // if no tag found within specified time then set Status to N.A.
                        tagStatus.setText("N.A.");
                        tagStatusImage.setVisibility(View.VISIBLE);
                        tagStatusImage.setImageResource(R.drawable.na_status);
                        findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);

                    }
                }
            }, Constants.BLE.SCAN_PERIOD);
            bluetoothAdapter.startLeScan(scanCallback);

        }
        else {
            bluetoothAdapter.stopLeScan(scanCallback);
        }
    }

    // scan callback
    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Log.d(TAG, device.toString());

                    if(tagList.size() > 0){
                        for(int i = 0; i < tagList.size(); i++){
                            if(tagList.get(i).getAddress().equals(device.getAddress())){
                                // to enable these devices in the list, as they are available nearby
                                pos = i;
                                tagList.set(i, new HitchTag(getApplicationContext(), device));
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("available"));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        tagStatusImage.setVisibility(View.VISIBLE);
                                        tagStatus.setText("Nearby");
                                        bluetoothAdapter.stopLeScan(scanCallback);
                                        tagStatusImage.setImageResource(R.drawable.nearby_statusicon);

                                        findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);
                                        tagStatusImage.setVisibility(View.VISIBLE);}
                                    //animateFabs();


                                });

                            }
                        }

                       /* if(tagList.get(pos).deviceAvailable() && !tagList.get(pos).connected()){

                            Log.d("Device Nearby","Device Nearby");
                            //check log




                        }*/
                     /*  if(!tagList.get(pos).deviceAvailable()){
                            //vaze even though tag is swithced off it shows that tag is available
                            //this if condition is not satisfied even though tag is off
                            //check log

                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("unavailable"));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("Device Not Available","Device Available");
                                     tagStatus.setText("N.A.");
                                    tagStatusImage.setVisibility(View.VISIBLE);
                                    tagStatusImage.setImageResource(R.drawable.na_status);

                                        findViewById(R.id.avloadingIndicatorView).setVisibility(View.GONE);

                                    //animateFabs();
                                }
                            });

                        }*/
                    }
                }
            });
        }
    };

    //
    private void connectLeDevice(boolean enable){
        // connects to the device which is currently selected
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(connectCallBack);
                }
            }, Constants.BLE.SCAN_PERIOD);
            bluetoothAdapter.startLeScan(connectCallBack);
        }
        else {
            bluetoothAdapter.stopLeScan(connectCallBack);
        }
    }

    // connect callback
    private BluetoothAdapter.LeScanCallback connectCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, device.toString());

                    if(tagList.size() > 0){
                        for(int i = 0; i < tagList.size(); i++){
                            if(tagList.get(i).getAddress().equals(device.getAddress())){
                                // to enable these devices in the list, as they are available nearby
                                pos = i;
                                tagList.set(i, new HitchTag(getApplicationContext(), device));
                            }
                        }
                    }
                    if(!tagList.get(pos).connected())
                    {
                        tagList.get(pos).connect();
                    }


                }
            });
        }
    };


    private void selectImage() {



        final CharSequence[] options = {  "Choose from Gallery","Cancel" };



        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Edit Pet Photo");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int item) {

            if (options[item].equals("Choose from Gallery"))

                {

                    Intent gallery_Intent=new Intent (getApplicationContext(),GalleryUtil.class);
                    startActivityForResult(gallery_Intent,GALLERY_ACTIVITY_CODE);

                }

                else if (options[item].equals("Cancel")) {

                    dialog.dismiss();

                }

            }

        });

        builder.show();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {


            if (requestCode == 2) {

                Intent gallery_Intent = new Intent(getApplicationContext(), GalleryUtil.class);
                startActivityForResult(gallery_Intent, GALLERY_ACTIVITY_CODE);


            } else if(requestCode == Constants.BLE.REQUEST_ENABLE_BT){

            }
            if (requestCode == GALLERY_ACTIVITY_CODE) {
                if(resultCode == Activity.RESULT_OK){
                    String picturePath = data.getStringExtra("picturePath");
                    //perform Crop on the Image Selected from Gallery
                    performCrop(picturePath);
                }
            }

            if (requestCode == RESULT_CROP ) {
                if(resultCode == Activity.RESULT_OK){
                    Bundle extras = data.getExtras();
                    Bitmap selectedBitmap = extras.getParcelable("data");
                    storeImage(selectedBitmap);

                    // Set The Bitmap Data To ImageView
                    tagImage.setImageBitmap(selectedBitmap);

                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.action_add){

        }
        return super.onOptionsItemSelected(item);
    }

    public void onTrackButtonPressed(View v) {
        trackButton.setClickable(false);
        // here for track button onclick to do
        tagList.get(pos).writeCharasLevel(Constants.UUIDS.LINK_LOSS, Constants.ALERT_HIGH);

        Intent serviceIntent = new Intent(MainActivity.this,fService.class);
        serviceIntent.putExtra("device",tagList.get(pos).getDevice());
        serviceIntent.putExtra("deviceaddress",tagList.get(pos).getAddress());
        serviceIntent.putExtra("trackingModeLong",trackingModeLong);
        serviceIntent.setAction("fore");
//        serviceIntent.setAction("trackingService");
        Log.d("starting","starting service");

        startService(serviceIntent);
        if(settings.getInt("user_first_time",2)==2)

        {
        mTourGuideHandler.cleanUp();
        mTourGuideHandler.setToolTip(new ToolTip().setTitle("Ring tag").setDescription("Tap on the Ring Button to ring the tag/ train your pet."))
                .setOverlay(new Overlay())
                .playOn(trainButton);}
        trackButton.setClickable(true);
    }

    public void onFindButtonPressed(View v) {
        findButton.setClickable(false);

        tagList.get(pos).writeCharasLevel(Constants.UUIDS.IMMEDIATE_ALERT, Constants.ALERT_HIGH);
        // here for find button onclick to do
//        HitchTag.trakHitch = true;
//        tagList.get(pos).findHitchTag();
        tagList.get(pos).writeCharasLevel(Constants.UUIDS.LINK_LOSS, Constants.ALERT_HIGH);

        trackButton.setClickable(true);
        Intent findIntent = new Intent(MainActivity.this,StrengthBars.class);
        findIntent.putExtra("device",tagList.get(pos).getDevice());
        startActivity(findIntent);
        if(settings.getInt("user_first_time",2)==2)

        {
        mTourGuideHandler.cleanUp();
        settings.edit().putInt("user_first_time",3).apply();}
        findButton.setClickable(true);
    }

    public void onTrainButtonPressed(View v) {
        trainButton.setClickable(false);
        // here for train button onclick to do
        tagList.get(pos).trainHitchTag();
        if(settings.getInt("user_first_time",2)==2)

        {
            mTourGuideHandler.cleanUp();
            mTourGuideHandler.setToolTip(new ToolTip().setTitle("Find tag").setDescription("Find your tag"))
                    .setOverlay(new Overlay())
                    .playOn(findButton);
        }
        trainButton.setClickable(true);
    }

    public void onRefreshPressed(View v) {
        if(tagStatus.getText().equals("Nearby"))
        {
            if(settings.getInt("user_first_time",2)==2)

            {
                mTourGuideHandler.cleanUp();
                mTourGuideHandler.setToolTip(new ToolTip().setTitle("Connect to Tag").setDescription("Tap on the paw to connect to Hitch"))
                        .setOverlay(new Overlay())
                        .playOn(pawImage);
            }
        }
        // here for refresh button onclick to do


        if(tagStatus.getText().equals("Connected")||tagStatus.getText().equals("Searching"))
        {
            
        }
        else{
            scanLeDevice(true);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tagStatus.setText("Searching");
                    findViewById(R.id.avConnectedView).setVisibility(View.GONE);
                    findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
                    tagStatusImage.setVisibility(View.GONE);
                }
            });
        }

    }

    public void onConnectPressed(View v) {


    }


    public void onTagSettingsPressed(View v) {
        Intent i=new Intent(this, TagSettingsActivity.class);

        //put the selected tag name to refer to change settings in other activity
        i.putExtra("tagStatus",tagStatus.getText().toString());
        i.putExtra("tag",selectedTag+"");
        startActivity(i);

        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
        // here for tag settings button onclick to do
        //tagList.get(pos).disconnect();
    }
    public void onInfoPressed(View v) {
        Intent i=new Intent(this,InfoActivity.class);
        i.putExtra("tag",selectedTag+"");
        startActivity(i);
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);

    }
/*

    public void playAlarm(){
        Intent startIntent = new Intent(MainActivity.this, AlarmService.class);
        startIntent.setAction(Constants.NOTIFICATION.STARTFOREGROUND_ACTION);
        startService(startIntent);
    }

    public void stopAlarm(){
        Intent stopIntent = new Intent(MainActivity.this, AlarmService.class);
        stopIntent.setAction(Constants.NOTIFICATION.STOPFOREGROUND_ACTION);
        startService(stopIntent);
    }
*/



    public static class StringMessageHandler extends Handler{

        Context context;

        public StringMessageHandler(Context context){
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            // strings
            if(msg.what == Constants.STRINGMESSAGE.TOAST){
//                Toast.makeText(context, (String) msg.obj, Toast.LENGTH_LONG).show();
            }
            else if(msg.what == Constants.STRINGMESSAGE.ALARM){
                if((msg.obj).equals("a")){
//                    playAlarm();
                }
                else{
//                    stopAlarm();
                }
            }

            // ints

            else if(msg.what == Constants.INTMESSAGE.TRACK){
                // not sure if this one is really needed
//                Toast.makeText(context, "TRACK: rssi = " + msg.obj, Toast.LENGTH_SHORT).show();
            }
            else if(msg.what == Constants.INTMESSAGE.FIND){
//                Toast.makeText(context, "FIND: bars = " + msg.obj, Toast.LENGTH_SHORT).show();
            }else if(msg.what == 69){
                Toast.makeText(context, "Tag Connected", Toast.LENGTH_SHORT).show();



            }
        }

    }



    @Override
    protected void onDestroy() {
        unregisterReceiver(bleStateReceiver);
        super.onDestroy();
    }
    public void setCurrentTheme(int id)
    {

        switch (id)
        {
            case 0:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pawImage.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                        findButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor0)));
                        trackButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor0)));
                        trainButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor0)));
                        refreshButton.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                        tagSettingsButton.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                        hitchLogoTop.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                        tagImage.setBorderColor(getResources().getColor(R.color.themeColor0));
                        tagRange.setTintColor(getResources().getColor(R.color.themeColor0));

                    }
                });

                break;
            case 1:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pawImage.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                        findButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor1)));
                        trackButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor1)));
                        trainButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor1)));
                        refreshButton.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                        tagSettingsButton.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                        hitchLogoTop.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                        tagImage.setBorderColor(getResources().getColor(R.color.themeColor1));
                        tagRange.setTintColor(getResources().getColor(R.color.themeColor1));

                    }
                });

                break;
            case 2:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pawImage.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                        findButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor2)));
                        trackButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor2)));
                        trainButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor2)));
                        refreshButton.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                        tagSettingsButton.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                        hitchLogoTop.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                        tagImage.setBorderColor(getResources().getColor(R.color.themeColor2));
                        tagRange.setTintColor(getResources().getColor(R.color.themeColor2));

                    }
                });

                break;
        }
    }
    public void setStartTheme(int id)
    {

        switch(id)
        {
            case 0:
                setTheme(R.style.AppTheme_L_Theme0);
                break;
            case 1:
                setTheme(R.style.AppTheme_L_Theme1);
                break;
            case 2:
                setTheme(R.style.AppTheme_L_Theme2);
        }
    }
    private void performCrop(String picUri) {
        try {
            //Start Crop Activity

            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            File f = new File(picUri);
            Uri contentUri = Uri.fromFile(f);

            cropIntent.setDataAndType(contentUri, "image/*");
            // set crop properties
            cropIntent.putExtra("crop", "true");
            // indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            // indicate output X and Y
            cropIntent.putExtra("outputX", 280);
            cropIntent.putExtra("outputY", 280);

            // retrieve data on return
            cropIntent.putExtra("return-data", true);
            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, RESULT_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            // display an error message
            String errorMessage = "your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private  File getOutputMediaFile(){
        // might need  Environment.getExternalStorageState()
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");
        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        pet_dp = mediaStorageDir.getPath() + File.separator + mImageName;
        SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit()
                .putString("pet_image_name",pet_dp)
                .apply();
        mediaFile = new File(pet_dp);
        Log.d("fileSaved",pet_dp);
        return mediaFile;
    }

    public void stopAll(View v){
        tagList.get(pos).stopAlarm();
        scanLeDevice(true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tagStatus.setText("Searching");
                findViewById(R.id.avloadingIndicatorView).setVisibility(View.VISIBLE);
                tagStatusImage.setVisibility(View.GONE);
            }
        });
    }


    public void animateFabs(){

        if(!tagList.get(pos).connected()){


            trackButton.startAnimation(fab_close);
            trackButton.setClickable(false);
            trainButton.startAnimation(fab_close);
            trainButton.setClickable(false);
            findButton.startAnimation(fab_close);
            findButton.setClickable(false);

        } else {


            trackButton.startAnimation(fab_open);
            trackButton.setClickable(true);
            trainButton.startAnimation(fab_open);
            trainButton.setClickable(true);
            findButton.startAnimation(fab_open);
            findButton.setClickable(true);

        }
    }
}

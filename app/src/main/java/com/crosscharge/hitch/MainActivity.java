package com.crosscharge.hitch;
import android.app.Activity;
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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.kyleduo.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity {


    private ImageView pawImage;
    private String TAG = MainActivity.class.getName();
    // tab related
    TextView tagName, tagStatus;
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


    // bluetooth
    BluetoothAdapter bluetoothAdapter;
    BroadcastReceiver bleStateReceiver;

//    HitchTag tag;
    ArrayList<HitchTag> tagList;

    // others
    static StringMessageHandler handler;
    DbHelper helper;

    //Tag Images path
    File tagImagePath = new File("/sdcard/Hitch/Images");
    String path = Environment.getRootDirectory()
            + File.separator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        helper = new DbHelper(this);
        setStartTheme(Integer.parseInt(helper.getHitchTagThemeColors().get(0)));
        //setTheme(R.style.AppTheme_L_ThemePink);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        //record the fact that user has added atleast one tag
        settings.edit().putBoolean("user_first_time", false).commit();
        //Tag Images create Directory

        bottomLinearLayout=(LinearLayout)findViewById(R.id.bottomLinearLayout);
        pawImage=(ImageView)findViewById(R.id.pawImage);

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
        refreshButton=(FloatingActionButton)findViewById(R.id.refresh);
        tagSettingsButton=(FloatingActionButton)findViewById(R.id.tagSettings);
        tagRange=(SwitchButton)findViewById(R.id.rangeSwitch);

        //change 3 to the number of tags added
        //if only 1 hitch tag then do not add Tab else add
        //tabLayout.addTab(tabLayout.newTab().setText("Daisy"));
       // tabLayout.addTab(tabLayout.newTab().setText("Silvy"));

        for(int i = 0; i < tagList.size(); i++) {
            tabLayout.addTab(tabLayout.newTab().setText(tagList.get(i).getName()));
           // tagName.setText(tagList.get(i).getName());
        }




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

                setCurrentTheme(Integer.parseInt(helper.getHitchTagThemeColors().get(selectedTag)));
                tagList.get(pos).connect();
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
        tagImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectImage();


            }
        });

        handler = new StringMessageHandler(this);

        init();
        enableBLE();

        Toast.makeText(this, tabLayout.getFocusedChild() + "", Toast.LENGTH_SHORT).show();

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
    }

    @Override
    protected void onStart() {

            setCurrentTheme((Integer.parseInt(helper.getHitchTagThemeColors().get(selectedTag))));

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
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

    private void scanLeDevice(final boolean enable) {
        // true: start scan for 'SCAN_PERIOD' seconds
        // false: stop scan

        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.stopLeScan(scanCallback);
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
                            }
                        }
                        if(tagList.get(pos).deviceAvailable()){
                            tagStatus.setText("Nearby");
                            tagStatusImage.setBackgroundResource(R.drawable.nearby_statusicon);
                        }
                        else{
                            // disable this
                            tagStatus.setText("N.A.");
                            tagStatusImage.setBackgroundResource(R.drawable.stat_unavailable);
                        }
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

                    tagList.get(pos).connect();
                }
            });
        }
    };


    private void selectImage() {



        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };



        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Edit Pet Photo");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo"))

                {

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    //pic = f;

                    startActivityForResult(intent, 1);


                }

                else if (options[item].equals("Choose from Gallery"))

                {

                    Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult(intent, 2);



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

            if (requestCode == 1) {
                //h=0;
                File f = new File(Environment.getExternalStorageDirectory().toString());

                for (File temp : f.listFiles()) {

                    if (temp.getName().equals("temp.jpg")) {

                        f = temp;
                        File photo = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
                        //pic = photo;
                        break;

                    }

                }

                try {

                    Bitmap bitmap;

                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();



                    bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),

                            bitmapOptions);



                    tagImage.setImageBitmap(bitmap);



                    //p = path;

                    f.delete();

                    OutputStream outFile = null;

                    File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");

                    try {

                        outFile = new FileOutputStream(file);

                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
                        //pic=file;
                        outFile.flush();

                        outFile.close();


                    } catch (FileNotFoundException e) {

                        e.printStackTrace();

                    } catch (IOException e) {

                        e.printStackTrace();

                    } catch (Exception e) {

                        e.printStackTrace();

                    }

                } catch (Exception e) {

                    e.printStackTrace();

                }

            } else if (requestCode == 2) {

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
                    FileOutputStream out = null;
                    try {

                        out = new FileOutputStream(path);
                        selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                        // PNG is a lossless format, the compression factor (100) is ignored
                    } catch (Exception e) {
                        Log.d("blah","blah");
                        e.printStackTrace();
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
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
        // here for track button onclick to do
        tagList.get(pos).trackHitchTag();
    }

    public void onFindButtonPressed(View v) {
        // here for find button onclick to do
        tagList.get(pos).findHitchTag();
    }

    public void onTrainButtonPressed(View v) {
        // here for train button onclick to do
        tagList.get(pos).trainHitchTag();
    }

    public void onRefreshPressed(View v) {
        // here for refresh button onclick to do
        scanLeDevice(true);
    }

    public void onTagSettingsPressed(View v) {
        Intent i=new Intent(this, TagSettingsActivity.class);
        //put the selected tag name to refer to change settings in other activity
        i.putExtra("tag",selectedTag+"");
        startActivity(i);

        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
        // here for tag settings button onclick to do
        //tagList.get(pos).disconnect();
    }
    public void onSettingsPressed(View v) {
        /*PopupMenu popup = new PopupMenu(MainActivity.this,v);
        popup.getMenuInflater()
                .inflate(R.menu.main, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_settings) {
                    return true;
                }
                else if(id == R.id.action_add){
                    startActivity(new Intent(MainActivity.this, ScanActivity.class));
                }
                return true;
            }
        });

        popup.show(); */

        Intent i=new Intent(this,SettingsActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);

    }

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



    public static class StringMessageHandler extends Handler{

        Context context;

        public StringMessageHandler(Context context){
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            // strings
            if(msg.what == Constants.STRINGMESSAGE.TOAST){
                Toast.makeText(context, (String) msg.obj, Toast.LENGTH_LONG).show();
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
                Toast.makeText(context, "TRACK: rssi = " + msg.obj, Toast.LENGTH_SHORT).show();
            }
            else if(msg.what == Constants.INTMESSAGE.FIND){
                Toast.makeText(context, "FIND: bars = " + msg.obj, Toast.LENGTH_SHORT).show();
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
                pawImage.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                findButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor0)));
                trackButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor0)));
                trainButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor0)));
                refreshButton.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                tagSettingsButton.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                hitchLogoTop.setColorFilter(getResources().getColor(R.color.themeColor0), PorterDuff.Mode.SRC_IN);
                tagImage.setBorderColor(getResources().getColor(R.color.themeColor0));
                tagRange.setTintColor(getResources().getColor(R.color.themeColor0));
                break;
            case 1:
                pawImage.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                findButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor1)));
                trackButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor1)));
                trainButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor1)));
                refreshButton.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                tagSettingsButton.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                hitchLogoTop.setColorFilter(getResources().getColor(R.color.themeColor1), PorterDuff.Mode.SRC_IN);
                tagImage.setBorderColor(getResources().getColor(R.color.themeColor1));
                tagRange.setTintColor(getResources().getColor(R.color.themeColor1));
                break;
            case 2:
                pawImage.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                findButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor2)));
                trackButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor2)));
                trainButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.themeColor2)));
                refreshButton.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                tagSettingsButton.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                hitchLogoTop.setColorFilter(getResources().getColor(R.color.themeColor2), PorterDuff.Mode.SRC_IN);
                tagImage.setBorderColor(getResources().getColor(R.color.themeColor2));
                tagRange.setTintColor(getResources().getColor(R.color.themeColor2));
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
}

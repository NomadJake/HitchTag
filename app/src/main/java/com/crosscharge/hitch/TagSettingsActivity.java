package com.crosscharge.hitch;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.ribell.colorpickerview.ColorPickerView;
import com.ribell.colorpickerview.interfaces.ColorPickerViewListener;

import java.util.ArrayList;

public class TagSettingsActivity extends AppCompatActivity implements ColorPickerViewListener {

    public int chosenThemeColor=0;
    public String chosenName="HITCH";
    ColorPickerView colorPickerView;
    DbHelper helper;
    HitchTag tag;
    String tagStatusSaved;
    ArrayList<String> tagThemeColorList;
    ArrayList<HitchTag> tagList;


    //EditText
    MaterialEditText TagNameEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        helper = new DbHelper(this);
        tagList=helper.getHitchTagList();

        tagThemeColorList=helper.getHitchTagThemeColors();

        //Log.d("TAG ",Integer.parseInt(helper.getHitchTagThemeColors().get(Integer.parseInt(getIntent().getStringExtra("tag"))))+"");
        //setCurrentTheme(Integer.parseInt(helper.getHitchTagThemeColors().get(Integer.parseInt(getIntent().getStringExtra("tag")))));
       if(getIntent().hasExtra("tag"))
       {   tag=tagList.get(Integer.parseInt(getIntent().getStringExtra("tag")));
           setCurrentTheme((chosenThemeColor=Integer.parseInt(helper.getHitchTagThemeColors().get(Integer.parseInt(getIntent().getStringExtra("tag"))))));
        }
        if(getIntent().hasExtra("tagStatus"))
        {
            tagStatusSaved=getIntent().getStringExtra("tagStatus");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_settings);
        TagNameEditText=(MaterialEditText)findViewById(R.id.tagNameEditText);
        try
        {
        TagNameEditText.setText(tag.getName());}
        catch (Exception e)
        {}
        colorPickerView=(ColorPickerView)findViewById(R.id.colorPickerView);
        colorPickerView.setListener(this);

    }
    @Override
    public void onColorPickerClick(int colorPosition) {

        chosenThemeColor=colorPosition;
        Log.d("Color", "TEST color: "+chosenThemeColor);
    }

    public void deletePressed(View v)
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        new AlertDialog.Builder(TagSettingsActivity.this)
                                .setTitle("")
                                .setMessage(" Are you really sure. This will delete your tag and close the app")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        //Yes button clicked
                                        helper.removeHitchTag(tag);
                                        ((ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE))
                                                .clearApplicationUserData();
                                        TagSettingsActivity.this.finish();
                                    }})
                                .setNegativeButton("NO", null).show();


                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to remove this tag?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }
    public void saveSettingsPressed(View v)
    {

        helper.updateHitchThemeColor(tag,chosenThemeColor);
        helper.updateHitchTagName(tag,TagNameEditText.getText().toString());
        Log.d("tagName",TagNameEditText.getText().toString());
        onBackPressed();
        //tagList = helper.getHitchTagList();
        //Log.d("COLOR",tagList.get(0).getThemeColor()+"");
        //pass chosenTheme to the mainActivity and setTheme onResume VAZE

    }
    public void backPressed(View v)
    {
        onBackPressed();
    }
    @Override
    public void onBackPressed()
    {
        Intent ii=new Intent(this,MainActivity.class);
        ii.putExtra("tagStatus",tagStatusSaved);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        if (settings.getInt("user_first_time", 1)==1) {
            settings.edit().putInt("user_first_time",2).apply();
            startActivity(ii);

        }
        else
        {
            Log.d("not first time","not fit");
            super.onBackPressed();
        }
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }
    public void setCurrentTheme(int id)
    {

        switch(id)
        {
            case 0:
                setTheme(R.style.AppTheme_Theme0);
                break;
            case 1:
                setTheme(R.style.AppTheme_Theme1);
                break;
            case 2:
                setTheme(R.style.AppTheme_Theme2);
        }
    }
}

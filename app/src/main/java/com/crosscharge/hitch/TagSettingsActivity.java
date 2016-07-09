package com.crosscharge.hitch;

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
    public void backPressed(View v)
    {
        onBackPressed();

    }
    public void deletePressed(View v)
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        //remove hitchtag here VAZE

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
        //tagList = helper.getHitchTagList();
        //Log.d("COLOR",tagList.get(0).getThemeColor()+"");
        //pass chosenTheme to the mainActivity and setTheme onResume VAZE
        startActivity(new Intent(this,MainActivity.class));
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        if (settings.getBoolean("my_first_time", false)) {
            new Intent(this,MainActivity.class);
        }
        else
        {
            onBackPressed();
        }
    }
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
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

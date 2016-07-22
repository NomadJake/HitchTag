package com.crosscharge.hitch;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class InfoActivity extends AppCompatActivity {
    private FloatingActionButton addADeviePressed;
    DbHelper helper;
    HitchTag tag;
    ArrayList<String> tagThemeColorList;
    ArrayList<HitchTag> tagList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        helper = new DbHelper(this);
        tagList = helper.getHitchTagList();

        helper = new DbHelper(this);
        tagThemeColorList=helper.getHitchTagThemeColors();
        if(getIntent().hasExtra("tag"))

        {
            Log.d("Theme color changed ", tagThemeColorList.get(0));
            tag=tagList.get(Integer.parseInt(getIntent().getStringExtra("tag")));

            setCurrentTheme(Integer.parseInt(tagThemeColorList.get(Integer.parseInt(getIntent().getStringExtra("tag")))));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);


        //Log.d("TAG ",Integer.parseInt(helper.getHitchTagThemeColors().get(Integer.parseInt(getIntent().getStringExtra("tag"))))+"");
        //setCurrentTheme(Integer.parseInt(helper.getHitchTagThemeColors().get(Integer.parseInt(getIntent().getStringExtra("tag")))));

    }
    public void backPressed(View v)
    {
        onBackPressed();

    }
    public void  webClicked(View v)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.hitchtag.com"));
        startActivity(browserIntent);
    }
    public void  fbClicked(View v)
    {

        try {
            this.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/<id_here>")));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/<id_here>")));
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

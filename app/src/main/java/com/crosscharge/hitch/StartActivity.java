package com.crosscharge.hitch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import github.chenupt.springindicator.SpringIndicator;

public class StartActivity extends AppCompatActivity {
    ViewPager appShowcaseViewPager;
    int[] mResources= {R.drawable.p1, R.drawable.p2, R.drawable.p3,};
    FloatingActionButton addAHitchFab;
    SpringIndicator springIndicator;
    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);

        if (settings.getBoolean("user_first_time", true)) {
            //the app is being launched for first time



        setContentView(R.layout.activity_start);
        appShowcaseViewPager= (ViewPager) findViewById(R.id.viewpager);
        addAHitchFab=(FloatingActionButton)findViewById(R.id.addButton);
        text=(TextView)findViewById(R.id.addText);
        String[] appText=
                { getResources().getString(R.string.appText1),
                        getResources().getString(R.string.appText2), getResources().getString(R.string.appText3)};
        appShowcaseViewPager.setAdapter(new AppShowcaseAdapter(this,mResources,appText));
        springIndicator=(SpringIndicator)findViewById(R.id.indicator);
        springIndicator.setViewPager(appShowcaseViewPager);

        springIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position==2)
                {
                    text.setVisibility(View.VISIBLE);
                    addAHitchFab.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        }
        else
        {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }
    public void addPressed(View v)
    {
        Intent intent=new Intent(this,ScanActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }
}

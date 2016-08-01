package com.crosscharge.hitch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import github.chenupt.springindicator.SpringIndicator;

public class StartActivity extends AppCompatActivity {
    ViewPager appShowcaseViewPager;
    int[] mResources= {R.drawable.p1, R.drawable.p2, R.drawable.p3,};
    FloatingActionButton addAHitchFab;
    SpringIndicator springIndicator;
    ImageView arrowIcon,lArrowIcon;
    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);

        if (settings.getInt("user_first_time",0)==0) {
            //the app is being launched for first time

        setContentView(R.layout.activity_start);
            final Animation hide=AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_closed);
            final Animation show=AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_open);
        appShowcaseViewPager= (ViewPager) findViewById(R.id.viewpager);
            arrowIcon=(ImageView)findViewById(R.id.RarrowIcon);
            lArrowIcon=(ImageView)findViewById(R.id.LarrowIcon);
            addAHitchFab=(FloatingActionButton)findViewById(R.id.addAHitch);
            text=(TextView)findViewById(R.id.addText);
            lArrowIcon.startAnimation(hide);
            addAHitchFab.startAnimation(hide);
            addAHitchFab.setTag("hidden");
       // addAHitchFab=(FloatingActionButton)findViewById(R.id.addButton);
        //text=(TextView)findViewById(R.id.addText);
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
                    arrowIcon.startAnimation(hide);


                    if(addAHitchFab.getTag().equals("hidden"))
                    {
                        addAHitchFab.setTag("shown");
                        addAHitchFab.startAnimation(show);
                        text.setText("Add a Hitch");
                        text.startAnimation(show);
                    }
                   // text.setVisibility(View.VISIBLE);
                   // addAHitchFab.setVisibility(View.VISIBLE);
                }
                else
                {
                    arrowIcon.startAnimation(show);
                }
                if(position!=0)
                {
                    lArrowIcon.startAnimation(show);
                }
                else
                {
                    lArrowIcon.startAnimation(hide);
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

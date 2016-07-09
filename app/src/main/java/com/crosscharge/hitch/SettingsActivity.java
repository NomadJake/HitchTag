package com.crosscharge.hitch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ViewSwitcher;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
    public void backPressed(View v)
    {
        onBackPressed();

    }
    public void  addAdevicePressed(View v)
    {
        startActivity(new Intent(this, ScanActivity.class));
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }

}

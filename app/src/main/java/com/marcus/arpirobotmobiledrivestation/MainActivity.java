package com.marcus.arpirobotmobiledrivestation;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    public static MainActivity instance = null;

    public String robotLogText = "";
    public String dsLogText = "";
    public SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(MainActivity.instance != null){
            finish();
            return;
        }

        MainActivity.instance = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.mnuAbout:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.mnuPreferences:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.mnuLog:
                startActivity(new Intent(this, LogActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

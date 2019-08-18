package com.marcus.arpirobotmobiledrivestation;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    public static final boolean ENABLE_DEBUG_LOGGING = true;

    public static MainActivity instance = null;
    public NetworkManager networkManager = new NetworkManager();

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

    public void logInfo(String message) {

    }

    public void logDebug(String message) {
        if(ENABLE_DEBUG_LOGGING) {

        }
    }

    public void logWarning(String message) {

    }

    public void logError(String message) {

    }

    public void handleRobotLog(String line){

    }

    public void failedToConnectToRobot(){

    }

    public void connectedToRobot(){

    }

    public void disconnectedFromRobot(boolean userInitiated){

    }

    public String getRobotAddress(){
        return prefs.getString("robotaddress", "192.168.10.1");
    }

    public String getBatteryVoltage() {
        return prefs.getString("batvoltage", "7.5");
    }
}

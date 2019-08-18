package com.marcus.arpirobotmobiledrivestation;

import android.app.ProgressDialog;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    public static final boolean ENABLE_DEBUG_LOGGING = true;

    public static MainActivity instance = null;
    public NetworkManager networkManager = new NetworkManager();
    public NetworkTable netTable = new NetworkTable();

    public String robotLogText = "";
    public String dsLogText = "";
    public SharedPreferences prefs;

    private ProgressDialog connectProgressDialog;

    private MenuItem connectMenuItem = null, disconnectMenuItem = null;
    private View mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(MainActivity.instance != null){
            finish();
            return;
        }

        mainView = findViewById(R.id.mainView);

        MainActivity.instance = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        connectMenuItem = menu.findItem(R.id.mnuConnect);
        disconnectMenuItem = menu.findItem(R.id.mnuDisconnect);

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
            case R.id.mnuConnect:
                connectProgressDialog = new ProgressDialog(this);
                connectProgressDialog.setCancelable(false);
                connectProgressDialog.setMessage("Connecting to robot...");
                connectProgressDialog.setIndeterminate(true);
                connectProgressDialog.show();

                networkManager.connect();
                return true;
            case R.id.mnuDisconnect:
                networkManager.disconnect(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void logInfo(String message) {
        message = "[DS INFO]: " + message;
        dsLogText += message;
    }

    public void logDebug(String message) {
        if(ENABLE_DEBUG_LOGGING) {
            message = "[DS DEBUG]: " + message;
            dsLogText += message;
        }
    }

    public void logWarning(String message) {
        message = "[DS WARNING]: " + message;
        dsLogText += message;
    }

    public void logError(String message) {
        message = "[DS ERROR]: " + message;
        dsLogText += message;
    }

    public void handleRobotLog(String line){
        robotLogText += line;
    }

    public void failedToConnectToRobot(){
        connectProgressDialog.hide();
        Snackbar.make(mainView,"Failed to connect to robot.", Snackbar.LENGTH_SHORT).show();
    }

    public void connectedToRobot(){

        connectMenuItem.setVisible(false);
        disconnectMenuItem.setVisible(true);

        connectProgressDialog.hide();
    }

    public void disconnectedFromRobot(boolean userInitiated){

        connectMenuItem.setVisible(true);
        disconnectMenuItem.setVisible(false);

        if(!userInitiated) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Connection to the robot was lost.");
            builder.setTitle("Disconnected");
            builder.create().show();
        }
    }

    public void networkTableEntryChanged(String key, boolean isNew){

    }

    public void setIndicatorPanelEnabled(boolean enabled){

    }

    public String getRobotAddress(){
        return prefs.getString("robotaddress", "192.168.10.1");
    }

    public String getBatteryVoltage() {
        return prefs.getString("batvoltage", "7.5");
    }
}

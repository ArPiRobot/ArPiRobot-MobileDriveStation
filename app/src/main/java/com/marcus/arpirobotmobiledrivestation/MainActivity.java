package com.marcus.arpirobotmobiledrivestation;

import android.app.ProgressDialog;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

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
    private Button enableButton, disableButton;

    private byte leftx, lefty, rightx, righty;

    // Virtual gamepad views
    JoystickView leftjs, rightjs;
    Button btnL2, btnR2, btnA, btnB, btnX, btnY, btnBack, btnStart, btnL1, btnR1;
    Button btnDpadUp, btnDpadDown, btnDpadRight, btnDpadLeft;

    private ControllerDataSendThread controllerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(MainActivity.instance != null){
            finish();
            return;
        }

        mainView = findViewById(R.id.mainView);

        enableButton = findViewById(R.id.btnEnable);
        disableButton = findViewById(R.id.btnDisable);

        enableButton.setOnClickListener(this);
        disableButton.setOnClickListener(this);

        // Virtual gamepad views
        leftjs = findViewById(R.id.jsLeft);
        rightjs = findViewById(R.id.jsRight);
        btnL2 = findViewById(R.id.btnL2);
        btnR2 = findViewById(R.id.btnR2);

        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnX = findViewById(R.id.btnX);
        btnY = findViewById(R.id.btnY);
        btnBack = findViewById(R.id.btnBack);
        btnStart = findViewById(R.id.btnStart);
        btnL1 = findViewById(R.id.btnL1);
        btnR1 = findViewById(R.id.btnR1);

        btnDpadDown = findViewById(R.id.btnDpadDown);
        btnDpadUp = findViewById(R.id.btnDpadUp);
        btnDpadLeft = findViewById(R.id.btnDpadLeft);
        btnDpadRight = findViewById(R.id.btnDpadRight);

        leftjs.setOnMoveListener((int angle, int strength) -> {
            leftx = (byte)(Math.cos(angle * Math.PI / 180.0) * strength);
            lefty = (byte)(-1 * Math.sin(angle * Math.PI / 180.0) * strength);
        }, 20);

        rightjs.setOnMoveListener((int angle, int strength) -> {
            rightx = (byte)(Math.cos(angle * Math.PI / 180.0) * strength);
            righty = (byte)(-1 * Math.sin(angle * Math.PI / 180.0) * strength);
        }, 20);

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

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnEnable:
                networkManager.sendCommand(NetworkManager.COMMAND_ENABLE);
                break;
            case R.id.btnDisable:
                networkManager.sendCommand(NetworkManager.COMMAND_DISABLE);
                break;
        }
    }

    public void logInfo(String message) {
        message = "[DS INFO]: " + message;
        dsLogText += message + "\n";
    }

    public void logDebug(String message) {
        if(ENABLE_DEBUG_LOGGING) {
            message = "[DS DEBUG]: " + message;
            dsLogText += message + "\n";
        }
    }

    public void logWarning(String message) {
        message = "[DS WARNING]: " + message;
        dsLogText += message + "\n";
    }

    public void logError(String message) {
        message = "[DS ERROR]: " + message;
        dsLogText += message + "\n";
    }

    public void handleRobotLog(String line){
        robotLogText += line + "\n";
    }

    public void failedToConnectToRobot(){
        connectProgressDialog.hide();
        Snackbar.make(mainView,"Failed to connect to robot.", Snackbar.LENGTH_SHORT).show();
    }

    public void connectedToRobot(){

        connectMenuItem.setVisible(false);
        disconnectMenuItem.setVisible(true);

        connectProgressDialog.hide();

        new ControllerDataSendThread().start();
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

    private byte getDpadDirection(){
        boolean up = btnDpadUp.isPressed(),
                down = btnDpadDown.isPressed(),
                left = btnDpadLeft.isPressed(),
                right = btnDpadRight.isPressed();

        if(up && left) return 8;
        else if (up && right) return 2;
        else if(down && left) return 6;
        else if(down && right) return 4;
        else if(up) return 1;
        else if(down) return 5;
        else if(left) return 3;
        else if(right) return 7;
        return 0;
    }

    private byte[] makeControllerData(){
        byte[] data = new byte[22];
        data[0] = 0; // controller number

        // Axes need to be -100 to 100 not 0 to 100
        data[1] = leftx;
        data[2] = lefty;
        data[3] = rightx;
        data[4] = righty;
        data[5] = (byte)(btnL2.isPressed() ? 100 : 0);
        data[6] = (byte)(btnR2.isPressed() ? 100 : 0);
        data[7] = 127; // 127 signed = 255 unsigned = separator

        // Buttons
        data[8] = (byte)(btnA.isPressed() ? 0 : 1);
        data[9] = (byte)(btnB.isPressed() ? 0 : 1);
        data[10] = (byte)(btnX.isPressed() ? 0 : 1);
        data[11] = (byte)(btnY.isPressed() ? 0 : 1);
        data[12] = (byte)(btnBack.isPressed() ? 0 : 1);
        data[13] = 0; // Guide button
        data[14] = (byte)(btnStart.isPressed() ? 0 : 1);
        data[15] = 0; // Left stick
        data[16] = 0; // Right stick
        data[17] = (byte)(btnL1.isPressed() ? 0 : 1);
        data[18] = (byte)(btnR1.isPressed() ? 0 : 1);
        data[19] = 127; // separator

        // Dpad
        data[20] = getDpadDirection();
        data[21] = '\n';

        return data;
    }



    class ControllerDataSendThread extends Thread {
        @Override
        public void run() {
            while(networkManager.isConnected()) {
                networkManager.sendControllerData(makeControllerData());

                try{Thread.sleep(20);}catch (InterruptedException e){}
            }
        }
    }
}

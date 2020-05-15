package com.marcus.arpirobotmobiledrivestation;

import android.app.ProgressDialog;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.PreferenceManager;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.nio.ByteBuffer;

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
    private TextView mainBatteryLabel;

    private double leftx, lefty, rightx, righty;

    // Virtual gamepad views
    JoystickView leftjs, rightjs;
    Button btnL2, btnR2, btnA, btnB, btnX, btnY, btnBack, btnStart, btnL1, btnR1;
    Button btnDpadUp, btnDpadDown, btnDpadRight, btnDpadLeft;

    private ControllerDataSendThread controllerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        if(MainActivity.instance != null){
            finish();
            return;
        }

        mainView = findViewById(R.id.mainView);

        enableButton = findViewById(R.id.btnEnable);
        disableButton = findViewById(R.id.btnDisable);

        enableButton.setOnClickListener(this);
        disableButton.setOnClickListener(this);

        mainBatteryLabel = findViewById(R.id.lblMainBat);

        // Virtual gamepad views
        leftjs = findViewById(R.id.jsLeft);
        rightjs = findViewById(R.id.jsRight);

        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnX = findViewById(R.id.btnX);
        btnY = findViewById(R.id.btnY);
        btnBack = findViewById(R.id.btnBack);
        btnStart = findViewById(R.id.btnStart);
        btnL1 = findViewById(R.id.btnL1);
        btnR1 = findViewById(R.id.btnR1);
        btnL2 = findViewById(R.id.btnL2);
        btnR2 = findViewById(R.id.btnR2);

        btnDpadDown = findViewById(R.id.btnDpadDown);
        btnDpadUp = findViewById(R.id.btnDpadUp);
        btnDpadLeft = findViewById(R.id.btnDpadLeft);
        btnDpadRight = findViewById(R.id.btnDpadRight);

        leftjs.setOnMoveListener((int angle, int strength) -> {
            leftx = Math.cos(angle * Math.PI / 180.0) * strength;
            lefty = -1 * Math.sin(angle * Math.PI / 180.0) * strength;
        }, 20);

        rightjs.setOnMoveListener((int angle, int strength) -> {
            rightx = Math.cos(angle * Math.PI / 180.0) * strength;
            righty = -1 * Math.sin(angle * Math.PI / 180.0) * strength;
        }, 20);

        MainActivity.instance = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBatteryVoltageIndicator();
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
            case R.id.mnuNetTable:
                startActivity(new Intent(this, NetworkTableViewActivity.class));
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


    ProgressDialog syncProgressDialog = null;
    public void setIndicatorPanelEnabled(boolean enabled){
        if(enabled) {
            runOnUiThread(() -> {
                if (syncProgressDialog != null){
                    syncProgressDialog.hide();
                    syncProgressDialog = null;
                }
            });
        }else{
            runOnUiThread(() -> {
                syncProgressDialog = new ProgressDialog(this);
                syncProgressDialog.setTitle("Net Table Sync");
                syncProgressDialog.setMessage("Network table sync in progress...");
                syncProgressDialog.setCancelable(false);
                syncProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                syncProgressDialog.show();
            });
        }
    }

    public void logInfo(String message) {
        Log.i("[DS INFO]", message);
        message = "[DS INFO]: " + message;
        dsLogText += message + "\n";
    }

    public void logDebug(String message) {
        Log.d("[DS DEBUG]", message);
        if(ENABLE_DEBUG_LOGGING) {
            message = "[DS DEBUG]: " + message;
            dsLogText += message + "\n";
        }
    }

    public void logWarning(String message) {
        Log.w("[DS WARNING]", message);
        message = "[DS WARNING]: " + message;
        dsLogText += message + "\n";
    }

    public void logError(String message) {
        Log.e("[DS ERROR]", message);
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

        logInfo("Connected to robot.");

        connectMenuItem.setVisible(false);
        disconnectMenuItem.setVisible(true);

        connectProgressDialog.hide();

        networkManager.sendCommand(NetworkManager.COMMAND_NET_TABLE_SYNC);

        new ControllerDataSendThread().start();
    }

    public void disconnectedFromRobot(boolean userInitiated){

        runOnUiThread(() -> {
            if(syncProgressDialog != null){
                syncProgressDialog.hide();
                syncProgressDialog = null;
            }
        });

        logInfo("Disconnected from robot.");

        connectMenuItem.setVisible(true);
        disconnectMenuItem.setVisible(false);

        if(!userInitiated) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Connection to the robot was lost.");
            builder.setTitle("Disconnected");
            builder.setPositiveButton("OK", null);
            builder.create().show();
        }
    }

    /**
     * Update the color of the main battery voltage indicator without changing the value of label.
     */
    public void refreshBatteryVoltageIndicator() {
        updateBatteryVoltage(mainBatteryLabel.getText().toString().replace("V", ""));
    }

    /**
     * Set a new battery voltage to display. This will also adjust the background color of the label based on the new value.
     * @param value The new battery voltage (as a string received over the network)
     */
    public void updateBatteryVoltage(String value) {
        try {
            mainBatteryLabel.setText(value + "V");
            double voltage = Double.parseDouble(value);

            double mainBatVoltagePreference = getBatteryVoltage();

            if (voltage >= mainBatVoltagePreference) {
                mainBatteryLabel.setBackgroundColor(ContextCompat.getColor(this, R.color.battery_green));
            } else if (voltage >= (mainBatVoltagePreference * 0.85)) {
                mainBatteryLabel.setBackgroundColor(ContextCompat.getColor(this, R.color.battery_yellow));
            } else if (voltage >= (mainBatVoltagePreference * 0.7)) {
                mainBatteryLabel.setBackgroundColor(ContextCompat.getColor(this, R.color.battery_orange));
            } else {
                mainBatteryLabel.setBackgroundColor(ContextCompat.getColor(this, R.color.battery_red));
            }

        } catch (NumberFormatException e) {
            mainBatteryLabel.setBackgroundColor(ContextCompat.getColor(this, R.color.battery_red));
        }
    }

    public void networkTableEntryChanged(String key, boolean isNew){
        if(key.equals("vbat0")){
            String value = netTable.get(key);
            updateBatteryVoltage(value);
        }
    }

    public String getRobotAddress(){
        return prefs.getString("robotaddress", "192.168.10.1");
    }

    public double getBatteryVoltage() {
        return prefs.getFloat("batvoltage", 7.5f);
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

    /**
     * Converts -100 through 100 to a 16-bit integer value to match how the PC drive station works
     * @param percentageVal A percentage (-1.0 to 1.0) value for the axis
     * @return A signed short representing the same value
     */
    private short getAxisValue(double percentageVal){
        if(percentageVal >= 0) return (short)(percentageVal * 32767);
        return (short)(percentageVal * 32768);
    }

    private byte[] makeControllerData(){
        final byte axisCount = 6;
        final byte buttonCount = 11;
        final byte dpadCount = 1;

        // Packet = controller_num, axis_count, button_count, dpad_count, 2 bytes per axis, 1 byte per 8 buttons, 1 byte per two dpads, '\n'
        // Axes are signed shorts send across two bytes. Big endian
        // Buttons are 1 bit per button (8 buttons per byte)
        // Dpads are 4 bits per dpad (2 dpads per byte)
        int packetSize = 5 + 2 * axisCount + (int)Math.ceil(buttonCount / 8.0) + (int)Math.ceil(dpadCount / 2.0);

        ByteBuffer buffer = ByteBuffer.allocate(packetSize);

        // Controller number
        buffer.put((byte)0);

        // Prepend counts
        buffer.put(axisCount);
        buffer.put(buttonCount);
        buffer.put(dpadCount);

        // Encode axes
        buffer.putShort(getAxisValue(leftx / 100.0));
        buffer.putShort(getAxisValue(lefty / 100.0));
        buffer.putShort(getAxisValue(rightx / 100.0));
        buffer.putShort(getAxisValue(righty / 100.0));
        buffer.putShort((short)(btnL2.isPressed() ? 32767 : 0));
        buffer.putShort((short)(btnR2.isPressed() ? 32767 : 0));

        // Encode buttons
        boolean[] buttonData = new boolean[buttonCount];
        buttonData[0] = btnA.isPressed();
        buttonData[1] = btnB.isPressed();
        buttonData[2] = btnX.isPressed();
        buttonData[3] = btnY.isPressed();
        buttonData[4] = btnBack.isPressed();
        buttonData[5] = false; // Guide button
        buttonData[6] = btnStart.isPressed();
        buttonData[7] = false; // Left stick
        buttonData[8] = false; // Right stick
        buttonData[9] = btnL1.isPressed();
        buttonData[10] = btnR1.isPressed();

        for(int i = 0; i < Math.ceil(buttonCount / 8.0); ++i) {
            byte b = 0;
            for(int j = 0; j < 8; j++) {
                b = (byte) (b << 1); // Shift bits left
                // Add data if still buttons
                if(i * 8 + j < buttonCount) {
                    b |= (buttonData[i * 8 + j] ? (byte) 1 : (byte) 0);
                }
            }
            buffer.put(b);
        }

        // Encode dpad data
        for(int i = 0; i < Math.ceil(dpadCount / 2.0); ++i) {
            byte b = 0;
            for(int j = 0; j < 2; j++) {
                b = (byte) (b << 4); // Shift bits left
                // Add data if still dpads
                if(i * 8 + j < dpadCount) {
                    b |= getDpadDirection();
                }
            }
            buffer.put(b);
        }

        // Make it easy to identify this as the end of the packet
        buffer.put((byte)'\n');

        buffer.position(0);
        byte[] rtnData = new byte[buffer.remaining()];
        buffer.get(rtnData);
        return rtnData;
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

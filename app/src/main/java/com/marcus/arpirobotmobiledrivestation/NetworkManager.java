/*
 * Copyright 2020 Marcus Behel
 *
 * This file is part of ArPiRobot-MobileDriveStation.
 * 
 * ArPiRobot-MobileDriveStation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArPiRobot-MobileDriveStation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArPiRobot-MobileDriveStation.  If not, see <https://www.gnu.org/licenses/>. 
 */
 
package com.marcus.arpirobotmobiledrivestation;

import android.util.Log;

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager{
    private ExecutorService asyncExecutor = Executors.newFixedThreadPool(3);

    private Socket commandSocket = null,
                         netTableSocket = null,
                         logSocket = null;
    private DatagramSocket controllerSocket = null;

    private OutputStream commandSocketOut, netTableSocketOut;
    private InputStream netTableSocketIn, logSocketIn, commandSocketIn;

    // Networking settings
    public final static int CONTROLLER_PORT = 8090;
    public final static int COMMAND_PORT = 8091;
    public final static int NET_TABLE_PORT = 8092;
    public final static int LOG_PORT = 8093;
    public final static int CONNECT_TIMEOUT = 5000; // ms

    // Commands that can be sent to the robot
    public final static String COMMAND_ENABLE = "ENABLE\n";
    public final static String COMMAND_DISABLE = "DISABLE\n";
    public final static String COMMAND_NET_TABLE_SYNC = "NT_SYNC\n";

    public final static byte[] NT_SYNC_START_DATA = new byte[] {(byte)255, (byte)255, '\n'};
    public final static byte[] NT_SYNC_STOP_DATA = new byte[] {(byte)255, (byte)255, (byte)255, '\n'};

    private boolean hasRespondedToDisconnect = false;
    private boolean isConnected = false;

    // Net table data
    private List<Byte> netTableReadBuffer = new ArrayList<>();
    private ArrayList<String> syncKeys = new ArrayList<>();
    private ArrayList<String> syncValues = new ArrayList<>();
    private boolean netTableInSync = false;

    private String logBuffer = "";

    public void connect(){
        if(isConnected) return;
        hasRespondedToDisconnect = false;

        // Reset net table data state
        netTableReadBuffer.clear();
        netTableInSync = false;
        syncKeys.clear();
        syncValues.clear();

        logBuffer = "";

        asyncExecutor.execute(() -> {
            try {
                commandSocket = new Socket();
                commandSocket.connect(new InetSocketAddress(MainActivity.instance.getRobotAddress(), COMMAND_PORT), CONNECT_TIMEOUT);

                netTableSocket = new Socket();
                netTableSocket.connect(new InetSocketAddress(MainActivity.instance.getRobotAddress(), NET_TABLE_PORT), CONNECT_TIMEOUT);


                logSocket = new Socket();
                logSocket.connect(new InetSocketAddress(MainActivity.instance.getRobotAddress(), LOG_PORT), CONNECT_TIMEOUT);

                commandSocketOut = commandSocket.getOutputStream();
                netTableSocketOut = netTableSocket.getOutputStream();

                logSocketIn = logSocket.getInputStream();
                netTableSocketIn = netTableSocket.getInputStream();
                commandSocketIn = commandSocket.getInputStream();

                // Connect UDP socket last (only if exception not thrown = all TCp connections successful)
                controllerSocket = new DatagramSocket();
                controllerSocket.connect(new InetSocketAddress(
                        MainActivity.instance.getRobotAddress(),
                        CONTROLLER_PORT
                ));

                isConnected = true;

                MainActivity.instance.runOnUiThread(() -> {
                    MainActivity.instance.connectedToRobot();
                });

            }catch (IOException e){
                doDisconnect();
                e.printStackTrace();
                MainActivity.instance.runOnUiThread(() -> {
                    MainActivity.instance.failedToConnectToRobot();
                });
            }

            // Already async. Run periodic reads and connectivity checks
            periodicReadAndConnectivityCheck();
        });
    }

    public void disconnect(boolean userInitiated){
        if(!isConnected || hasRespondedToDisconnect) return;
        hasRespondedToDisconnect = true;

        // Don't block while closing
        asyncExecutor.execute(() -> {
            doDisconnect();
            MainActivity.instance.runOnUiThread(() -> {
                MainActivity.instance.disconnectedFromRobot(userInitiated);
            });
        });
    }

    public boolean isConnected(){
        return this.isConnected;
    }

    public void sendControllerData(byte[] data) {
        if(isConnected) {
            asyncExecutor.execute(() -> {
                try {
                    controllerSocket.send(new DatagramPacket(data, data.length));
                } catch (IOException e) {
                    // UDP channel is for some reason closed (this should never happen)
                    if(!hasRespondedToDisconnect) {
                        MainActivity.instance.logError("Failed to write to controller port.");
                    }
                    disconnect(false);
                }
            });
        }
    }

    public void sendCommand(String command) {
        if(isConnected) {
            asyncExecutor.execute(() -> {
                try{
                    commandSocketOut.write(command.getBytes(StandardCharsets.UTF_8));
                    commandSocketOut.flush();
                }catch(IOException e){
                    // Write failed. Disconnect.
                    if(!hasRespondedToDisconnect)
                        MainActivity.instance.logError("Failed to write to command port.");
                    disconnect(false);
                }
            });
        }
    }

    public void sendNTKey(String key, String value) {
        byte[] data = new byte[key.length() + value.length() + 2];
        System.arraycopy(key.getBytes(StandardCharsets.UTF_8), 0, data, 0, key.length());
        data[key.length()] = (byte)255;
        System.arraycopy(value.getBytes(StandardCharsets.UTF_8), 0, data, key.length() + 1, value.length());
        data[data.length - 1] = '\n';
        sendNTRaw(data);
    }

    public void sendNTRaw(byte[] data) {
        if(isConnected) {
            asyncExecutor.execute(() -> {
                try{
                    netTableSocketOut.write(data);
                    netTableSocketOut.flush();
                }catch(IOException e){
                    // Write failed. Disconnect.
                    if(!hasRespondedToDisconnect)
                        MainActivity.instance.logError("Failed to write to net table port.");
                    disconnect(false);
                }
            });
        }
    }



    private void doDisconnect(){
        if(!isConnected) return;

        isConnected = false;

        MainActivity.instance.netTable.abortSync(false);

        try{
            if(commandSocket != null)
                commandSocket.close();
            if(netTableSocket != null)
                netTableSocket = null;
            if(logSocket != null)
                logSocket.close();
            if(controllerSocket != null)
                controllerSocket.close();

            commandSocket = null;
            commandSocketOut = null;
            netTableSocket = null;
            netTableSocketOut = null;
            netTableSocketIn = null;
            logSocketIn = null;
            controllerSocket = null;
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void handleNetTableData(byte[] newData) {
        for(byte b : newData){
            netTableReadBuffer.add(b);
        }

        int endPos = -1;

        while((endPos = netTableReadBuffer.indexOf((byte)'\n')) != -1) {
            List<Byte> subset = netTableReadBuffer.subList(0, endPos + 1);
            if(matchData(subset, NT_SYNC_START_DATA)) {
                netTableInSync = true;
                MainActivity.instance.netTable.startSync();
            }else if(matchData(subset, NT_SYNC_STOP_DATA)) {
                MainActivity.instance.netTable.finishSync(syncKeys, syncValues);
                netTableInSync = false;
            }else {
                int delimPos = netTableReadBuffer.indexOf((byte)255);

                if(delimPos >= 0 && delimPos < endPos) {
                    // Valid data
                    List<Byte> keySubset = netTableReadBuffer.subList(0, delimPos);
                    List<Byte> valueSubset = netTableReadBuffer.subList(delimPos + 1, endPos);
                    String key = new String(Bytes.toArray(keySubset), StandardCharsets.UTF_8);
                    String value = new String(Bytes.toArray(valueSubset), StandardCharsets.UTF_8);

                    if(netTableInSync) {
                        syncKeys.add(key);
                        syncValues.add(value);
                    }else {
                        MainActivity.instance.netTable.setFromRobot(key, value);
                    }
                }
            }

            netTableReadBuffer = netTableReadBuffer.subList(endPos + 1, netTableReadBuffer.size());

        }
    }

    private boolean matchData(List<Byte> dataSubset, byte[] dataToMatch) {
        if(dataSubset.size() != dataToMatch.length)
            return false;

        for(int i = 0; i < dataToMatch.length; ++i) {
            if(dataSubset.get(i) != dataToMatch[i])
                return false;
        }
        return true;
    }

    private void periodicReadAndConnectivityCheck(){
        while(isConnected){
            try{

                // TODO: Somehow check if each socket is still connected

                // handle data
                if(netTableSocketIn.available() > 0){
                    int len =  netTableSocketIn.available();
                    byte[] msg = new byte[len];
                    netTableSocketIn.read(msg);
                    handleNetTableData(msg);
                }

                if(logSocketIn.available() > 0){
                    int len = logSocketIn.available();
                    byte[] msg = new byte[len];
                    logSocketIn.read(msg);
                    String msgStr = new String(msg, StandardCharsets.UTF_8);

                    logBuffer += msgStr;
                    if(logBuffer.indexOf('\n') != -1) {
                        String[] messages = logBuffer.split("\n");
                        int end = messages.length;
                        if(!logBuffer.endsWith("\n")) {
                            end -= 1;
                            logBuffer = messages[messages.length - 1];
                        }else {
                            logBuffer = "";
                        }
                        for(int i = 0; i < end; ++i) {
                            MainActivity.instance.handleRobotLog(messages[i]);
                        }
                    }
                }

                try{Thread.sleep(3);}catch (InterruptedException e) {}

            }catch(Exception e){

            }
        }
    }
}
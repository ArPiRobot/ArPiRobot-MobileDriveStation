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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkTable {
    private HashMap<String, String> values = new HashMap<>();
    private Lock lock = new ReentrantLock(true);
    private boolean modifiable = true;

    public void startSync() {
        if(!modifiable) return; // Already syncing if not modifiable
        MainActivity.instance.setIndicatorPanelEnabled(false);
        MainActivity.instance.logInfo("Starting network table sync.");
        modifiable = false;
    }

    public void finishSync(ArrayList<String> keys, ArrayList<String> values) {
        MainActivity.instance.logDebug("Got " + keys.size() + " entries from the robot.");
        try {
            // Set or add the values from the robot
            for(int i = 0; i < keys.size(); ++i) {
                String key = keys.get(i);
                boolean isNew = !this.values.containsKey(key);
                this.values.put(keys.get(i), values.get(i));
                MainActivity.instance.runOnUiThread(() -> {
                    MainActivity.instance.networkTableEntryChanged(key, isNew);
                });
            }

            MainActivity.instance.logDebug("Done syncing keys from robot to DS. Syncing from DS to robot.");

            // Send keys the robot doesn't have
            for(String key : this.values.keySet()) {
                if(!keys.contains(key)) {
                    MainActivity.instance.networkManager.sendNTKey(key, this.values.get(key));
                }
            }

            MainActivity.instance.logDebug("Done syncing from DS to robot.");

            // End the sync operation
            MainActivity.instance.networkManager.sendNTRaw(NetworkManager.NT_SYNC_STOP_DATA);
            MainActivity.instance.setIndicatorPanelEnabled(true);

            MainActivity.instance.logInfo("Network table sync complete.");

        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            modifiable = true;
        }
    }

    public void abortSync(boolean stillConnected) {
        if(modifiable) return; // Not syncing if modifiable
        if(stillConnected) {
            // Unlock the robot's net table
            MainActivity.instance.networkManager.sendNTRaw(NetworkManager.NT_SYNC_STOP_DATA);
        }
        MainActivity.instance.logWarning("Network table sync aborted.");
        MainActivity.instance.setIndicatorPanelEnabled(true);
        modifiable = true;
    }

    public void setFromRobot(String key, String value) {
        if(!modifiable) return;
        try {
            lock.lock();
            boolean isNew = !this.values.containsKey(key);
            values.put(key, value);
            MainActivity.instance.runOnUiThread(() -> {
                MainActivity.instance.networkTableEntryChanged(key, isNew);
            });

        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    ArrayList<String> getKeys(){
        return new ArrayList<>(values.keySet());
    }

    public void setFromUI(String key, String value) {
        if(!modifiable) return;
        try {
            lock.lock();

            if(MainActivity.instance.networkManager.isConnected() && !value.equals(values.get(key))) {
                MainActivity.instance.networkManager.sendNTKey(key, value);
            }

            values.put(key, value);

        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    public String get(String key) {
        try{
            lock.lock();

            if(values.containsKey(key)) {
                return values.get(key);
            }

        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return "";
    }

    public boolean has(String key) {
        boolean result = false;
        try {
            lock.lock();
            result = values.containsKey(key);
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return result;
    }

    @SuppressWarnings("unused")
    private void printNetworkTable() {
        try {
            lock.lock();

            for(String key : values.keySet()) {
                String value = values.get(key);
                System.out.println(key + "=" + value);
            }

        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}
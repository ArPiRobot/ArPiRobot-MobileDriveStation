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

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class MySettingsFragment extends PreferenceFragmentCompat {

    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final EditTextPreference addressPreference = (EditTextPreference) findPreference("robotaddress");
        addressPreference.setSummary(prefs.getString("robotaddress", "192.168.10.1"));
        addressPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                String yourString = o.toString();
                prefs.edit().putString("robotaddress", yourString).apply();
                addressPreference.setSummary(yourString);

                return true;
            }
        });

        final EditTextPreference batPreference = (EditTextPreference) findPreference("batvoltage");
        batPreference.setSummary(prefs.getFloat("batvoltage", 7.5f) + "");
        batPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                String yourString = o.toString();
                try {
                    prefs.edit().putFloat("batvoltage", Float.parseFloat(yourString)).apply();
                }catch(NumberFormatException e){
                    prefs.edit().putFloat("batvoltage", 7.5f).apply();
                }
                batPreference.setSummary(prefs.getFloat("batvoltage", 7.5f) + "");

                return true;
            }
        });
    }

}
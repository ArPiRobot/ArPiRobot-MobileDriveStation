package com.marcus.arpirobotmobiledrivestation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.Map;

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
        batPreference.setSummary(prefs.getString("batvoltage", "7.5"));
        batPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {

                String yourString = o.toString();
                prefs.edit().putString("batvoltage", yourString).apply();
                batPreference.setSummary(yourString);

                return true;
            }
        });
    }

}
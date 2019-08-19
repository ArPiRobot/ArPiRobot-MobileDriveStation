package com.marcus.arpirobotmobiledrivestation;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


public class DriveStationLogFragment extends Fragment {

    EditText logView;

    public DriveStationLogFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_drive_station_log, container, false);
        logView = root.findViewById(R.id.dsLogView);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        logView.setText(MainActivity.instance.dsLogText);

        if(logView.getText().length() > 0)
            logView.setSelection(logView.getText().length() - 1);
    }
}

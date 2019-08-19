package com.marcus.arpirobotmobiledrivestation;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class RobotLogFragment extends Fragment {

    EditText logView;

    public RobotLogFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_robot_log, container, false);
        logView = root.findViewById(R.id.robotLogView);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        logView.setText(MainActivity.instance.robotLogText);

        if(logView.getText().length() > 0)
            logView.setSelection(logView.getText().length() - 1);
    }
}

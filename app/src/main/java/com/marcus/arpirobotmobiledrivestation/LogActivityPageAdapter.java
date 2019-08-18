package com.marcus.arpirobotmobiledrivestation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class LogActivityPageAdapter extends FragmentPagerAdapter {

    private Context context;

    public LogActivityPageAdapter(Context context, FragmentManager fm){
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch(position){
            case 0:
                return new RobotLogFragment();
            default:
                return new DriveStationLogFragment();
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case 0:
                return context.getString(R.string.robot_log);
            case 1:
                return context.getString(R.string.ds_log);
        }
        return "";
    }

    @Override
    public int getCount() {
        return 2;
    }
}

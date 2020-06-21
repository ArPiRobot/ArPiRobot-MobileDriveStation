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

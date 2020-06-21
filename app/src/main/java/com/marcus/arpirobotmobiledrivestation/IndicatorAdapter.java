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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class IndicatorAdapter extends RecyclerView.Adapter<IndicatorAdapter.InidicatorViewHolder> {

    private ArrayList<String> items;

    public static class InidicatorViewHolder extends RecyclerView.ViewHolder{
        public TextView keyView, valueView;
        public InidicatorViewHolder(View v){
            super(v);
            keyView = v.findViewById(R.id.keyView);
            valueView = v.findViewById(R.id.valueView);
        }
    }

    public void setItems(ArrayList<String> items){
        this.items = items;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InidicatorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.indicator_view, parent, false);
        return new InidicatorViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull InidicatorViewHolder holder, int position) {
        String key = items.get(position);
        String value = MainActivity.instance.netTable.get(key);
        holder.keyView.setText(key);
        holder.valueView.setText(value);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

}

package com.marcus.arpirobotmobiledrivestation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import static androidx.recyclerview.widget.DividerItemDecoration.HORIZONTAL;
import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;

public class NetworkTableViewActivity extends AppCompatActivity {

    // Locked when either key array list is in use
    static final Object keyLock = new Object();
    static NetworkTableViewActivity instance = null;

    // For keys that exist but are not in the UI
    static ArrayList<String> existingKeys = new ArrayList<>();
    // For keys that exist and are already in the UI
    static ArrayList<String> keysInUi = new ArrayList<>();

    private RecyclerView recyclerView;
    private IndicatorAdapter rvAdapter;
    private RecyclerView.LayoutManager rvLayoutManager;

    static void updateKey(String key){
        if(!existingKeys.contains(key) && !keysInUi.contains(key)){
                existingKeys.add(key);
        }
        if(instance != null){
            instance.updateKeyInUi(key);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_table_view);

        NetworkTableViewActivity.instance = this;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recyclerView = findViewById(R.id.netTableRecuclerView);
        recyclerView.setHasFixedSize(true);
        rvLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(rvLayoutManager);
        rvAdapter = new IndicatorAdapter();
        recyclerView.setAdapter(rvAdapter);

        DividerItemDecoration itemDecor = new DividerItemDecoration(recyclerView.getContext(), VERTICAL);
        recyclerView.addItemDecoration(itemDecor);

        rvAdapter.setItems(keysInUi);

        setTitle(R.string.nettable_title);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkTableViewActivity.instance = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_nettable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            case R.id.mnuNtAddExisting:
                synchronized (keyLock) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    String[] items = NetworkTableViewActivity.existingKeys.toArray(new String[0]);
                    builder.setItems(items, (dialog, which) -> {
                        addKeyToUi(items[which]);
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.setCancelable(true);
                    builder.create().show();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addKeyToUi(String key){
        synchronized (keyLock) {
            keysInUi.add(key);
            // Don't show in that list anymore
            existingKeys.remove(key);
        }

        // This list has now changed
        rvAdapter.notifyDataSetChanged();
    }

    private void updateKeyInUi(String key){
        synchronized (keyLock){
            if(keysInUi.contains(key)){
                rvAdapter.notifyItemChanged(keysInUi.indexOf(key));
            }
        }
    }
}

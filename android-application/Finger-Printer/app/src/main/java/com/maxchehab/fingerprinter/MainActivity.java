package com.maxchehab.fingerprinter;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.google.gson.Gson;

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Intent serviceIntent;
    private ServerService serverService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        serverService = new ServerService(this);
        serviceIntent = new Intent(this, serverService.getClass());

        if(!isServiceRunning(serverService.getClass())){
            startService(serviceIntent);
        }



        final SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(false);
        searchView.clearFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        Application app = new Application("app-id","My application");
        app.addUser(new User("maxchehab","asfasdf","today"));
        Application[] apps = {app};

        Gson gson = new Gson();

        String json = gson.toJson(apps);

        Log.i("app-json",json);



    }

    private boolean isServiceRunning(Class<?> serviceClass){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(serviceClass.getName().equals(service.service.getClassName())){
                Log.i("isServiceRunning:",true + "");
                return true;
            }
        }

        Log.i("isServiceRunning:", false + "");
        return false;
    }
}

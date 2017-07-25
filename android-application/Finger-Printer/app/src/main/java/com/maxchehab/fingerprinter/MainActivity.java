package com.maxchehab.fingerprinter;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Intent serviceIntent;
    private ServerService serverService;
    private static Gson gson = new Gson();

    private LinearLayout mainLayout;
    private LinearLayout applicationsLayout;
    private LinearLayout emptyLayout;
    private LinearLayout searchLayout;
    private SearchView searchView;

    private static List<Application> applications = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverService = new ServerService(this);
        serviceIntent = new Intent(this, serverService.getClass());

        if(!isServiceRunning(serverService.getClass())){
            startService(serviceIntent);
        }

        mainLayout = (LinearLayout)findViewById(R.id.mainLayout);
        searchLayout = (LinearLayout)findViewById(R.id.searchLayout);
        applicationsLayout = (LinearLayout)findViewById(R.id.applicationsLayout);
        emptyLayout = (LinearLayout)findViewById(R.id.emptyLayout);
        searchView = (SearchView) findViewById(R.id.searchView);


        updateDatabase();

        searchView.setIconifiedByDefault(false);
        searchView.clearFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateDatabase();
                return false;
            }
        });

    }

    private void updateDatabase(){
        String query = searchView.getQuery().toString();

        SharedPreferences sharedPreferences = getSharedPreferences("database",MODE_PRIVATE);

        //TODO reverse to getString("applications","[]");
        applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[{'applicationID':'app-id','label':'Instagram','users':[{'uniqueKey':'asfasdf','username':'maxchehab'}]}, {'applicationID':'app-id','label':'Google Inbox','users':[{'uniqueKey':'asfasdf','username':'maxchehab@gmail.com'}, {'uniqueKey':'asfasdf','username':'maxchehab1@gmail.com'}]}]"), Application[].class)));

        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        centerParams.gravity = Gravity.CENTER;

        FrameLayout.LayoutParams normalParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        normalParams.gravity = Gravity.TOP;



        LinearLayout feedLayout = (LinearLayout) findViewById(R.id.applicationsLayout);
        feedLayout.removeAllViews();

        for (Application application : applications) {
            if(query.isEmpty() || application.label.toLowerCase().contains(query.toLowerCase()) || queryContainsUsers(application,query)){
                feedLayout.addView(new ApplicationLayout(this,application, query));
            }
        }

        if(applications.size() == 0){
            applicationsLayout.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            searchLayout.setVisibility(View.GONE);
            mainLayout.setLayoutParams(centerParams);
        }else if (feedLayout.getChildCount() > 0){
            applicationsLayout.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            searchLayout.setVisibility(View.GONE);
            mainLayout.setLayoutParams(normalParams);

        }else{
            applicationsLayout.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
            searchLayout.setVisibility(View.VISIBLE);
            mainLayout.setLayoutParams(centerParams);
        }

    }

    private boolean queryContainsUsers(Application application, String query){
        for(User user : application.users){
            if(user.username.toLowerCase().contains(query.toLowerCase())) {
                return true;
            }
        }
        return false;
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

    public void deleteUser(String applicationID, String username){

        Log.i("deleting",applicationID + " : " + username);
        SharedPreferences sharedPreferences = getSharedPreferences("database",MODE_PRIVATE);

        //TODO reverse to getString("applications","[]");
        applications = new LinkedList<>(Arrays.asList(gson.fromJson(sharedPreferences.getString("applications","[{'applicationID':'app-id','label':'Instagram','users':[{'uniqueKey':'asfasdf','username':'maxchehab'}]}, {'applicationID':'app-id','label':'Google Inbox','users':[{'uniqueKey':'asfasdf','username':'maxchehab@gmail.com'}, {'uniqueKey':'asfasdf','username':'maxchehab1@gmail.com'}]}]"), Application[].class)));

        for (Application application : applications) {
            if(application.applicationID.equals(applicationID)){
                userLoop: for (User user : application.users) {
                    if(user.username.equals(username)){
                        application.users.remove(user);
                        break userLoop;
                    }
                }

                if(application.users.size() == 0){
                    applications.remove(application);
                }
                break;
            }
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("applications", gson.toJson(applications.toArray()));
        editor.commit();

        updateDatabase();
    }
}

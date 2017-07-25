package com.maxchehab.fingerprinter;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maxchehab on 7/25/17.
 */

public class Application {
    public String applicationID;
    public String label;
    public List<User> users = new ArrayList<User>();

    public Application(String applicationID, String label){
        this.applicationID = applicationID;
        this.label = label;
    }

    public void addUser(User user){
        users.add(user);
    }

    public void removeUser(String username){
        for(int i = 0; i < users.size(); i++){
            if(users.get(i).username.equals(username)){
                users.remove(i);
                return;
            }
        }
    }
}


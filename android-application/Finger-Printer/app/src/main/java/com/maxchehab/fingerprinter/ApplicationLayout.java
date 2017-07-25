package com.maxchehab.fingerprinter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ApplicationLayout extends LinearLayout {

    public ApplicationLayout(Context context, Application application, String query){
        super(context);
        inflate(application,query);
    }

    private void inflate(Application application, String query){
        inflate(getContext(), R.layout.application_layout, this);

        TextView applicationLabelText = (TextView) findViewById(R.id.applicationLabel);
        LinearLayout usersLayout = (LinearLayout) findViewById(R.id.usersLayout);

        applicationLabelText.setText(application.label);

        for (User user : application.users) {
            if(query.isEmpty() || user.username.toLowerCase().contains(query.toLowerCase()) || application.label.toLowerCase().contains(query.toLowerCase()) ){
                usersLayout.addView(new UserLayout(getContext(), user, application.applicationID));
            }
        }
    }
}


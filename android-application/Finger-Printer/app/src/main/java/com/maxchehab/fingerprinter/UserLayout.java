package com.maxchehab.fingerprinter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class UserLayout extends RelativeLayout {

    private String applicationID;

    public UserLayout(Context context, User user, String applicationID){
        super(context);

        this.applicationID = applicationID;
        inflate(user);
    }

    private void inflate(final User user){
        inflate(getContext(),R.layout.user_layout,this);
        TextView userText = (TextView) findViewById(R.id.username);
        ImageView deleteButton = (ImageView) findViewById(R.id.deleteButton);

        userText.setText(user.username);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                ((MainActivity)getContext()).deleteUser(applicationID, user.username);
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure you want delete '" + user.username + "'?")
                        .setPositiveButton("delete", dialogClickListener)
                        .setNegativeButton("no", dialogClickListener)
                        .setTitle("Attention")
                        .setIcon(R.mipmap.ic_warning)
                        .show();

            }
        });
    }
}

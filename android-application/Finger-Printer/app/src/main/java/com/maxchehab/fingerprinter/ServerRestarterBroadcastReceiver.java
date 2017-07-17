package com.maxchehab.fingerprinter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by maxchehab on 7/17/17.
 */

public class ServerRestarterBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent){
        Log.i("BroadcastReciever","Service stoped");
        context.startService(new Intent(context, ServerService.class));
    }
}

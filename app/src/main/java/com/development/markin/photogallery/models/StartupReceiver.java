package com.development.markin.photogallery.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by magaz on 21.07.2017.
 */

public class StartupReceiver extends BroadcastReceiver {
    public static final String TAG = "StartupReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: "+intent.getAction());
        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}

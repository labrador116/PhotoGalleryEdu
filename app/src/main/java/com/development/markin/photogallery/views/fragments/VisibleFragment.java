package com.development.markin.photogallery.views.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.development.markin.photogallery.models.PollService;

/**
 * Created by magaz on 23.07.2017.
 */

public abstract class VisibleFragment extends Fragment {
    public static final String TAG = "VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().getApplicationContext().registerReceiver(mOnShowNotification, filter,PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().getApplicationContext().unregisterReceiver(mOnShowNotification);
    }

    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i (TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}

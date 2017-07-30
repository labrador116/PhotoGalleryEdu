package com.development.markin.photogallery.models;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.development.markin.photogallery.R;
import com.development.markin.photogallery.views.activities.PhotoGalleryActivity;

import java.util.List;

/**
 * Created by magaz on 12.07.2017.
 */

public class PollService extends IntentService {
    public static final String TAG = "PollService";
    public static final long  POLL_INTERVAL = 1000*60;
    public static final String ACTION_SHOW_NOTIFICATION = "com.development.markin.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.development.markin.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public PollService() {
        super(TAG);
    }

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!isNetworkAvailableAndConnected()){
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;
        if (query==null){
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.size()==0){
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG,"Got an old result: "+resultId);
        } else {
            Log.i(TAG,"Got an new result: "+resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);
            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();
            showBackgroundNotification(0, notification);
        }

        QueryPreferences.setLastResultId(this,resultId);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetwork() != null;
        boolean isNetworkConnected = isNetworkAvailable &&
                cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context,0,i,0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),POLL_INTERVAL,pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn (Context context){
        Intent i = PollService.newIntent(context);

        PendingIntent pendingIntent = PendingIntent.getService(context,0,i,PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    private void showBackgroundNotification (int requestCode, Notification notification){
        Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
        intent.putExtra(REQUEST_CODE, requestCode);
        intent.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(intent, PERM_PRIVATE,null, null, Activity.RESULT_OK, null,null);
    }
}

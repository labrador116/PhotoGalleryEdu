package com.development.markin.photogallery.models;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.development.markin.photogallery.R;
import com.development.markin.photogallery.views.activities.PhotoGalleryActivity;

import java.util.List;

/**
 * Created by magaz on 16.07.2017.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {
    public static final String TAG = "PollService";
    private PollTask mPollTask;
    private Context mContext;
    private String mQuery;
    private String mLastResultId;

//    public PollJobService(Context context){
//        mContext = context;
//    }

    @Override
    public boolean onStartJob(JobParameters params) {

        mQuery = QueryPreferences.getStoredQuery(this);
        mLastResultId = QueryPreferences.getLastResultId(this);
        mContext=getApplicationContext();
        mPollTask=new PollTask();
        mPollTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mPollTask!=null){
            mPollTask.cancel(true);
        }

        return true;
    }

    private class PollTask extends AsyncTask<JobParameters,Void, String>{

        @Override
        protected String doInBackground(JobParameters... params) {
            JobParameters jobParams = params[0];



            List<GalleryItem> items;
            if (mQuery==null){
                items = new FlickrFetchr().fetchRecentPhotos();
            } else {
                items = new FlickrFetchr().searchPhotos(mQuery);
            }

            if (items.size()==0){
                return null;
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(mLastResultId)) {
                Log.i(TAG,"Got an old result: "+resultId);
            } else {
                Log.i(TAG,"Got an new result: "+resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(mContext);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, i, 0);
                Notification notification = new NotificationCompat.Builder(mContext)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(mContext);
                notificationManagerCompat.notify(0, notification);
            }

            QueryPreferences.setLastResultId(mContext,resultId);

            jobFinished(jobParams,false);
            return null;
        }
    }
}

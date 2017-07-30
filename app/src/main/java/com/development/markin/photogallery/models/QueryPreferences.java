package com.development.markin.photogallery.models;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by magaz on 09.07.2017.
 */

public class QueryPreferences {
    public static final String PREF_SEARCH_QUERY = "searcgQuery";
    public static final String PREF_LAST_RESULT_ID = "lastResultId";
    public static final String PREF_IS_ALARM_ON = "isAlarmOn";

    public static String getStoredQuery (Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY,null);
    }

    public static void setStoredQuery (Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SEARCH_QUERY,query).apply();
    }

    public static String getLastResultId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LAST_RESULT_ID,null);
    }

    public static void setLastResultId(Context context, String id){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, id)
                .apply();
    }

    public static boolean isAlarmOn (Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_IS_ALARM_ON, false);
    }

    public static void setAlarmOn (Context context, boolean isOn){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON, isOn)
                .apply();
    }
}

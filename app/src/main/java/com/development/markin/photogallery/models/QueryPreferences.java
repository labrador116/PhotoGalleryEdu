package com.development.markin.photogallery.models;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by magaz on 09.07.2017.
 */

public class QueryPreferences {
    public static final String PREF_SEARCH_QUERY = "searcgQuery";

    public static String getStoredQuery (Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY,null);
    }

    public static void setStoredQuery (Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SEARCH_QUERY,query).apply();
    }
}

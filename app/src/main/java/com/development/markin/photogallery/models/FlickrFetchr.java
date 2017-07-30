package com.development.markin.photogallery.models;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sbt-markin-aa on 11.05.17.
 */

public class FlickrFetchr {

    public static final String TAG ="FlickrFetch";
    public static final String API_KEY = "5a768c6e13c8c1dc53e796f5ee1337eb";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/").buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes (String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode()!=HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead =0;
            byte [] buffer = new byte[1024];


            while ((bytesRead = in.read(buffer)) > 0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }
        finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlString) throws IOException{
        return new String(getUrlBytes(urlString));
    }

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENT_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos (String query){
            String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    private List<GalleryItem> downloadGalleryItems (String url){
        List<GalleryItem> items = new ArrayList<>();
             try {
                 String jsonString = getUrlString(url);
                 Log.i(TAG, "Receive JSON:" + jsonString);
                 JSONObject object = new JSONObject(jsonString);
                 parseItems(items,object);
             } catch (IOException e) {
                 e.printStackTrace();
                 Log.i(TAG, "Failed to fetch items", e);
             } catch (JSONException e) {
                 e.printStackTrace();
             }

             return items;
         }

         private String buildUrl(String method, String query){
             Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);

             if(method.equals(SEARCH_METHOD)){
                 uriBuilder.appendQueryParameter("text", query);
             }

             return uriBuilder.build().toString();
         }

    private void parseItems(List<GalleryItem> items, JSONObject jsonObject)
                 throws IOException, JSONException{

             JSONObject photosJsonObject = jsonObject.getJSONObject("photos");
             JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

             for (int i=0; i< photoJsonArray.length(); i++){

                 JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
                 GalleryItem item = new GalleryItem();

                 item.setId(photoJsonObject.getString("id"));
                 item.setCaption(photoJsonObject.getString("title"));

                 if(!photoJsonObject.has("url_s")){
                     continue;
                 }
                 item.setUrl(photoJsonObject.getString("url_s"));
                 item.setOwner(photoJsonObject.getString("owner"));
                 items.add(item);
             }
         }
}

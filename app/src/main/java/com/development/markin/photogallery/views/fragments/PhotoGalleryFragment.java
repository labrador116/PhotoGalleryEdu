package com.development.markin.photogallery.views.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.development.markin.photogallery.R;
import com.development.markin.photogallery.models.FlickrFetchr;

import java.io.IOException;

/**
 * Created by sbt-markin-aa on 11.05.17.
 */

public class PhotoGalleryFragment extends Fragment {
    private RecyclerView mPhotoRecyclerView;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_gallery_fragment, container, false);

        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.photo_gallery_fragment_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        return view;
    }

    private class FetchItemTask extends AsyncTask<Void, Void, Void>{
        private static final String TAG ="PhotoGalleryFragment";

        @Override
        protected Void doInBackground(Void... params) {
           new FlickrFetchr().fetchItems();
            return null;
        }
    }
}

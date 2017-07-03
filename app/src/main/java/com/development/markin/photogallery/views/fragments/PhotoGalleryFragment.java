package com.development.markin.photogallery.views.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.development.markin.photogallery.R;
import com.development.markin.photogallery.models.FlickrFetchr;
import com.development.markin.photogallery.models.GalleryItem;
import com.development.markin.photogallery.models.assync.ThumbnailDownloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sbt-markin-aa on 11.05.17.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG ="PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mCountPage =1;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private LruCache<String,Bitmap> mCache;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        ActivityManager am = (ActivityManager) getContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        int maxKb = am.getMemoryClass() * 1024;
        int limitKb = maxKb / 8;

        mCache = new LruCache<>(limitKb);
        new FetchItemTask().execute(mCountPage);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        });
        Log.i(TAG, "Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.photo_gallery_fragment, container, false);

        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.photo_gallery_fragment_recycler_view);
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int widthColumn = 270;
                mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),
                        mPhotoRecyclerView.getWidth() / widthColumn
                ));

                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemScrolled = recyclerView.getLayoutManager().getChildCount();
                int totalItemCount = recyclerView.getLayoutManager().getItemCount();
                int firstVisibleItems = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

                if((visibleItemScrolled+firstVisibleItems)>=totalItemCount){
                    new FetchItemTask().execute(++mCountPage);
                    recyclerView.getAdapter().notifyItemInserted(totalItemCount-1);
                    recyclerView.getAdapter().notifyDataSetChanged();
                }

            }
        });

        setupAdapter();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    private void setupAdapter(){
        if (isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class FetchItemTask extends AsyncTask<Integer, Void, List<GalleryItem>>{


        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
           return new FlickrFetchr().fetchItems(params[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            setupAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        public List<GalleryItem> getGalleryItems() {
            return mGalleryItems;
        }

        public void setGalleryItems(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position){
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.drawable.brian_up_close);
            holder.bindDrawable(placeHolder);

            Bitmap bitmap = mCache.get(galleryItem.getUrl());
            if (bitmap!=null){
                Drawable drawable = new BitmapDrawable(getResources(),bitmap);
                holder.bindDrawable(drawable);
            }
            mThumbnailDownloader.quequeThumbnail(holder,galleryItem.getUrl(), mCache);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
}

package com.development.markin.photogallery.views.fragments;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.development.markin.photogallery.R;
import com.development.markin.photogallery.models.FlickrFetchr;
import com.development.markin.photogallery.models.GalleryItem;
import com.development.markin.photogallery.models.PollJobService;
import com.development.markin.photogallery.models.PollService;
import com.development.markin.photogallery.models.QueryPreferences;
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
    private ProgressBar mProgressBar;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        ActivityManager am = (ActivityManager) getContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        int maxKb = am.getMemoryClass() * 1024;
        int limitKb = maxKb / 8;

        mCache = new LruCache<>(limitKb);
        updateItems();

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
        View view = inflater.inflate(R.layout.photo_gallery_fragment, container, false);

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

//                if((visibleItemScrolled+firstVisibleItems)>=totalItemCount){
//                    //new FetchItemTask(null).execute(++mCountPage);
//                    updateItems();
//                    recyclerView.getAdapter().notifyItemInserted(totalItemCount-1);
//                    recyclerView.getAdapter().notifyDataSetChanged();
//                }

            }
        });

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView  searchView =(SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getContext(),query);
                updateItems();

                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                searchView.onActionViewCollapsed();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getContext());
                searchView.setQuery(query,false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getContext())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getContext(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:

                if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                    final int JOB_ID=1;
                    JobScheduler scheduler = (JobScheduler) getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);

                    boolean hasBeenScheduled = false;
                    for (JobInfo jobInfo : scheduler.getAllPendingJobs()){
                        if (jobInfo.getId()==JOB_ID){
                            hasBeenScheduled=true;
                        }

                        if (!hasBeenScheduled){
                            JobInfo jInfo = new JobInfo.Builder(JOB_ID, new ComponentName(getContext(), PollJobService.class))
                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                                    .setPeriodic(1000*60)
                                    .setPersisted(true)
                                    .build();
                            scheduler.equals(jInfo);
                        }
                    }
                }else {
                    boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getContext());
                    PollService.setServiceAlarm(getContext(), shouldStartAlarm);
                }
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getContext());
        new FetchItemTask(query).execute();
    }

    private void setupAdapter(){
        if (isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class FetchItemTask extends AsyncTask<Integer, Void, List<GalleryItem>>{
        private String mQuery;

        public FetchItemTask (String query){
            mQuery=query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            if(mQuery==null){
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems.clear();
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
            mProgressBar.setVisibility(mProgressBar.INVISIBLE);
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

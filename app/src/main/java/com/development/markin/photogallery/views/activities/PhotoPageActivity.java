package com.development.markin.photogallery.views.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

import com.development.markin.photogallery.R;
import com.development.markin.photogallery.views.fragments.PhotoPageFragment;

/**
 * Created by magaz on 30.07.2017.
 */

public class PhotoPageActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Uri photoPageruri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageruri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        WebView webView = (WebView) findViewById(R.id.fragment_photo_page_web_view);
        if (webView.canGoBack()){
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

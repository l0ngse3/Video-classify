package com.example.doanvideoclassification.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

public class ImageUtils {
    public static void loadCircleImageInto(Context context, String url, ImageView view)
    {
        Glide.with(context).load(url)
                .centerCrop()
                .apply(new RequestOptions().circleCrop())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(view);
    }

    public static void loadCircleImageInto(Context context, Bitmap bitmap, ImageView view)
    {
        Glide.with(context).load(bitmap)
                .centerCrop()
                .apply(new RequestOptions().circleCrop())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(view);
    }
}

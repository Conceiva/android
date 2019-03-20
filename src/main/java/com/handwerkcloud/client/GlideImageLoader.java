package com.handwerkcloud.client;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.esafirm.imagepicker.features.imageloader.ImageLoader;
import com.esafirm.imagepicker.features.imageloader.ImageType;

class GlideImageLoader implements ImageLoader {

    @Override
    public void loadImage(String path, ImageView imageView, ImageType imageType) {
        Glide.with(imageView.getContext()).load(path).into(imageView);
    }
}

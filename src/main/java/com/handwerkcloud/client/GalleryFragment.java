package com.handwerkcloud.client;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.owncloud.android.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class GalleryFragment extends Fragment {

    public static final String LAYOUT_ID = "LAYOUT_ID";

    public GalleryFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        LinearLayout startGallery = view.findViewById(R.id.startGallery);
        startGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.create(getActivity())// Activity or Fragment
                    .imageLoader(new GlideImageLoader())
                    .single()
                    .theme(R.style.ImagePickerTheme)
                    .showCamera(false) // show camera or not (true by default)
                    .start();
            }
        });


        return view;
    }
}

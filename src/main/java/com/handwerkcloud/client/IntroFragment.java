package com.handwerkcloud.client;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.owncloud.android.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class IntroFragment extends Fragment {

    public static final String LAYOUT_ID = "LAYOUT_ID";

    public IntroFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Bundle args = getArguments();
        int layoutId = args.getInt(LAYOUT_ID);
        View view = inflater.inflate(layoutId, container, false);

        ImageButton next = view.findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), IntroActivity.class);
                i.setAction(IntroActivity.NEXT);
                getActivity().startActivity(i);
            }
        });

        TextView skip = view.findViewById(R.id.intro_skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), IntroActivity.class);
                i.setAction(IntroActivity.SKIP);
                getActivity().startActivity(i);
            }
        });
        return view;
    }
}

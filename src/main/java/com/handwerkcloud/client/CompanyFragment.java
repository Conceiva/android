package com.handwerkcloud.client;

import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.owncloud.android.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class CompanyFragment extends Fragment {

    private EditText mCompany;
    private EditText mRole;
    private EditText mPhonenumber;
    private EditText mAddress;

    public CompanyFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_company, container, false);
        ImageButton startTrial = view.findViewById(R.id.startTrial);
        mCompany = view.findViewById(R.id.company);
        mRole = view.findViewById(R.id.role);
        mPhonenumber = view.findViewById(R.id.phonenumber);
        mAddress = view.findViewById(R.id.address);

        startTrial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.START_TRIAL);
                i.putExtra(RegisterActivity.EXTRA_COMPANY, mCompany.getText().toString());
                i.putExtra(RegisterActivity.EXTRA_ROLE, mRole.getText().toString());
                i.putExtra(RegisterActivity.EXTRA_PHONENUMBER, mPhonenumber.getText().toString());
                i.putExtra(RegisterActivity.EXTRA_ADDRESS, mAddress.getText().toString());
                getActivity().startActivity(i);
            }
        });

        return view;
    }
}

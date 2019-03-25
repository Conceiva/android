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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class CompanyFragment extends Fragment implements RegisterActivity.OnUserDataReceivedListener {

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
        ((RegisterActivity)getActivity()).setAboutDataListener(this);
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

    @Override
    public void onDataReceived(JSONObject data) {
        try {
            String displayname = data.getString("displayname");
            String phone = data.getString("phone");
            mPhonenumber.setText(phone);
            String address = data.getString("address");
            mAddress.setText(address);
            String company = data.getString("company");
            mCompany.setText(company);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

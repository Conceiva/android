package com.handwerkcloud.client;

import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.MDC.put;

/**
 * A placeholder fragment containing a simple view.
 */
public class CompanyFragment extends Fragment implements RegisterActivity.OnUserDataReceivedListener {

    private EditText mCompany;
    private Spinner mRole;
    private EditText mPhonenumber;
    private EditText mAddress;
    private final String[] mRoles = {"owner", "employee", "contractor"};
    private static final Map<String, Integer> mRoleMap = new HashMap<String, Integer>() {{
        put("owner", 0);
        put("employee", 1);
        put("contractor", 2);
    }};
    private Spinner mBusinesssize;
    private static final int[] mBusinesssizes = {1, 5, 10, 20, 50, 100, 250, 500, 1000};
    private static final Map<Integer, Integer> mBusinesssizeMap = new HashMap<Integer, Integer>() {{
        put(1, 0);
        put(5, 1);
        put(10, 2);
        put(20, 3);
        put(50, 4);
        put(100, 5);
        put(250, 6);
        put(500, 7);
        put(1000, 8);
    }};
    private Snackbar snackbar;

    public static InputFilter EMOJI_FILTER = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int index = start; index < end; index++) {

                int type = Character.getType(source.charAt(index));

                if (type == Character.SURROGATE) {
                    return "";
                }
            }
            return null;
        }
    };

    public CompanyFragment() {
    }

    public static String getBusinessSizeString(Context context, String businesssize) {
        if (businesssize.length() == 0) {
            return "";
        }

        int size = Integer.parseInt(businesssize);
        int pos = mBusinesssizeMap.get(size);
        String[] values = context.getResources().getStringArray(R.array.businesssize);
        return values[pos];
    }

    public static String getRoleString(Context context, String role) {
        if (role.length() == 0) {
            return "";
        }

        int pos = mRoleMap.get(role);
        String[] values = context.getResources().getStringArray(R.array.roles_array);
        return values[pos];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((RegisterActivity)getActivity()).setAboutDataListener(this);
        View view = inflater.inflate(R.layout.fragment_company, container, false);
        ImageButton startTrial = view.findViewById(R.id.next);
        mCompany = view.findViewById(R.id.company);
        mRole = view.findViewById(R.id.role);
        mPhonenumber = view.findViewById(R.id.phonenumber);
        mAddress = view.findViewById(R.id.address);
        mBusinesssize = view.findViewById(R.id.businesssize);
        mCompany.setFilters(new InputFilter[]{EMOJI_FILTER});
        mPhonenumber.setFilters(new InputFilter[]{EMOJI_FILTER});
        mAddress.setFilters(new InputFilter[]{EMOJI_FILTER});

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (snackbar != null && snackbar.isShown()) {
                    snackbar.dismiss();
                    snackbar = null;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        mCompany.addTextChangedListener(textWatcher);
        mPhonenumber.addTextChangedListener(textWatcher);
        mAddress.addTextChangedListener(textWatcher);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
            R.array.roles_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mRole.setAdapter(adapter);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> businesssizeAdapter = ArrayAdapter.createFromResource(getActivity(),
            R.array.businesssize, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        businesssizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mBusinesssize.setAdapter(businesssizeAdapter);

        startTrial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCompany.getText().toString().length() == 0) {
                    snackbar = Snackbar.make(view, R.string.company_name_required,
                        Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    return;
                }
                else if (mPhonenumber.getText().toString().length() == 0) {
                    snackbar = Snackbar.make(view, R.string.phonenr_required,
                        Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    return;
                }
                else if (mAddress.getText().toString().length() == 0) {
                    snackbar = Snackbar.make(view, R.string.address_required,
                        Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    return;
                }
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.START_TRIAL);
                i.putExtra(RegisterActivity.EXTRA_COMPANY, mCompany.getText().toString());
                i.putExtra(RegisterActivity.EXTRA_ROLE, mRoles[mRole.getSelectedItemPosition()]);
                i.putExtra(RegisterActivity.EXTRA_PHONENUMBER, mPhonenumber.getText().toString());
                i.putExtra(RegisterActivity.EXTRA_ADDRESS, mAddress.getText().toString());
                i.putExtra(RegisterActivity.EXTRA_BUSINESSSIZE, mBusinesssizes[mBusinesssize.getSelectedItemPosition()]);
                getActivity().startActivity(i);
            }
        });

        return view;
    }

    @Override
    public void onDataReceived(JSONObject data) {
        if (data == null) {
            return;
        }

        try {
            String displayname = data.getString("displayname");
            String phone = data.getString("phone");
            mPhonenumber.setText(phone);
            String address = data.getString("address");
            mAddress.setText(address);
            String company = data.getString("company");
            mCompany.setText(company);
            String role = data.getString("role");
            int pos = 0;
            for (int i = 0; i < mRoles.length; i++) {
                if (mRoles[i].compareTo(role) == 0) {
                    pos = i;
                    break;
                }
            }
            mRole.setSelection(pos);

            pos = 0;
            int businesssize = data.getInt("businesssize");
            pos = mBusinesssizeMap.get(businesssize);
            mBusinesssize.setSelection(pos);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

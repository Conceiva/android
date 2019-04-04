package com.handwerkcloud.client;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

public class RegisterActivity extends FragmentActivity {

    public static final String CARPENTER = "CARPENTER";
    public static final String STOVEBUILDER = "STOVEBUILDER";
    public static final String WINDOWBUILDER = "WINDOWBUILDER";
    public static final String INSTALLER = "INSTALLER";
    public static final String ELECTRICIAN = "ELECTRICIAN";
    public static final String PAINTER = "PAINTER";
    public static final String PLUMBER = "PLUMBER";
    public static final String BRICKLAYER = "BRICKLAYER";
    public static final String ROOFER = "ROOFER";
    public static final String STUCCOER = "STUCCOER";
    public static final String ARCHITECT = "ARCHITECT";
    public static final String OTHER = "OTHER";
    public static final String SKIP = "SKIP";
    public static final String NEXT = "NEXT";
    public static final String SELECT_PROFESSION = "SELECT_PROFESSION";
    public static final String REGISTER_COMPANY = "REGISTER_COMPANY";
    public static final String EXTRA_INDUSTRY = "EXTRA_INDUSTRY";
    public static final String START_TRIAL = "START_TRIAL";
    public static final String EXTRA_COMPANY = "EXTRA_COMPANY";
    public static final String EXTRA_ROLE = "EXTRA_ROLE";
    public static final String EXTRA_PHONENUMBER = "EXTRA_PHONENUMBER";
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    public static final String EXTRA_WAIT_FOR_UPDATE = "EXTRA_WAIT_FOR_UPDATE";
    public static final String EXTRA_BUSINESSSIZE = "EXTRA_BUSINESSSIZE";
    public static final String ACTION_PREV = "ACTION_PREV";
    static final String FIRSTRUN_SHARED_PREFERENCE = "FIRSTRUN_SHARED_PREF";

    /**
     * The number of pages (wizard steps) to show
     */
    private static final int NUM_PAGES = 2;
    public static final String EXTRA_EDIT = "EXTRA_EDIT";

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private RegistrationViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter;
    private String mProfession;
    private String mCompany;
    private String mRole;
    private String mPhonenumber;
    private String mAddress;
    private int mBusinesssize;
    public ArrayList<OnUserDataReceivedListener> mUserDataListener = new ArrayList<>();
    private boolean mUpdateBeforeFinish;
    private ProgressBar mLoading;
    boolean mEditing = false;

    public interface OnUserDataReceivedListener {
        void onDataReceived(JSONObject data);
    }

    public void setAboutDataListener(OnUserDataReceivedListener listener) {
        this.mUserDataListener.add(listener);
    }

    //AsyncTask<Params, Progress, Result>
    //Params: type passed in the execute() call, and received in the doInBackground method
    //Progress: type of object passed in publishProgress calls
    //Result: object type returned by the doInBackground method, and received by onPostExecute()
    static class RegisterTask extends AsyncTask<String, Integer, JSONObject> {
        WeakReference<Context> mContext = null;
        WeakReference<RegisterActivity> mRegisterActivity = null;
        RegisterTask(Context context, RegisterActivity activity) {
            mContext = new WeakReference<>(context);
            mRegisterActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            String operation = params[0];
            Account account = AccountUtils.getCurrentOwnCloudAccount(mContext.get());
            OwnCloudAccount ocAccount = null;

            if (account != null) {
                try {
                    ocAccount = new OwnCloudAccount(account, mContext.get());
                } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                    e.printStackTrace();
                }

                OwnCloudClient client = null;
                try {
                    client = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, mContext.get());
                } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                    e.printStackTrace();
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AccountManager mAccountMgr = AccountManager.get(mContext.get());
                String userId = mAccountMgr.getUserData(account,
                    com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

                if (operation == "GET") {
                    UserProfileDataOperation gfo = new UserProfileDataOperation(userId, null);
                    RemoteOperationResult getresult = gfo.execute(client);

                    if (getresult.isSuccess()) {
                        JSONObject data = (JSONObject) getresult.getData().get(0);

                        return data;

                    }
                }
                else if (operation == "POST") {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext.get());
                    String company = preferences.getString(EXTRA_COMPANY, "");
                    String phone = preferences.getString(EXTRA_PHONENUMBER, "");
                    String role = preferences.getString(EXTRA_ROLE, "");
                    String address = preferences.getString(EXTRA_ADDRESS, "");
                    String industry = preferences.getString(EXTRA_INDUSTRY, "");
                    int businesssize = preferences.getInt(EXTRA_BUSINESSSIZE, 1);

                    if (company.length() != 0) {
                        JSONObject jObjectData = new JSONObject();
                        try {
                            jObjectData.put("company", company);
                            jObjectData.put("phone", phone);
                            jObjectData.put("address", address);
                            jObjectData.put("businesstype", industry);
                            jObjectData.put("role", role);
                            jObjectData.put("businesssize", businesssize);
                            //jObjectData.put("role", role);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String fields = jObjectData.toString();
                        UserProfileDataOperation sfo = new UserProfileDataOperation(userId, fields);
                        RemoteOperationResult result = sfo.execute(client);

                        if (result.isSuccess()) {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(EXTRA_COMPANY, "");
                            editor.putString(EXTRA_PHONENUMBER, "");
                            editor.putString(EXTRA_ROLE, "");
                            editor.putString(EXTRA_ADDRESS, "");
                            editor.putString(EXTRA_INDUSTRY, "");
                            editor.putInt(EXTRA_BUSINESSSIZE, 0);
                            editor.commit();
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            super.onPostExecute(data);
            if (mRegisterActivity.get() != null) {
                if (mRegisterActivity.get().mUpdateBeforeFinish && data == null) {
                    mRegisterActivity.get().finish();
                }
                else {
                    Iterator it = mRegisterActivity.get().mUserDataListener.iterator();
                    while (((Iterator) it).hasNext()) {
                        OnUserDataReceivedListener listener = (OnUserDataReceivedListener) it.next();
                        listener.onDataReceived(data);
                    }
                }
            }


        }
    }

    public static void runIfNeeded(Activity activity) {
        new RegisterTask(activity, null).execute("POST");
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                Fragment fragment = new ProfessionFragment();
                Bundle args = new Bundle();
                if (mEditing) {
                    args.putBoolean("EDIT", mEditing);
                }
                fragment.setArguments(args);
                return fragment;
            }
            else if (position == 1) {
                Fragment fragment = new CompanyFragment();
                return fragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    @Override
    public void onBackPressed() {
        int current = mPager.getCurrentItem();
        if (current != 0) {
            current--;
            mPager.setCurrentItem(current);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        new RegisterTask(this, this).execute("GET");

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (RegistrationViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setPagingEnabled(false);
        mLoading = findViewById(R.id.loading);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i)
    {
        handleIntent(i);
    }

    void handleIntent(Intent i) {
        if (i.hasExtra(EXTRA_EDIT)) {
            mEditing = true;
        }

        if (i.getAction() == ACTION_PREV) {
            int current = mPager.getCurrentItem();
            if (current != 0) {
                current--;
                mPager.setCurrentItem(current);
            }
        }

        if (i.hasExtra(EXTRA_WAIT_FOR_UPDATE)) {
            mUpdateBeforeFinish = true;
        }

       if (i.getAction() == NEXT) {
            if (mPager.getCurrentItem() != NUM_PAGES - 1) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
            else {
                finish();
            }
        }
        else if (i.getAction() == REGISTER_COMPANY) {
            mProfession = i.getStringExtra(EXTRA_INDUSTRY);
            mPager.setCurrentItem(2);
        }
       else if (i.getAction() == START_TRIAL) {
           mCompany = i.getStringExtra(EXTRA_COMPANY);
           mRole = i.getStringExtra(EXTRA_ROLE);
           mPhonenumber = i.getStringExtra(EXTRA_PHONENUMBER);
           mAddress = i.getStringExtra(EXTRA_ADDRESS);
           mBusinesssize = i.getIntExtra(EXTRA_BUSINESSSIZE, 1);
           SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
           SharedPreferences.Editor editor = preferences.edit();
           editor.putString(EXTRA_COMPANY, mCompany);
           editor.putString(EXTRA_PHONENUMBER, mPhonenumber);
           editor.putString(EXTRA_ROLE, mRole);
           editor.putString(EXTRA_ADDRESS, mAddress);
           editor.putString(EXTRA_INDUSTRY, mProfession);
           editor.putInt(EXTRA_BUSINESSSIZE, mBusinesssize);
           editor.commit();
           setResult(Activity.RESULT_OK);

           if (mUpdateBeforeFinish) {
               new RegisterTask(this, this).execute("POST");
               mLoading.setVisibility(View.VISIBLE);
           }
           else {
               finish();
           }
       }
    }
}

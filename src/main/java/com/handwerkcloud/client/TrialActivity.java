package com.handwerkcloud.client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.OCCapability;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

public class TrialActivity extends AppCompatActivity {

    public static final String EXTRA_TRIAL_REMAINING_SEC = "EXTRA_TRIAL_REMAINING_SEC";
    public static final String EXTRA_TRIAL_END_TIME = "EXTRA_TRIAL_END_TIME";
    public static final String EXTRA_TRIAL_END = "EXTRA_TRIAL_END";
    public static String EXTRA_TRIAL_EXPIRED = "EXTRA_TRIAL_EXPIRED";
    public static String EXTRA_GROUP_EXPIRED = "EXTRA_GROUP_EXPIRED";
    public static String EXTRA_SHOP_URL = "EXTRA_SHOP_URL";
    public static final String EXTRA_ACCOUNT_REMOVE_REMAINING_SEC = "EXTRA_ACCOUNT_REMOVE_REMAINING_SEC";
    private static final long CHECK_INTERVAL = 60 * 60 * 1000;

    //AsyncTask<Params, Progress, Result>
    //Params: type passed in the execute() call, and received in the doInBackground method
    //Progress: type of object passed in publishProgress calls
    //Result: object type returned by the doInBackground method, and received by onPostExecute()
    static class FeaturesTask extends AsyncTask<String, Integer, JSONObject> {
        WeakReference<Context> mContext = null;

        FeaturesTask(Context context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext.get());
            long currentTime = SystemClock.elapsedRealtime();
            long lastCheck = preferences.getLong("lastTrialCheck", 0);
            if (lastCheck != 0 && currentTime < lastCheck + CHECK_INTERVAL) {
                return null;
            }

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

                    FeaturesOperation gfo = new FeaturesOperation(userId);
                    RemoteOperationResult getresult = gfo.execute(client);

                    if (getresult.isSuccess()) {
                        JSONObject data = (JSONObject) getresult.getData().get(0);

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putLong("lastTrialCheck", currentTime);
                        editor.commit();
                        GetRemoteHCCapabilitiesOperation getCapabilities = new GetRemoteHCCapabilitiesOperation();
                        RemoteOperationResult result = getCapabilities.execute(client);

                        if (result.isSuccess()
                            && result.getData() != null && result.getData().size() > 0) {
                            // Read data from the result
                            HCCapability capability = (HCCapability) result.getData().get(0);
                            try {
                                data.put("shop_url", capability.getShopUrl());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        return data;
                    }

            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            super.onPostExecute(data);

            if (mContext.get() == null || data == null) {
                return;
            }

            try {
                boolean nextGroupExpiration = data.has("next_group_expiration");
                boolean isTrial = data.getBoolean("is_trial");

                if (nextGroupExpiration) {
                    JSONObject nextGroupExpObj = data.getJSONObject("next_group_expiration");
                    boolean groupExpired = nextGroupExpObj.getBoolean("group_expired");
                    if (groupExpired) {
                        Intent trialIntent = new Intent(mContext.get(), TrialActivity.class);
                        trialIntent.putExtra(EXTRA_GROUP_EXPIRED, groupExpired);
                        trialIntent.putExtra(EXTRA_SHOP_URL, data.optString("shop_url"));
                        mContext.get().startActivity(trialIntent);
                    }
                }
                else if (isTrial) {
                    Intent trialIntent = new Intent(mContext.get(), TrialActivity.class);
                    boolean trialExpired = data.getBoolean("trial_expired");
                    trialIntent.putExtra(EXTRA_TRIAL_EXPIRED, trialExpired);
                    trialIntent.putExtra(EXTRA_SHOP_URL, data.optString("shop_url"));

                    if (data.has("account_remove_remaining_sec")) {
                        int accountRemoveRemainingSec = data.getInt("account_remove_remaining_sec");
                        trialIntent.putExtra(EXTRA_ACCOUNT_REMOVE_REMAINING_SEC, accountRemoveRemainingSec);
                    }
                    if (data.has("trial_remaining_sec")) {
                        int trialRemainingSec = data.getInt("trial_remaining_sec");
                        trialIntent.putExtra(EXTRA_TRIAL_REMAINING_SEC, trialRemainingSec);
                    }
                    if (data.has("trial_end_time")) {
                        int trialEndTime = data.getInt("trial_end_time");
                        trialIntent.putExtra(EXTRA_TRIAL_END_TIME, trialEndTime);
                    }
                    if (data.has("trial_end")) {
                        String trialEnd = data.getString("trial_end");
                        trialIntent.putExtra(EXTRA_TRIAL_END, trialEnd);
                    }
                    mContext.get().startActivity(trialIntent);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void runIfNeeded(Activity activity) {
        new FeaturesTask(activity).execute();
    }

    public static void clearLastCheck(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("lastTrialCheck", 0);
        editor.commit();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trial);

        Intent intent = getIntent();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        TrialFragment fragment = new TrialFragment();
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_GROUP_EXPIRED, intent.getBooleanExtra(EXTRA_GROUP_EXPIRED, false));
        args.putBoolean(EXTRA_TRIAL_EXPIRED, intent.getBooleanExtra(EXTRA_TRIAL_EXPIRED, false));
        args.putInt(EXTRA_ACCOUNT_REMOVE_REMAINING_SEC, intent.getIntExtra(EXTRA_ACCOUNT_REMOVE_REMAINING_SEC, 0));
        args.putInt(EXTRA_TRIAL_REMAINING_SEC, intent.getIntExtra(EXTRA_TRIAL_REMAINING_SEC, 0));
        args.putInt(EXTRA_TRIAL_END_TIME, intent.getIntExtra(EXTRA_TRIAL_END_TIME, 0));
        args.putString(EXTRA_TRIAL_END, intent.getStringExtra(EXTRA_TRIAL_END));
        args.putString(EXTRA_SHOP_URL, intent.getStringExtra(EXTRA_SHOP_URL));
        fragment.setArguments(args);
        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}

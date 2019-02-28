package com.handwerkcloud.client;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FirstRunActivity;
import com.owncloud.android.ui.activity.RequestCredentialsActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.appcompat.app.AppCompatActivity;

public class CredentialActivity extends AppCompatActivity {

    static final String TAG = "CredentialActivity";

    /**
     * Helper class handling a callback from the {@link AccountManager} after the creation of
     * a new ownCloud {@link Account} finished, successfully or not.
     */
    public class CredentialCreationCallback implements AccountManagerCallback<Bundle> {

        boolean mMandatoryCreation;

        /**
         * Constructor
         *
         * @param mandatoryCreation     When 'true', if an account was not created, the app is closed.
         */
        public CredentialCreationCallback(boolean mandatoryCreation) {
            mMandatoryCreation = mandatoryCreation;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            boolean accountWasSet = false;
            if (future != null) {
                try {
                    Bundle result;
                    result = future.getResult();
                    String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    if (AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), name)) {
                        accountWasSet = true;
                    }

                } catch (OperationCanceledException e) {
                    Log_OC.d(TAG, "Account creation canceled");

                } catch (Exception e) {
                    Log_OC.e(TAG, "Account creation finished in exception: ", e);
                }

            } else {
                Log_OC.e(TAG, "Account creation callback with null bundle");
            }
            if (mMandatoryCreation && !accountWasSet) {
                moveTaskToBack(true);
            }
        }
    }

    private static class MyTask extends AsyncTask<Void, Void, String> {

        private WeakReference<CredentialActivity> activityReference;

        // only retain a weak reference to the activity
        MyTask(CredentialActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... params) {

            returnCredentials(activityReference.get());

            return "task finished";
        }

        @Override
        protected void onPostExecute(String result) {

            // get a reference to the activity if it is still there
            CredentialActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        Account account = AccountUtils.getCurrentOwnCloudAccount(this);
        if (account != null) {
            new MyTask(this).execute();
        }
        else if (intent.hasExtra("LOGIN_IF_ACCOUNT_UNAVAILABLE")){

           AccountManager am = AccountManager.get(getApplicationContext());
            am.addAccount(MainApp.getAccountType(this),
                null,
                null,
                null,
                this,
                new CredentialActivity.CredentialCreationCallback(true),
                new Handler());
        }
        else {

            Intent resultIntent = new Intent();
            resultIntent.putExtra("ACCOUNT_UNAVAILABLE", true);
            setResult(AppCompatActivity.RESULT_OK, resultIntent);
            finish();
        }

    }

    public static void returnCredentials(AppCompatActivity context) {

        Intent resultIntent = new Intent();

        Account account = AccountUtils.getCurrentOwnCloudAccount(context);
        if (account != null) {

            AccountManager accountManager = AccountManager.get(context);
            OwnCloudAccount ocAccount = null;
            try {
                ocAccount = new OwnCloudAccount(account, context);
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                e.printStackTrace();
            }
            try {
                OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context);
                OwnCloudCredentials cred = client.getCredentials();
                if (cred != null) {
                    resultIntent.putExtra("USERNAME", cred.getUsername());
                    resultIntent.putExtra("AUTH_TOKEN", cred.getAuthToken());
                }
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                e.printStackTrace();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            context.setResult(AppCompatActivity.RESULT_OK, resultIntent);
            context.finish();
        }
    }
}

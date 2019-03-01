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
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
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
import com.owncloud.android.utils.EncryptionUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import androidx.appcompat.app.AppCompatActivity;

import static com.owncloud.android.ui.activity.FirstRunActivity.EXTRA_EXIT;

public class CredentialActivity extends AppCompatActivity {

    static final String TAG = "CredentialActivity";
    static final String IMAGEMETER_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC5CQnGP0zWBSvgMTyKOEXyNl8rpZMn4D5JNP3OLjqaDoVYP4VM7BIVvAhEeq4Ja+JJit5Xa0l2B6wQOj32MARL8vFXFWDXwHAnxWcNmZ1mFY+2CgmwoN69DS+9se2/50TW3Z6NIKjGZDjgEmDGdhvFyJXFdN8JYzS+p2vDEtkccQIDAQAB";
    static final String IMAGEMETER_APPLICANT = "ImageMeter";
    static Map<String, String> mPublicKeys = new HashMap<String, String>();
    static String mApplicant = ""; // Currently only supports ImageMeter but we can add others in the future to support different keys
    static final int AUTHENTICATE = 1;

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

        mPublicKeys.put(IMAGEMETER_APPLICANT, IMAGEMETER_PUBLIC_KEY);

        if (intent.hasExtra("APPLICANT")) {
            mApplicant = intent.getStringExtra("APPLICANT");
        }

        Account account = AccountUtils.getCurrentOwnCloudAccount(this);
        if (account != null) {
            new MyTask(this).execute();
        }
        else if (intent.hasExtra("LOGIN_IF_ACCOUNT_UNAVAILABLE")){
            Intent authIntent = new Intent(getApplicationContext(), AuthenticatorActivity.class);
            authIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            authIntent.putExtra(EXTRA_EXIT, true);
            startActivityForResult(authIntent, AUTHENTICATE);
           /*AccountManager am = AccountManager.get(getApplicationContext());
            am.addAccount(MainApp.getAccountType(this),
                null,
                null,
                null,
                this,
                new CredentialActivity.CredentialCreationCallback(true),
                new Handler());*/
        }
        else {

            Intent resultIntent = new Intent();
            resultIntent.putExtra("ACCOUNT_UNAVAILABLE", true);
            setResult(AppCompatActivity.RESULT_OK, resultIntent);
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == AUTHENTICATE) {
            returnCredentials(this);
        }
    }

    static String encryptData(String txt)
    {
        String applicantKey = mPublicKeys.get(mApplicant);
        String encoded = "";
        byte[] encrypted = null;
        try {
            byte[] publicBytes = Base64.decode(applicantKey, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            encrypted = cipher.doFinal(txt.getBytes());
            encoded = Base64.encodeToString(encrypted, Base64.DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return encoded;
    }

    public static void returnCredentials(AppCompatActivity context) {

        Intent resultIntent = new Intent();
        resultIntent.putExtra("SERVER_URL", context.getString(R.string.server_url));
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
                    resultIntent.putExtra("USERNAME", encryptData(cred.getUsername()));
                    resultIntent.putExtra("AUTH_TOKEN", encryptData(cred.getAuthToken()));
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

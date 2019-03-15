package com.handwerkcloud.client;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.FrameLayout;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.ui.activity.FileActivity;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class RegisterActivity extends FragmentActivity {

    public static final String SELECT_PROFESSION = "SELECT_PROFESSION";
    public static final String REGISTER_COMPANY = "REGISTER_COMPANY";
    public static final String EXTRA_INDUSTRY = "EXTRA_INDUSTRY";
    public static final String DEFAULT_INDUSTRY = "DEFAULT_INDUSTRY";
    public static final String CONSTRUCTION_INDUSTRY = "CONSTRUCTION_INDUSTRY";
    public static final String ELECTRICAL_INDUSTRY = "ELECTRICAL_INDUSTRY";
    public static final String LANDSCAPING_INDUSTRY = "LANDSCAPING_INDUSTRY";
    public static final String INDUSTRIAL_INDUSTRY = "INDUSTRIAL_INDUSTRY";
    public static final String METAL_INDUSTRY = "METAL_INDUSTRY";
    public static final String PLUMBING_INDUSTRY = "PLUMBING_INDUSTRY";
    public static final String CARPENTERS_INDUSTRY = "CARPENTERS_INDUSTRY";
    public static final String START_TRIAL = "START_TRIAL";
    public static final String EXTRA_COMPANY = "EXTRA_COMPANY";
    public static final String EXTRA_ROLE = "EXTRA_ROLE";
    public static final String EXTRA_PHONENUMBER = "EXTRA_PHONENUMBER";
    static final String FIRSTRUN_SHARED_PREFERENCE = "FIRSTRUN_SHARED_PREF";
    private FrameLayout mFragmentContainer;

    public static void runIfNeeded(FileActivity fileActivity) {
        Account account = AccountUtils.getCurrentOwnCloudAccount(fileActivity);
        boolean firstRun = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(fileActivity);
       firstRun = preferences.getBoolean(FIRSTRUN_SHARED_PREFERENCE, true);
        if (account != null && firstRun) {
            Intent i = new Intent(fileActivity, RegisterActivity.class);
            fileActivity.startActivity(i);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mFragmentContainer = findViewById(R.id.fragment_container);

        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create a new Fragment to be placed in the activity layout
        RegisterFragment registerFragment = new RegisterFragment();

        // In case this activity was started with special instructions from an
        // Intent, pass the Intent's extras to the fragment as arguments
        registerFragment.setArguments(getIntent().getExtras());

        // Add the fragment to the 'fragment_container' FrameLayout
        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, registerFragment).commit();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i)
    {
        handleIntent(i);
    }

    void handleIntent(Intent i) {
        if (i.getAction() == SELECT_PROFESSION) {
            // Create fragment and give it an argument specifying the article it should show
            ProfessionFragment newFragment = new ProfessionFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
        else if (i.getAction() == REGISTER_COMPANY) {
            // Create fragment and give it an argument specifying the article it should show
            CompanyFragment newFragment = new CompanyFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
        else if (i.getAction() == START_TRIAL) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRSTRUN_SHARED_PREFERENCE, false);
            editor.apply();
            finish();
        }
    }
}

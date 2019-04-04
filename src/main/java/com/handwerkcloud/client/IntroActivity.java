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
import android.text.Html;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

public class IntroActivity extends FragmentActivity {

    public static final String PREV = "PREV";
    static private final int FIRSTRUN_RESULT = 1;
    private static final int REGISTER_RESULT = 2;
    public static final String SKIP = "SKIP";
    public static final String NEXT = "NEXT";

    /**
     * The number of pages (wizard steps) to show
     */
    private static final int NUM_PAGES = 3;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter pagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;


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
                Fragment fragment = new IntroFragment();
                Bundle arguments = new Bundle();
                arguments.putInt(IntroFragment.LAYOUT_ID, R.layout.fragment_intro);
                fragment.setArguments(arguments);
                return fragment;
            }
            else if (position == 1) {
                Fragment fragment = new IntroFragment();
                Bundle arguments = new Bundle();
                arguments.putInt(IntroFragment.LAYOUT_ID, R.layout.fragment_intro2);
                fragment.setArguments(arguments);
                return fragment;
            }
            else if (position == 2) {
                Fragment fragment = new IntroFragment();
                Bundle arguments = new Bundle();
                arguments.putInt(IntroFragment.LAYOUT_ID, R.layout.fragment_intro3);
                fragment.setArguments(arguments);
                return fragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

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

    public static void runIfNeeded(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean introShown = preferences.getBoolean("intro_shown", false);
        if (!introShown) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("intro_shown", true);
            editor.commit();
            Intent i = new Intent(context, IntroActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REGISTER_RESULT && resultCode != Activity.RESULT_CANCELED) {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.addOnPageChangeListener(viewPagerPageChangeListener);

        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);

        // adding bottom dots
        addBottomDots(0);

        handleIntent(getIntent());
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[NUM_PAGES];

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(R.color.color_accent));
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(getResources().getColor(R.color.primary));
    }

    @Override
    protected void onNewIntent(Intent i)
    {
        handleIntent(i);
    }

    void handleIntent(Intent i) {

        if (i.getAction() == NEXT) {
            if (mPager.getCurrentItem() != NUM_PAGES - 1) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
            else {
                finish();
            }
        }
        else if (i.getAction() == PREV) {
            int current = mPager.getCurrentItem();
            if (current != 0) {
                current--;
                mPager.setCurrentItem(current);
            }
        }
    }
}

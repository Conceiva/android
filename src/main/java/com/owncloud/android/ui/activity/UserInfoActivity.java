/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.handwerkcloud.client.CompanyFragment;
import com.handwerkcloud.client.FeaturesOperation;
import com.handwerkcloud.client.RegisterActivity;
import com.handwerkcloud.client.UserProfileDataOperation;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.handwerkcloud.client.ProfessionFragment.getBusinessTypeString;

/**
 * This Activity presents the user information.
 */
public class UserInfoActivity extends FileActivity implements Injectable {
    public static final String KEY_ACCOUNT = "ACCOUNT";

    private static final String TAG = UserInfoActivity.class.getSimpleName();
    private static final String KEY_USER_DATA = "USER_DATA";
    private static final String KEY_DIRECT_REMOVE = "DIRECT_REMOVE";
    private static final String KEY_FEATURES_INFO = "KEY_FEATURES_INFO";
    private static final String KEY_EXTRA_USER_INFO = "KEY_EXTRA_USER_INFO";
    private static final int KEY_DELETE_CODE = 101;
    private static final int EDIT_ACCOUNT_RESULT = 102;

    @BindView(R.id.empty_list_view) protected LinearLayout emptyContentContainer;
    @BindView(R.id.empty_list_view_text) protected TextView emptyContentMessage;
    @BindView(R.id.empty_list_view_headline) protected TextView emptyContentHeadline;
    @BindView(R.id.empty_list_icon) protected ImageView emptyContentIcon;
    @BindView(R.id.user_info_view) protected LinearLayout userInfoView;
    @BindView(R.id.user_icon) protected ImageView avatar;
    @BindView(R.id.userinfo_username) protected TextView userName;
    @BindView(R.id.userinfo_username_full) protected TextView fullName;
    @BindView(R.id.user_info_list) protected RecyclerView mUserInfoList;
    @BindView(R.id.empty_list_progress) protected ProgressBar multiListProgressBar;

    @BindString(R.string.user_information_retrieval_error) protected String sorryMessage;

    @Inject AppPreferences preferences;
    private float mCurrentAccountAvatarRadiusDimension;

    private Unbinder unbinder;

    private UserInfo userInfo;
    private Account account;
    private JSONObject extraUserInfo;
    private JSONObject featuresInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();

        account = Parcels.unwrap(bundle.getParcelable(KEY_ACCOUNT));

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_USER_DATA)) {
            userInfo = Parcels.unwrap(savedInstanceState.getParcelable(KEY_USER_DATA));
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_FEATURES_INFO)) {
            try {
                featuresInfo = new JSONObject(savedInstanceState.getString(KEY_FEATURES_INFO));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_EXTRA_USER_INFO)) {
            try {
                extraUserInfo = new JSONObject(savedInstanceState.getString(KEY_EXTRA_USER_INFO));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mCurrentAccountAvatarRadiusDimension = getResources().getDimension(R.dimen.nav_drawer_header_avatar_radius);

        setContentView(R.layout.user_info_layout);
        unbinder = ButterKnife.bind(this);

        setAccount(AccountUtils.getCurrentOwnCloudAccount(this));
        onAccountSet(false);

        boolean useBackgroundImage = URLUtil.isValidUrl(
                getStorageManager().getCapability(account.name).getServerBackground());

        setupToolbar(useBackgroundImage);
        updateActionBarTitleAndHomeButtonByString("");

        mUserInfoList.setAdapter(new UserInfoAdapter(null, ThemeUtils.primaryColor(getAccount(), true, this)));

        if (userInfo != null) {
            populateUserInfoUi(userInfo);
        } else {
            setMultiListLoadingMessage();
            fetchAndSetData();
        }

        setHeaderImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_ACCOUNT_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                userInfo = null;
                setMultiListLoadingMessage();
                fetchAndSetData();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_info_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.delete_account:
                openAccountRemovalConfirmationDialog(account, getSupportFragmentManager(), false);
                break;
            case R.id.edit_account:
                Intent i = new Intent(this, RegisterActivity.class);
                i.putExtra(RegisterActivity.EXTRA_WAIT_FOR_UPDATE, true);
                i.putExtra(RegisterActivity.EXTRA_EDIT, true);
                startActivityForResult(i, EDIT_ACCOUNT_RESULT);
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    private void setMultiListLoadingMessage() {
        if (emptyContentContainer != null) {
            emptyContentHeadline.setText(R.string.file_list_loading);
            emptyContentMessage.setText("");

            emptyContentIcon.setVisibility(View.GONE);
            emptyContentMessage.setVisibility(View.GONE);
            multiListProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryColor(this),
                    PorterDuff.Mode.SRC_IN);
            multiListProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void setErrorMessageForMultiList(String headline, String message, @DrawableRes int errorResource) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);
            emptyContentIcon.setImageResource(errorResource);

            multiListProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
            emptyContentMessage.setVisibility(View.VISIBLE);
        }
    }

    private void setHeaderImage() {
        if (getStorageManager().getCapability(account.name).getServerBackground() != null) {
            ViewGroup appBar = findViewById(R.id.appbar);

            if (appBar != null) {
                ImageView backgroundImageView = appBar.findViewById(R.id.drawer_header_background);

                String background = getStorageManager().getCapability(account.name).getServerBackground();
                int primaryColor = ThemeUtils.primaryColor(getAccount(), false, this);

                if (URLUtil.isValidUrl(background)) {
                    // background image
                    SimpleTarget target = new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                            Drawable[] drawables = {new ColorDrawable(primaryColor), resource};
                            LayerDrawable layerDrawable = new LayerDrawable(drawables);
                            backgroundImageView.setImageDrawable(layerDrawable);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            Drawable[] drawables = {new ColorDrawable(primaryColor),
                                    getResources().getDrawable(R.drawable.background)};
                            LayerDrawable layerDrawable = new LayerDrawable(drawables);
                            backgroundImageView.setImageDrawable(layerDrawable);
                        }
                    };

                    Glide.with(this)
                            .load(background)
                            .centerCrop()
                            .placeholder(R.drawable.background)
                            .error(R.drawable.background)
                            .crossFade()
                            .into(target);
                } else {
                    // plain color
                    backgroundImageView.setImageDrawable(new ColorDrawable(primaryColor));
                }
            }
        }
    }

    private void populateUserInfoUi(UserInfo userInfo) {
        userName.setText(account.name);
        avatar.setTag(account.name);
        DisplayUtils.setAvatar(account, this, mCurrentAccountAvatarRadiusDimension, getResources(), avatar, this);

        int tint = ThemeUtils.primaryColor(account, true, this);

        if (!TextUtils.isEmpty(userInfo.getDisplayName())) {
            fullName.setText(userInfo.getDisplayName());
        }

        if (userInfo.getPhone() == null && userInfo.getEmail() == null && userInfo.getAddress() == null
                && userInfo.getTwitter() == null && userInfo.getWebsite() == null) {

            setErrorMessageForMultiList(getString(R.string.userinfo_no_info_headline),
                getString(R.string.userinfo_no_info_text), R.drawable.ic_user);
        } else {
            emptyContentContainer.setVisibility(View.GONE);
            userInfoView.setVisibility(View.VISIBLE);

            if (mUserInfoList.getAdapter() instanceof UserInfoAdapter) {
                mUserInfoList.setAdapter(new UserInfoAdapter(createUserInfoDetails(userInfo), tint));
            }
        }
    }

    public static Date fromISO8601UTC(String dateStr) {
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);

        try {
            return df.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<UserInfoDetailsItem> createUserInfoDetails(UserInfo userInfo) {
        List<UserInfoDetailsItem> result = new LinkedList<>();

        try {
            if (featuresInfo != null && featuresInfo.has("next_group_expiration")) {
                JSONObject groupExpObj = featuresInfo.getJSONObject("next_group_expiration");
                if (!groupExpObj.getBoolean("group_expired")) {
                    Date expiresDate = fromISO8601UTC(groupExpObj.getString("expires"));
                    DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());
                    String expiresDateString = dateFormat.format(expiresDate);
                    addToListIfNeeded(result, R.drawable.baseline_calendar_today_24, String.format(getResources().getString(R.string.group_expires), expiresDateString), R.string.group_expires_date);
                }
                else {
                    result.add(new UserInfoDetailsItem(R.drawable.baseline_info_24, getResources().getString(R.string.group_expired), getResources().getString(R.string.group_expired)));
                }
            }
            if (featuresInfo != null && featuresInfo.getBoolean("is_trial")) {
                if (featuresInfo.getBoolean("trial_expired")) {
                    result.add(new UserInfoDetailsItem(R.drawable.baseline_info_24, getResources().getString(R.string.trial_ended), getResources().getString(R.string.trial_ended)));
                }
                else if (featuresInfo.has("trial_end")) {
                    Date endDate = fromISO8601UTC(featuresInfo.getString("trial_end"));
                    DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());
                    String endDateString = dateFormat.format(endDate);
                    result.add(new UserInfoDetailsItem(R.drawable.baseline_info_24, getResources().getString(R.string.trial_active), getResources().getString(R.string.trial_active)));
                    addToListIfNeeded(result, R.drawable.baseline_calendar_today_24, String.format(getResources().getString(R.string.trial_end), endDateString), R.string.trial_end_date);
                }
            }
            addToListIfNeeded(result, R.drawable.baseline_business_24, extraUserInfo.getString("company"), R.string.company);
            addToListIfNeeded(result, R.drawable.baseline_build_24, getBusinessTypeString(extraUserInfo.getString("businesstype"), this), R.string.businesstype);
            addToListIfNeeded(result, R.drawable.ic_user, CompanyFragment.getRoleString(this, extraUserInfo.getString("role")), R.string.role);
            addToListIfNeeded(result, R.drawable.baseline_people_24, CompanyFragment.getBusinessSizeString(this, extraUserInfo.getString("businesssize")), R.string.businesssize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        addToListIfNeeded(result, R.drawable.ic_phone, userInfo.getPhone(), R.string.user_info_phone);
        addToListIfNeeded(result, R.drawable.ic_email, userInfo.getEmail(), R.string.user_info_email);
        addToListIfNeeded(result, R.drawable.ic_map_marker, userInfo.getAddress(), R.string.user_info_address);
        try {
            addToListIfNeeded(result, R.drawable.baseline_location_city_24, extraUserInfo.getString("city") + " " + extraUserInfo.getString("zip"), R.string.user_info_city);
            addToListIfNeeded(result, R.drawable.baseline_language_24, CompanyFragment.getCountryName(this, extraUserInfo.getString("country")), R.string.user_info_country);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        addToListIfNeeded(result, R.drawable.ic_web, DisplayUtils.beautifyURL(userInfo.getWebsite()),
                    R.string.user_info_website);
        addToListIfNeeded(result, R.drawable.ic_twitter, DisplayUtils.beautifyTwitterHandle(userInfo.getTwitter()),
                    R.string.user_info_twitter);

        return result;
    }

    private void addToListIfNeeded(List<UserInfoDetailsItem> info, @DrawableRes int icon, String text,
                                   @StringRes int contentDescriptionInt) {
        if (!TextUtils.isEmpty(text)) {
            info.add(new UserInfoDetailsItem(icon, text, getResources().getString(contentDescriptionInt)));
        }
    }

    public static void openAccountRemovalConfirmationDialog(Account account, FragmentManager fragmentManager,
                                                            boolean removeDirectly) {
        UserInfoActivity.AccountRemovalConfirmationDialog dialog =
                UserInfoActivity.AccountRemovalConfirmationDialog.newInstance(account, removeDirectly);
        dialog.show(fragmentManager, "dialog");
    }

    public static class AccountRemovalConfirmationDialog extends DialogFragment {

        private Account account;

        public static UserInfoActivity.AccountRemovalConfirmationDialog newInstance(Account account,
                                                                                    boolean removeDirectly) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_ACCOUNT, account);
            bundle.putBoolean(KEY_DIRECT_REMOVE, removeDirectly);

            UserInfoActivity.AccountRemovalConfirmationDialog dialog = new
                    UserInfoActivity.AccountRemovalConfirmationDialog();
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            account = getArguments().getParcelable(KEY_ACCOUNT);
        }

        @Override
        public void onStart() {
            super.onStart();

            int color = ThemeUtils.primaryAccentColor(getActivity());

            AlertDialog alertDialog = (AlertDialog) getDialog();

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final boolean removeDirectly = getArguments().getBoolean(KEY_DIRECT_REMOVE);
            return new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.delete_account)
                    .setMessage(getResources().getString(R.string.delete_account_warning, account.name))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.common_ok,
                            (dialogInterface, i) -> {
                                // remove contact backup job
                                ContactsPreferenceActivity.cancelContactBackupJobForAccount(getActivity(), account);

                                ContentResolver contentResolver = getActivity().getContentResolver();

                                // disable daily backup
                                ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(
                                        contentResolver);

                                arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                        ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                                        "false");

                                String arbitraryDataPushString;

                                if (!TextUtils.isEmpty(arbitraryDataPushString = arbitraryDataProvider.getValue(
                                        account, PushUtils.KEY_PUSH)) &&
                                        !TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
                                    Gson gson = new Gson();
                                    PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryDataPushString,
                                            PushConfigurationState.class);
                                    pushArbitraryData.setShouldBeDeleted(true);
                                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, PushUtils.KEY_PUSH,
                                            gson.toJson(pushArbitraryData));
                                    EventBus.getDefault().post(new TokenPushEvent());
                                }


                                if (getActivity() != null && !removeDirectly) {
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelable(KEY_ACCOUNT, Parcels.wrap(account));
                                    Intent intent = new Intent();
                                    intent.putExtras(bundle);
                                    getActivity().setResult(KEY_DELETE_CODE, intent);
                                    getActivity().finish();
                                } else {
                                    AccountManager am = (AccountManager) getActivity()
                                            .getSystemService(ACCOUNT_SERVICE);

                                    am.removeAccount(account, null, null);

                                    Intent start = new Intent(getActivity(), FileDisplayActivity.class);
                                    start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(start);
                                }

                            })
                    .setNegativeButton(R.string.common_cancel, null)
                    .create();
        }
    }

    private void fetchExtraData() {
        OwnCloudAccount ocAccount = null;
        try {
            ocAccount = new OwnCloudAccount(account, this);
        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        }

        OwnCloudClient client = null;
        try {
            client = OwnCloudClientManagerFactory.getDefaultSingleton().
                getClientFor(ocAccount, this);
        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AccountManager mAccountMgr = AccountManager.get(this);
        String userId = mAccountMgr.getUserData(account,
            com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);
        UserProfileDataOperation gfo = new UserProfileDataOperation(userId, null);
        RemoteOperationResult getresult = gfo.execute(client);

        if (getresult.isSuccess()) {
            JSONObject data = (JSONObject) getresult.getData().get(0);

            extraUserInfo = data;

        }
        FeaturesOperation gfeato = new FeaturesOperation(userId);
        RemoteOperationResult getFeatresult = gfeato.execute(client);

        if (getFeatresult.isSuccess()) {
            JSONObject data = (JSONObject) getFeatresult.getData().get(0);

            featuresInfo = data;
        }
    }
    private void fetchAndSetData() {
        Thread t = new Thread(() -> {
            RemoteOperation getRemoteUserInfoOperation = new GetRemoteUserInfoOperation();
            RemoteOperationResult result = getRemoteUserInfoOperation.execute(account, this);

            // Handwerkcloud specific data
            fetchExtraData();

            if (result.isSuccess() && result.getData() != null) {
                userInfo = (UserInfo) result.getData().get(0);

                runOnUiThread(() -> populateUserInfoUi(userInfo));

            } else {
                // show error
                runOnUiThread(() -> setErrorMessageForMultiList(sorryMessage, result.getLogMessage(),
                        R.drawable.ic_list_empty_error));
                Log_OC.d(TAG, result.getLogMessage());
            }
        });

        t.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (userInfo != null) {
            outState.putParcelable(KEY_USER_DATA, Parcels.wrap(userInfo));
        }
        if (featuresInfo != null) {
            outState.putString(KEY_FEATURES_INFO, featuresInfo.toString());
        }
        if (extraUserInfo != null) {
            outState.putString(KEY_EXTRA_USER_INFO, extraUserInfo.toString());
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(TokenPushEvent event) {
        PushUtils.pushRegistrationToServer(preferences.getPushToken());
    }


    protected class UserInfoDetailsItem {
        @DrawableRes public int icon;
        public String text;
        public String iconContentDescription;

        public UserInfoDetailsItem(@DrawableRes int icon, String text, String iconContentDescription) {
            this.icon = icon;
            this.text = text;
            this.iconContentDescription = iconContentDescription;
        }
    }

    protected class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {
        protected List<UserInfoDetailsItem> mDisplayList;
        @ColorInt protected int mTintColor;

        public class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.icon) protected ImageView icon;
            @BindView(R.id.text) protected TextView text;

            public ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }

        public UserInfoAdapter(List<UserInfoDetailsItem> displayList, @ColorInt int tintColor) {
            mDisplayList = displayList == null ? new LinkedList<>() : displayList;
            mTintColor = tintColor;
        }

        public void setData(List<UserInfoDetailsItem> displayList) {
            mDisplayList = displayList == null ? new LinkedList<>() : displayList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.user_info_details_table_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserInfoDetailsItem item = mDisplayList.get(position);
            holder.icon.setImageResource(item.icon);
            holder.text.setText(item.text);
            holder.icon.setContentDescription(item.iconContentDescription);
            if (holder.icon.getDrawable() != null) {
                DrawableCompat.setTint(holder.icon.getDrawable(), mTintColor);
            }
        }

        @Override
        public int getItemCount() {
            return mDisplayList.size();
        }
    }
}

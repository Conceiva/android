package com.handwerkcloud.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.owncloud.android.R;

import org.w3c.dom.Text;

import androidx.fragment.app.Fragment;

import static com.handwerkcloud.client.TrialActivity.EXTRA_ACCOUNT_REMOVE_REMAINING_SEC;
import static com.handwerkcloud.client.TrialActivity.EXTRA_GROUP_EXPIRED;
import static com.handwerkcloud.client.TrialActivity.EXTRA_SHOP_URL;
import static com.handwerkcloud.client.TrialActivity.EXTRA_TRIAL_END;
import static com.handwerkcloud.client.TrialActivity.EXTRA_TRIAL_END_TIME;
import static com.handwerkcloud.client.TrialActivity.EXTRA_TRIAL_EXPIRED;
import static com.handwerkcloud.client.TrialActivity.EXTRA_TRIAL_REMAINING_SEC;

public class TrialFragment extends Fragment {
    private static final String WEBSITE_URL = "https://handwerkcloud.de";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trial, container, false);

        Bundle args = getArguments();

        boolean groupExpired = args.getBoolean(EXTRA_GROUP_EXPIRED, false);
        boolean trialExpired = args.getBoolean(EXTRA_TRIAL_EXPIRED, false);
        int accountRemoveRemainingSec = args.getInt(EXTRA_ACCOUNT_REMOVE_REMAINING_SEC, 0);
        int trialRemainingSec = args.getInt(EXTRA_TRIAL_REMAINING_SEC, 0);
        int trialEndTime = args.getInt(EXTRA_TRIAL_END_TIME, 0);
        String trialEnd = args.getString(EXTRA_TRIAL_END, "");
        final String shopUrl = args.getString(EXTRA_SHOP_URL, "");

        TextView introText = view.findViewById(R.id.intro);
        TextView trialText = view.findViewById(R.id.trial_text);
        TextView accountRemoveText = view.findViewById(R.id.account_remove_text);
        TextView trialPurchaseDesc = view.findViewById(R.id.trial_purchase_desc);
        MaterialButton trialPurchase = view.findViewById(R.id.trial_purchase);
        MaterialButton trialContinue = view.findViewById(R.id.trial_continue);

        trialPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String actionUrl = shopUrl;
                if (actionUrl.length() == 0) {
                    actionUrl = WEBSITE_URL;
                }
                Intent purchaseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl));
                startActivity(purchaseIntent);
            }
        });

        trialContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        if (groupExpired) {
            introText.setText(R.string.account_title);
            trialContinue.setVisibility(View.GONE);
            trialText.setText(R.string.group_expired);
            trialPurchaseDesc.setVisibility(View.VISIBLE);
            if (accountRemoveRemainingSec != 0) {
                int days = accountRemoveRemainingSec / 60 / 60 / 24;
                if (days > 0) {
                    accountRemoveText.setText(getResources().getQuantityString(R.plurals.account_remove_days_remaining, days, days));
                } else {
                    accountRemoveText.setText(R.string.account_remove_1_day_remaining);
                }
            }
            else {
                accountRemoveText.setVisibility(View.GONE);
            }
        }
        else if (trialExpired) {
            trialContinue.setVisibility(View.GONE);
            trialText.setText(R.string.trial_expired);
            trialPurchaseDesc.setVisibility(View.VISIBLE);
            if (accountRemoveRemainingSec != 0) {
                int days = accountRemoveRemainingSec / 60 / 60 / 24;
                if (days > 0) {
                    accountRemoveText.setText(getResources().getQuantityString(R.plurals.account_remove_days_remaining, days, days));
                }
                else {
                    accountRemoveText.setText(R.string.account_remove_1_day_remaining);
                }
            }
            else {
                accountRemoveText.setVisibility(View.GONE);
            }
        }
        else {
            trialPurchaseDesc.setVisibility(View.GONE);
            accountRemoveText.setVisibility(View.GONE);
            int days = trialRemainingSec / 60 / 60 / 24;
            if (days > 0) {
                trialText.setText(getResources().getQuantityString(R.plurals.trial_days_remaining, days, days));
            }
            else {
                trialText.setText(R.string.trial_1_day_remaining);
            }
        }
        return view;
    }
}

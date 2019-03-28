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

        boolean trialExpired = args.getBoolean(EXTRA_TRIAL_EXPIRED, false);
        int trialRemainingSec = args.getInt(EXTRA_TRIAL_REMAINING_SEC, 0);
        int trialEndTime = args.getInt(EXTRA_TRIAL_END_TIME, 0);
        String trialEnd = args.getString(EXTRA_TRIAL_END, "");

        TextView trialText = view.findViewById(R.id.trial_text);
        TextView trialPurchaseDesc = view.findViewById(R.id.trial_purchase_desc);
        MaterialButton trialPurchase = view.findViewById(R.id.trial_purchase);
        MaterialButton trialContinue = view.findViewById(R.id.trial_continue);

        trialPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent purchaseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL));
                startActivity(purchaseIntent);
            }
        });

        trialContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        if (trialExpired) {
            trialContinue.setVisibility(View.GONE);
            trialText.setText(R.string.trial_expired);
            trialPurchaseDesc.setVisibility(View.VISIBLE);
        }
        else {
            trialPurchaseDesc.setVisibility(View.GONE);
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

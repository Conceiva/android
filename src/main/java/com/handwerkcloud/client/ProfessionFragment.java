package com.handwerkcloud.client;

import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.owncloud.android.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfessionFragment extends Fragment {

    public ProfessionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_profession, container, false);

        LinearLayout defaultIndustry = view.findViewById(R.id.default_industry);
        defaultIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.DEFAULT_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout constructionIndustry = view.findViewById(R.id.construction_industry);
        constructionIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.CONSTRUCTION_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout electricalIndustry = view.findViewById(R.id.electrical_industry);
        electricalIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.ELECTRICAL_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout landscaping = view.findViewById(R.id.landscaping_industry);
        landscaping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.LANDSCAPING_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout industrialIndustry = view.findViewById(R.id.industrial_industry);
        industrialIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.INDUSTRIAL_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout metalIndustry = view.findViewById(R.id.metal_industry);
        metalIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.METAL_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout plumbingIndustry = view.findViewById(R.id.plumbing_industry);
        plumbingIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.PLUMBING_INDUSTRY);
                getActivity().startActivity(i);
            }
        });

        LinearLayout carpentersIndustry = view.findViewById(R.id.carpenters_industry);
        carpentersIndustry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getActivity(), RegisterActivity.class);
                i.setAction(RegisterActivity.REGISTER_COMPANY);
                i.putExtra(RegisterActivity.EXTRA_INDUSTRY, RegisterActivity.CARPENTERS_INDUSTRY);
                getActivity().startActivity(i);
            }
        });
        return view;
    }
}

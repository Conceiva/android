package com.handwerkcloud.client;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.content.DialogInterface;
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

    void showConfirmDialog(int value, String type) {
        String selection = String.format(getString(R.string.your_selection), getString(value));
        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.confirm_selection)
            .setTitle(selection)
            .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent i = new Intent(getActivity(), RegisterActivity.class);
                    i.setAction(RegisterActivity.REGISTER_COMPANY);
                    i.putExtra(RegisterActivity.EXTRA_INDUSTRY, type);
                    getActivity().startActivity(i);
                }})
            .setNegativeButton(R.string.change, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_profession, container, false);

        LinearLayout carpenter = view.findViewById(R.id.carpenter);
        carpenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.carpenter, RegisterActivity.CARPENTER);
            }
        });

        LinearLayout stovebuilder = view.findViewById(R.id.stovebuilder);
        stovebuilder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.stovebuilder, RegisterActivity.STOVEBUILDER);
            }
        });

        LinearLayout windowbuilder = view.findViewById(R.id.windowbuilder);
        windowbuilder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.windowbuilder, RegisterActivity.WINDOWBUILDER);
            }
        });

        LinearLayout installer = view.findViewById(R.id.installer);
        installer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.installer, RegisterActivity.INSTALLER);
            }
        });

        LinearLayout electrician = view.findViewById(R.id.electrician);
        electrician.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.electrician, RegisterActivity.ELECTRICIAN);
            }
        });

        LinearLayout painter = view.findViewById(R.id.painter);
        painter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.painter, RegisterActivity.PAINTER);
            }
        });

        LinearLayout flasher = view.findViewById(R.id.flasher);
        flasher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.flasher, RegisterActivity.FLASHER);
            }
        });

        LinearLayout bricklayer = view.findViewById(R.id.bricklayer);
        bricklayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.bricklayer, RegisterActivity.BRICKLAYER);
            }
        });

        LinearLayout roofer = view.findViewById(R.id.roofer);
        roofer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.roofer, RegisterActivity.ROOFER);
            }
        });

        LinearLayout stuccoer = view.findViewById(R.id.stuccoer);
        stuccoer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.stuccoer, RegisterActivity.STUCCOER);
            }
        });

        LinearLayout architect = view.findViewById(R.id.architect);
        architect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.architect, RegisterActivity.ARCHITECT);
            }
        });

        LinearLayout other = view.findViewById(R.id.other);
        other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog(R.string.other, RegisterActivity.OTHER);
            }
        });
        return view;
    }
}

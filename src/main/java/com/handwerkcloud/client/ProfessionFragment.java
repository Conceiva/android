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
import android.widget.TextView;

import com.owncloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfessionFragment extends Fragment implements RegisterActivity.OnUserDataReceivedListener {

    Map<Integer, String> mSelectedProfessions = new HashMap<Integer, String>();
    private TextView next;

    public ProfessionFragment() {
    }

    void selectProfession(int value, String type, View view) {
        if (mSelectedProfessions.containsKey(value)) {
            mSelectedProfessions.remove(value);
            view.setBackgroundResource(0);
        }
        else {
            mSelectedProfessions.put(value, type);
            view.setBackgroundResource(R.drawable.boxborder);
        }

        next.setVisibility(mSelectedProfessions.size() != 0 ? View.VISIBLE : View.GONE);
    }

    void showConfirmDialog() {
        String selections = "";
        String type = "";
        Iterator it = mSelectedProfessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if (selections.length() != 0) {
                selections += ", ";
                type += ",";
            }
            selections += getString((Integer)pair.getKey());
            type += pair.getValue();
        }
        final String finalType = type;
        String selection = String.format(getString(R.string.your_selection), selections);
        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.confirm_selection)
            .setTitle(selection)
            .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent i = new Intent(getActivity(), RegisterActivity.class);
                    i.setAction(RegisterActivity.REGISTER_COMPANY);
                    i.putExtra(RegisterActivity.EXTRA_INDUSTRY, finalType);
                    getActivity().startActivity(i);
                }})
            .setNegativeButton(R.string.change, null).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ((RegisterActivity)getActivity()).setAboutDataListener(this);
        View view = inflater.inflate(R.layout.fragment_profession, container, false);

        next = view.findViewById(R.id.next);
        next.setVisibility(View.GONE);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmDialog();
            }
        });

        LinearLayout carpenter = view.findViewById(R.id.carpenter);
        carpenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.carpenter, RegisterActivity.CARPENTER, view);
            }
        });

        LinearLayout stovebuilder = view.findViewById(R.id.stovebuilder);
        stovebuilder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.stovebuilder, RegisterActivity.STOVEBUILDER, view);
            }
        });

        LinearLayout windowbuilder = view.findViewById(R.id.windowbuilder);
        windowbuilder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.windowbuilder, RegisterActivity.WINDOWBUILDER, view);
            }
        });

        LinearLayout installer = view.findViewById(R.id.installer);
        installer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.installer, RegisterActivity.INSTALLER, view);
            }
        });

        LinearLayout electrician = view.findViewById(R.id.electrician);
        electrician.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.electrician, RegisterActivity.ELECTRICIAN, view);
            }
        });

        LinearLayout painter = view.findViewById(R.id.painter);
        painter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.painter, RegisterActivity.PAINTER, view);
            }
        });

        LinearLayout flasher = view.findViewById(R.id.flasher);
        flasher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.flasher, RegisterActivity.FLASHER, view);
            }
        });

        LinearLayout bricklayer = view.findViewById(R.id.bricklayer);
        bricklayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.bricklayer, RegisterActivity.BRICKLAYER, view);
            }
        });

        LinearLayout roofer = view.findViewById(R.id.roofer);
        roofer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.roofer, RegisterActivity.ROOFER, view);
            }
        });

        LinearLayout stuccoer = view.findViewById(R.id.stuccoer);
        stuccoer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.stuccoer, RegisterActivity.STUCCOER, view);
            }
        });

        LinearLayout architect = view.findViewById(R.id.architect);
        architect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.architect, RegisterActivity.ARCHITECT, view);
            }
        });

        LinearLayout other = view.findViewById(R.id.other);
        other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.other, RegisterActivity.OTHER, view);
            }
        });
        return view;
    }

    @Override
    public void onDataReceived(JSONObject data) {
        try {
            String businesstype = data.getString("businesstype");
            // parse the businesstype and set the selected professions
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

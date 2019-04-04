package com.handwerkcloud.client;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private Button next;
    LinearLayout carpenter;
    LinearLayout stovebuilder;
    LinearLayout windowbuilder;
    LinearLayout installer;
    LinearLayout electrician;
    LinearLayout painter;
    LinearLayout plumber;
    LinearLayout bricklayer;
    LinearLayout roofer;
    LinearLayout stuccoer;
    LinearLayout architect;
    LinearLayout other;
    public ProfessionFragment() {
    }

    void selectProfession(int value, String type, View view) {
        int paddingBottom = view.getPaddingBottom();
        int paddingTop = view.getPaddingTop();
        int paddingLeft = view.getPaddingLeft();
        int paddingRight = view.getPaddingRight();
        if (mSelectedProfessions.containsKey(value)) {
            mSelectedProfessions.remove(value);
            //view.setBackgroundResource(R.drawable.profession_button);
            //view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            ((LinearLayout)view).removeView(view.findViewWithTag(type));
        }
        else {
            mSelectedProfessions.put(value, type);
            //view.setBackgroundResource(R.drawable.boxborder);
            //view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            LinearLayout.LayoutParams params = new LinearLayout
                .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ImageView imageview = new ImageView(getActivity());

            // Add image path from drawable folder.
            imageview.setImageResource(R.drawable.baseline_check_circle_24);
            imageview.setLayoutParams(params);
            imageview.setTag(type);
            ((LinearLayout)view).addView(imageview);
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

        carpenter = view.findViewById(R.id.carpenter);
        carpenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.carpenter, RegisterActivity.CARPENTER, view);
            }
        });

        stovebuilder = view.findViewById(R.id.stovebuilder);
        stovebuilder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.stovebuilder, RegisterActivity.STOVEBUILDER, view);
            }
        });

        windowbuilder = view.findViewById(R.id.windowbuilder);
        windowbuilder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.windowbuilder, RegisterActivity.WINDOWBUILDER, view);
            }
        });

        installer = view.findViewById(R.id.installer);
        installer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.installer, RegisterActivity.INSTALLER, view);
            }
        });

        electrician = view.findViewById(R.id.electrician);
        electrician.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.electrician, RegisterActivity.ELECTRICIAN, view);
            }
        });

        painter = view.findViewById(R.id.painter);
        painter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.painter, RegisterActivity.PAINTER, view);
            }
        });

        plumber = view.findViewById(R.id.plumber);
        plumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.plumber, RegisterActivity.PLUMBER, view);
            }
        });

        bricklayer = view.findViewById(R.id.bricklayer);
        bricklayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.bricklayer, RegisterActivity.BRICKLAYER, view);
            }
        });

        roofer = view.findViewById(R.id.roofer);
        roofer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.roofer, RegisterActivity.ROOFER, view);
            }
        });

        stuccoer = view.findViewById(R.id.stuccoer);
        stuccoer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.stuccoer, RegisterActivity.STUCCOER, view);
            }
        });

        architect = view.findViewById(R.id.architect);
        architect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.architect, RegisterActivity.ARCHITECT, view);
            }
        });

        other = view.findViewById(R.id.other);
        other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfession(R.string.other, RegisterActivity.OTHER, view);
            }
        });
        return view;
    }

    public static String getBusinessTypeString(String value, Context context) {
        String retVal = "";
        String businesstype = value;
        // parse the businesstype and set the selected professions
        String[] values = businesstype.split(",");
        for (int i = 0; i < values.length; i++) {
            if (retVal.length() != 0) {
                retVal += ", ";
            }

            if (values[i].compareTo(RegisterActivity.CARPENTER) == 0) {
                retVal += context.getString(R.string.carpenter);
            }
            else if (values[i].compareTo(RegisterActivity.STOVEBUILDER) == 0) {
                retVal += context.getString(R.string.stovebuilder);
            }
            else if (values[i].compareTo(RegisterActivity.WINDOWBUILDER) == 0) {
                retVal += context.getString(R.string.windowbuilder);
            }
            else if (values[i].compareTo(RegisterActivity.INSTALLER) == 0) {
                retVal += context.getString(R.string.installer);
            }
            else if (values[i].compareTo(RegisterActivity.ELECTRICIAN) == 0) {
                retVal += context.getString(R.string.electrician);
            }
            else if (values[i].compareTo(RegisterActivity.PAINTER) == 0) {
                retVal += context.getString(R.string.painter);
            }
            else if (values[i].compareTo(RegisterActivity.PLUMBER) == 0) {
                retVal += context.getString(R.string.plumber);
            }
            else if (values[i].compareTo(RegisterActivity.BRICKLAYER) == 0) {
                retVal += context.getString(R.string.bricklayer);
            }
            else if (values[i].compareTo(RegisterActivity.ROOFER) == 0) {
                retVal += context.getString(R.string.roofer);
            }
            else if (values[i].compareTo(RegisterActivity.STUCCOER) == 0) {
                retVal += context.getString(R.string.stuccoer);
            }
            else if (values[i].compareTo(RegisterActivity.ARCHITECT) == 0) {
                retVal += context.getString(R.string.architect);
            }
            else if (values[i].compareTo(RegisterActivity.OTHER) == 0) {
                retVal += context.getString(R.string.other);
            }
        }
        return retVal;
    }

    @Override
    public void onDataReceived(JSONObject data) {
        if (data == null) {
            return;
        }

        try {
            String businesstype = data.getString("businesstype");
            // parse the businesstype and set the selected professions
            String[] values = businesstype.split(",");
            for (int i = 0; i < values.length; i++) {
                if (values[i].compareTo(RegisterActivity.CARPENTER) == 0) {
                    selectProfession(R.string.carpenter, RegisterActivity.CARPENTER, carpenter);
                }
                else if (values[i].compareTo(RegisterActivity.STOVEBUILDER) == 0) {
                    selectProfession(R.string.stovebuilder, RegisterActivity.STOVEBUILDER, stovebuilder);
                }
                else if (values[i].compareTo(RegisterActivity.WINDOWBUILDER) == 0) {
                    selectProfession(R.string.windowbuilder, RegisterActivity.WINDOWBUILDER, windowbuilder);
                }
                else if (values[i].compareTo(RegisterActivity.INSTALLER) == 0) {
                    selectProfession(R.string.installer, RegisterActivity.INSTALLER, installer);
                }
                else if (values[i].compareTo(RegisterActivity.ELECTRICIAN) == 0) {
                    selectProfession(R.string.electrician, RegisterActivity.ELECTRICIAN, electrician);
                }
                else if (values[i].compareTo(RegisterActivity.PAINTER) == 0) {
                    selectProfession(R.string.painter, RegisterActivity.PAINTER, painter);
                }
                else if (values[i].compareTo(RegisterActivity.PLUMBER) == 0) {
                    selectProfession(R.string.plumber, RegisterActivity.PLUMBER, plumber);
                }
                else if (values[i].compareTo(RegisterActivity.BRICKLAYER) == 0) {
                    selectProfession(R.string.bricklayer, RegisterActivity.BRICKLAYER, bricklayer);
                }
                else if (values[i].compareTo(RegisterActivity.ROOFER) == 0) {
                    selectProfession(R.string.roofer, RegisterActivity.ROOFER, roofer);
                }
                else if (values[i].compareTo(RegisterActivity.STUCCOER) == 0) {
                    selectProfession(R.string.stuccoer, RegisterActivity.STUCCOER, stuccoer);
                }
                else if (values[i].compareTo(RegisterActivity.ARCHITECT) == 0) {
                    selectProfession(R.string.architect, RegisterActivity.ARCHITECT, architect);
                }
                else if (values[i].compareTo(RegisterActivity.OTHER) == 0) {
                    selectProfession(R.string.other, RegisterActivity.OTHER, other);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

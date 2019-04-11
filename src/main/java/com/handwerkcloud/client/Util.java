package com.handwerkcloud.client;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.owncloud.android.R;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import androidx.appcompat.app.AlertDialog;

public class Util {
    static public void promptPlaystore(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        // Get the layout inflater
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.playstore_dialog, null);
        ImageButton openPlaystore = view.findViewById(R.id.open_playstore);
        openPlaystore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String appPackageName = "de.dirkfarin.imagemeter"; // getPackageName() from Context or Activity object

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                intent.setPackage("com.android.vending");
                activity.startActivity(intent);
            }
        });

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
            // Add action buttons
            .setNegativeButton(android.R.string.cancel, null);
        builder.show();

    }

    public static void promptUploadStarted(Activity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean showDialog = preferences.getBoolean("showUploadStartedDialog", true);
        if (showDialog) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // Get the layout inflater
            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.upload_started_dialog, null);
            CheckBox notAgain = view.findViewById(R.id.not_again);
            MaterialButton buttonContinue = view.findViewById(R.id.button_continue);

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view);

            AlertDialog dialog = builder.create();

            buttonContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (notAgain.isChecked()) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("showUploadStartedDialog", false);
                        editor.commit();
                    }
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }

    public static void promptUploadFailed(Activity activity, String message) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean showDialog = preferences.getBoolean("showUploadFailedDialog", true);
        if (showDialog) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // Get the layout inflater
            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.upload_failed_dialog, null);
            CheckBox notAgain = view.findViewById(R.id.not_again);
            MaterialButton buttonContinue = view.findViewById(R.id.button_continue);
            MaterialButton buttonRetry = view.findViewById(R.id.button_retry);
            TextView desc = view.findViewById(R.id.desc);
            desc.setText(message);

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view);

            AlertDialog dialog = builder.create();

            buttonContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (notAgain.isChecked()) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("showUploadFailedDialog", false);
                        editor.commit();
                    }
                    dialog.dismiss();
                }
            });

            buttonRetry.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (notAgain.isChecked()) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("showUploadFailedDialog", false);
                        editor.commit();
                    }
                    FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
                    new Thread(() -> requester.retryFailedUploads(view.getContext(), null, null)).start();
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
    }
}

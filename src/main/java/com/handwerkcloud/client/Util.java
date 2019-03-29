package com.handwerkcloud.client;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import com.owncloud.android.R;

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
}

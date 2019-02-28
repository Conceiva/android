package com.handwerkcloud.testcredentialquery;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import androidx.appcompat.app.AppCompatActivity;

public class CredentialQueryActivity extends AppCompatActivity {

    static final int GET_CREDENTIALS_RESULT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credential_query);
        Button clickButton = (Button) findViewById(R.id.button);
        clickButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_GET_CONTENT);
                i.putExtra("LOGIN_IF_ACCOUNT_UNAVAILABLE", true);
                i.setComponent(new ComponentName("com.handwerkcloud.client", "com.handwerkcloud.client.CredentialActivity"));
                startActivityForResult(i, GET_CREDENTIALS_RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GET_CREDENTIALS_RESULT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                TextView textView = (TextView) findViewById(R.id.textfield);
                if (data.hasExtra("ACCOUNT_UNAVAILABLE")) {
                    textView.setText("Account unavailable");
                }
                else {
                    textView.setText(data.getStringExtra("USERNAME") + " " + data.getStringExtra("AUTH_TOKEN"));
                }
            }
            else {
                TextView textView = (TextView) findViewById(R.id.textfield);
                textView.setText("could not get credentials");
            }
        }
    }
}

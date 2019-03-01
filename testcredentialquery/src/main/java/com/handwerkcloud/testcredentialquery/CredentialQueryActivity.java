package com.handwerkcloud.testcredentialquery;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import androidx.appcompat.app.AppCompatActivity;

public class CredentialQueryActivity extends AppCompatActivity {

    static final int GET_CREDENTIALS_RESULT = 1;
    static final String privateKey = "";

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
                i.putExtra("APPLICANT", "ImageMeter");
                i.setComponent(new ComponentName("com.handwerkcloud.client", "com.handwerkcloud.client.CredentialActivity"));
                startActivityForResult(i, GET_CREDENTIALS_RESULT);
            }
        });
    }

    public static String decrypt(byte[] inputData)
        throws Exception {
        byte[] privateBytes = Base64.decode(privateKey, Base64.DEFAULT);
        PrivateKey key = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(inputData);

        return new String(decryptedBytes);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GET_CREDENTIALS_RESULT) {
            // Make sure the request was successful
            String serverUrl = data.getStringExtra("SERVER_URL");
            if (resultCode == RESULT_OK) {
                TextView textView = (TextView) findViewById(R.id.textfield);
                if (data.hasExtra("ACCOUNT_UNAVAILABLE")) {
                    textView.setText("Account unavailable");
                }
                else {
                    try {
                        textView.setText("server: " + serverUrl + " username: " + decrypt(Base64.decode(data.getStringExtra("USERNAME"), Base64.DEFAULT)) + " " + decrypt(Base64.decode(data.getStringExtra("AUTH_TOKEN"), Base64.DEFAULT)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                TextView textView = (TextView) findViewById(R.id.textfield);
                textView.setText("server: " + serverUrl + " could not get credentials");
            }
        }
    }
}

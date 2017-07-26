package com.maxchehab.fingerprinter;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class FingerprintActivity extends AppCompatActivity {

    private static final String KEY_NAME = "fingerprinter_key";
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private Cipher cipher;
    private FingerprintManager.CryptoObject cryptoObject;


    public final static Object authenticateLock = new Object();

    public static boolean authenticate = false;
    public static String username;


    public ImageView statusIcon;
    public TextView statusText;
    private ProgressBar countdown;
    private TextView cancelButton;
    private CustomSpinner usernameSelector;
    private TextView selectedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        new KillListener(this).start();

        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        if(!keyguardManager.isKeyguardSecure()){
            Toast.makeText(this, "Lock screen security not enabled in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Fingerprint authentication permission not enabled", Toast.LENGTH_LONG).show();
            return;
        }

        if(!fingerprintManager.hasEnrolledFingerprints()){
            Toast.makeText(this, "Register at least one fingerprint in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        generateKey();

        if(cipherInit()){
            cryptoObject = new FingerprintManager.CryptoObject(cipher);
            FingerprintHandler helper = new FingerprintHandler(this);
            helper.startAuth(fingerprintManager,cryptoObject);
        }


        statusIcon = (ImageView)findViewById(R.id.statusIcon);
        statusText = (TextView)findViewById(R.id.statusText);
        countdown = (ProgressBar)findViewById(R.id.countDown);
        cancelButton = (TextView)findViewById(R.id.cancelButton);
        usernameSelector = (CustomSpinner)findViewById(R.id.usernameSelector);
        selectedUsername = (TextView)findViewById(R.id.selectedUsername);


        Long timeDifference = System.currentTimeMillis() - getIntent().getLongExtra("STARTTIME", System.currentTimeMillis());

        Long remainingTime = 30000 - timeDifference;

        Integer percentage = (int)((remainingTime * 100) / 30000);
        countdown.setProgress(percentage);
        ObjectAnimator animation = ObjectAnimator.ofInt(countdown, "progress", 0);
        animation.setDuration(remainingTime);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();

        String[] usernameList = {"maxchehab","will smith","really long username"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner,usernameList);
        usernameSelector.setAdapter(adapter);

        selectedUsername.setText(usernameSelector.getSelectedItem().toString());
        selectedUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernameSelector.performClick();
                usernameSelector.setVisibility(View.VISIBLE);
                selectedUsername.setVisibility(View.GONE);
            }
        });

        usernameSelector.setSpinnerEventsListener(new CustomSpinner.OnSpinnerEventsListener() {
            @Override
            public void onSpinnerOpened(Spinner spin) {

            }

            @Override
            public void onSpinnerClosed(Spinner spin) {
                selectedUsername.setText(usernameSelector.getSelectedItem().toString());
                usernameSelector.setVisibility(View.GONE);
                selectedUsername.setVisibility(View.VISIBLE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                authenticate = false;
                                synchronized (authenticateLock) {
                                    authenticateLock.notify();
                                }
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Are you sure you want to cancel the authentication?")
                        .setPositiveButton("cancel", dialogClickListener)
                        .setNegativeButton("no", dialogClickListener)
                        .setTitle("Attention")
                        .setIcon(R.mipmap.ic_warning)
                        .show();

            }
        });

    }

    private static class KillListener extends Thread {

        private Activity activity;

        public KillListener(Activity activity){
            this.activity = activity;
        }

        public void run(){
            try{
                synchronized (authenticateLock) {
                    authenticateLock.wait();
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            activity.finish();
        }
    }


    @Override
    protected void onDestroy(){
        Log.i("MAINACT", "onDestroy()");
        super.onDestroy();
    }


    protected void generateKey(){
        try{
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        }catch(Exception e){
            e.printStackTrace();
        }

        try{
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        }catch(NoSuchAlgorithmException | NoSuchProviderException e){
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }

        try{
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        }catch(NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e){
            throw new RuntimeException(e);
        }
    }

    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }
}

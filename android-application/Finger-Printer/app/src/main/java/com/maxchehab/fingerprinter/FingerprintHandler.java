package com.maxchehab.fingerprinter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.TextView;
import android.widget.Toast;

import static com.maxchehab.fingerprinter.FingerprintActivity.authenticate;
import static com.maxchehab.fingerprinter.FingerprintActivity.authenticateLock;
import static com.maxchehab.fingerprinter.FingerprintActivity.username;
import static com.maxchehab.fingerprinter.FingerprintActivity.selectedUsername;

/**
 * Created by maxchehab on 7/16/17.
 */

public class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

    private CancellationSignal cancellationSignal;
    private Context appContext;
    FingerprintActivity fingerprintActivity;

    public FingerprintHandler(Context context){
        appContext = context;
        fingerprintActivity = (FingerprintActivity) context;
    }

    public void startAuth(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject){
        cancellationSignal = new CancellationSignal();

        if(ActivityCompat.checkSelfPermission(appContext, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        manager.authenticate(cryptoObject,cancellationSignal,0,this,null);
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        ((FingerprintActivity)appContext).restartAuth();
        Toast.makeText(appContext,
                "Authentication error\n" + errString + "\nid: " + errMsgId,
                Toast.LENGTH_LONG).show();

    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        Toast.makeText(appContext,
                "Authentication help\n" + helpString,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAuthenticationFailed() {
        fingerprintActivity.statusIcon.setImageResource(R.mipmap.ic_warning);
        fingerprintActivity.statusText.setText("Fingerprint not recognized.\nTry again.");
        fingerprintActivity.statusText.setTextColor(ResourcesCompat.getColor(appContext.getResources(), R.color.colorError, null));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        fingerprintActivity.statusIcon.setImageResource(R.mipmap.ic_check);
        fingerprintActivity.statusText.setText("Fingerprint recognized.");
        fingerprintActivity.statusText.setTextColor(ResourcesCompat.getColor(appContext.getResources(), R.color.colorSuccess, null));

        authenticate = true;
        username = selectedUsername.getText().toString();
        synchronized (authenticateLock) {
            authenticateLock.notify();
        }
    }
}

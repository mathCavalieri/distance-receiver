package com.example.matheus.distancereceiver;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

/**
 * Created by Matheus on 4/5/2016.
 */
public class MyInstanceIDListenerService extends com.google.android.gms.iid.InstanceIDListenerService{

    @Override
    public void onTokenRefresh() {
        Log.d("MATH HERE", "onTokenRefresh called");
        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}

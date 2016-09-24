package com.example.matheus.distancereceiver;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class RegistrationIntentService extends IntentService {

    public RegistrationIntentService(){
        super("Name for the service");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        Log.d("onHandleIntent called","onHandleIntent called" );
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(Constants.sender_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Intent asd = new Intent(this, MainActivity.class);
            Constants.token = token;
            Log.d("MATH HERE", "token: "+token);
            asd.putExtra("token", token);
            startService(asd);

        }

        catch (Exception e) {
            Toast.makeText(this, "Error getting a client token for GCM: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("MATH HERE", "error: "+e.getMessage());
            Constants.lastError += " "+e.getMessage();
            Intent asd = new Intent(this, MainActivity.class);
            startService(asd);
        }
    }

}

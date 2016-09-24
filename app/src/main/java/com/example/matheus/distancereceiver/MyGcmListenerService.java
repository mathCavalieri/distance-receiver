package com.example.matheus.distancereceiver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Matheus on 4/6/2016.
 */
public class MyGcmListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(String from, Bundle data) {
        try {
            String distance = data.getString("distance");
            Intent intent = new Intent(this, MainActivity.class);
            Constants.distance = distance;
            Log.d("MATH HERE", "Set the distance to the static variable 'Constants.distance'. Value: '" + distance + "'");
            startService(intent);
        }
        catch(Exception e) {
            Constants.lastError += " "+e.getMessage();
        }

    }

}
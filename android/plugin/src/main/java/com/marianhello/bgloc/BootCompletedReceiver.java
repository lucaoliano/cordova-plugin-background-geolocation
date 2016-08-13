/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.LocationService;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.ConfigurationDAO;
/**
 * BootCompletedReceiver class
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = BootCompletedReceiver.class.getName();

    @Override
     public void onReceive(Context context, Intent intent) {
        boolean taskRemoved = intent.getBooleanExtra("taskRemoved", false);

        if (taskRemoved) {
            Log.d(TAG, "Received service restart");
        } else {
            Log.d(TAG, "Received boot completed");
        }
        
        ConfigurationDAO dao = DAOFactory.createConfigurationDAO(context);
        Config config = null;

        try {
            config = dao.retrieveConfiguration();
        } catch (JSONException e) {
            //noop
        }

        if (config == null) { return; }

        if (taskRemoved) {
        	Log.d(TAG, "Location Service restarting " + config.toString());
        } else {
            Log.d(TAG, "Boot completed " + config.toString());
        }

        if (config.getStartOnBoot() || taskRemoved) {
        	if (taskRemoved) {
            	Log.i(TAG, "Starting service after it has been removed");
            } else {
                Log.i(TAG, "Starting service after boot");
            }
            Intent locationServiceIntent = new Intent(context, LocationService.class);
            locationServiceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            locationServiceIntent.putExtra("config", config);

            context.startService(locationServiceIntent);
        }
     }
}

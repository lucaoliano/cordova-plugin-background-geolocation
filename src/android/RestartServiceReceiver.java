package com.tenforwardconsulting.cordova.bgloc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.ServiceProvider;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Created by lucaoliano on 26/12/15.
 */
public class RestartServiceReceiver extends BroadcastReceiver {

    private static final String TAG = "RestartServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        try {
            Log.d(TAG, Arrays.toString(context.getApplicationContext().fileList()));
            FileInputStream fis = context.getApplicationContext().openFileInput("config.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line = null, input = "";
            while ((line = reader.readLine()) != null)
                input += line;
            reader.close();
            fis.close();
            Log.d(TAG, input + "");
            Config config = new Config(input);
            if (config.getServiceProvider() == null)
                config.setServiceProvider(ServiceProvider.ANDROID_DISTANCE_FILTER);
            Intent i;
            switch (config.getServiceProvider()) {
                case ANDROID_FUSED_LOCATION:
                    i = new Intent(context.getApplicationContext(), FusedLocationService.class);
                    break;
                case ANDROID_DISTANCE_FILTER:
                default:
                    i = new Intent(context.getApplicationContext(), DistanceFilterLocationService.class);
                    break;
            }

            i.putExtra("config", config);
            context.startService(i);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() + "", e);
        }

    }
}
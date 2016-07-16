/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.location.Location;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.ToneGenerator;

import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.cordova.JSONErrorFactory;

import org.json.JSONObject;

/**
 * AbstractLocationProvider
 */
public abstract class AbstractLocationProvider implements LocationProvider {
    private static final String TAG = "AbstractLocationProvider";

    private static final int PERMISSION_DENIED_ERROR_CODE = 2;

    protected static enum Tone {
        BEEP,
        BEEP_BEEP_BEEP,
        LONG_BEEP,
        DOODLY_DOO,
        CHIRP_CHIRP_CHIRP,
        DIALTONE
    };

    protected Integer PROVIDER_ID;
    protected LocationService locationService;
    protected Location lastLocation;
    protected Config config;

    protected ToneGenerator toneGenerator;

    protected AbstractLocationProvider(LocationService locationService) {
        this.locationService = locationService;
        this.config = locationService.getConfig();
    }

    public void onCreate() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }

    public void onDestroy() {
        toneGenerator.release();
    }

    public Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter) {
        return locationService.registerReceiver(receiver, filter);
    }

    public void unregisterReceiver (BroadcastReceiver receiver) {
        locationService.unregisterReceiver(receiver);
    }

    public void handleLocation (Location location) {
        locationService.handleLocation(new BackgroundLocation(PROVIDER_ID, location));
    }

    public void handleSecurityException (SecurityException e) {
        JSONObject error = JSONErrorFactory.getJSONError(PERMISSION_DENIED_ERROR_CODE, e.getMessage());
        locationService.handleError(error);
    }

    /**
     * Plays debug sound
     * @param name
     */
    protected void startTone(Tone name) {
        int tone = 0;
        int duration = 1000;

        switch (name) {
            case BEEP:
                tone = ToneGenerator.TONE_PROP_BEEP;
                break;
            case BEEP_BEEP_BEEP:
                tone = ToneGenerator.TONE_CDMA_CONFIRM;
                break;
            case LONG_BEEP:
                tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
                break;
            case DOODLY_DOO:
                tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
                break;
            case CHIRP_CHIRP_CHIRP:
                tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                break;
            case DIALTONE:
                tone = ToneGenerator.TONE_SUP_RINGTONE;
                break;
        }

        toneGenerator.startTone(tone, duration);
    }
}

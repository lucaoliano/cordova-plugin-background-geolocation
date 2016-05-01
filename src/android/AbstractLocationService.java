/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.tenforwardconsulting.cordova.bgloc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.AlarmManager;
import android.support.v4.app.NotificationCompat;
import android.app.Service;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.marianhello.cordova.bgloc.Config;
import com.marianhello.cordova.bgloc.Constant;
import com.marianhello.cordova.bgloc.ServiceProvider;
import com.tenforwardconsulting.cordova.bgloc.data.LocationProxy;
import com.tenforwardconsulting.cordova.bgloc.data.LocationDAO;
import com.tenforwardconsulting.cordova.bgloc.data.DAOFactory;

import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.LOG;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractLocationService extends Service {
    private static final String TAG = "AbstractLocationService";

    private JSONObject oauthToken;

    protected Config config;
    private Boolean isActionReceiverRegistered = false;

    protected Location lastLocation;
    protected ToneGenerator toneGenerator;

    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getExtras();
            switch (data.getInt(Constant.ACTION)) {
                case Constant.ACTION_START_RECORDING:
                    startRecording();
                    break;
                case Constant.ACTION_STOP_RECORDING:
                    stopRecording();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "OnBind" + intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        // Receiver for actions
        registerActionReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null) {
            // config = Config.fromByteArray(intent.getByteArrayExtra("config"));
            config = (Config) intent.getParcelableExtra("config");
            Log.i(TAG, "Config: " + config.toString());

            // Build a Notification required for running service in foreground.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentTitle(config.getNotificationTitle());
            builder.setContentText(config.getNotificationText());
            builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
            if (config.getNotificationIcon() != null) {
                builder.setSmallIcon(getPluginResource(config.getSmallNotificationIcon()));
                builder.setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(), getPluginResource(config.getLargeNotificationIcon())));
            }
            if (config.getNotificationIconColor() != null) {
                builder.setColor(this.parseNotificationIconColor(config.getNotificationIconColor()));
            }

            try {
                Map<String, String> map = new HashMap<String, String>();
                map.put("access_token", config.getAccessToken());
                map.put("refresh_token", config.getRefreshToken());
                oauthToken = new JSONObject(map);
            } catch (Exception e) {
                oauthToken = null;
            }

            setClickEvent(builder);

            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
            startForeground(0, notification);
        }

        //We want this service to continue running until it is explicitly stopped
        return START_REDELIVER_INTENT;
    }

    public Integer getPluginResource(String resourceName) {
        return getApplication().getResources().getIdentifier(resourceName, "drawable", getApplication().getPackageName());
    }

    /**
     * Adds an onclick handler to the notification
     */
    protected NotificationCompat.Builder setClickEvent (NotificationCompat.Builder builder) {
        int requestCode = new Random().nextInt();
        Context context     = getApplicationContext();
        String packageName  = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        return builder.setContentIntent(contentIntent);
    }

    private Integer parseNotificationIconColor(String color) {
        int iconColor = 0;
        if (color != null) {
            try {
                iconColor = Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "couldn't parse color from android options");
            }
        }
        return iconColor;
    }

    /**
     * Plays debug sound
     * @param name
     */
    protected void startTone(String name) {
        int tone = 0;
        int duration = 1000;

        if (name.equals("beep")) {
            tone = ToneGenerator.TONE_PROP_BEEP;
        } else if (name.equals("beep_beep_beep")) {
            tone = ToneGenerator.TONE_CDMA_CONFIRM;
        } else if (name.equals("long_beep")) {
            tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
        } else if (name.equals("doodly_doo")) {
            tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
        } else if (name.equals("chirp_chirp_chirp")) {
            tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
        } else if (name.equals("dialtone")) {
            tone = ToneGenerator.TONE_SUP_RINGTONE;
        }
        toneGenerator.startTone(tone, duration);
    }

    protected void persistLocation (LocationProxy location) {
        LocationDAO dao = DAOFactory.createLocationDAO(this.getApplicationContext());

        if (dao.persistLocation(location)) {
            Log.d(TAG, "Persisted Location: " + location);
        } else {
            Log.w(TAG, "Failed to persist location");
        }
    }

    protected void handleLocation (Location location) {
        final LocationProxy bgLocation = LocationProxy.fromAndroidLocation(location);
        bgLocation.setServiceProvider(config.getServiceProvider());

        if (config.isDebugging()) {
            bgLocation.setDebug(true);
            persistLocation(bgLocation);
        }

        Log.d(TAG, "Broadcasting update message: " + bgLocation.toString());
        try {
            final JSONObject locJson = bgLocation.toJSONObject();
            String locStr = locJson.toString();
            Intent intent = new Intent(Constant.ACTION_FILTER);
            intent.putExtra(Constant.ACTION, Constant.ACTION_LOCATION_UPDATE);
            intent.putExtra(Constant.DATA, locStr);
            this.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                // @SuppressLint("NewApi")
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (config.getUrl() != null && !config.getUrl().isEmpty()) {
                        try {
                            JSONObject params = new JSONObject();
                            params.accumulate("location", locJson);
                            if (config.getParams() != null && !config.getParams().isEmpty()) {
                                params = new JSONObject(config.getParams());
                            }


                            Map<String, String> headers = new HashMap<String, String>();
                            if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
                                JSONObject o = new JSONObject(config.getHeaders());
                                for (Iterator<String> iterator = o.keys(); iterator.hasNext(); ) {
                                    String key = iterator.next();
                                    String value = o.getString(key);
                                    headers.put(key, value);
                                }
                            }

                            new PostPositionAsyncTask().execute(config.getUrl(), params, headers);
                        } catch (Exception e) {
                            LOG.e(TAG, e.getMessage());
                        }
                    }
                    Log.d(TAG, "Final Result Receiver");
                    Bundle results = getResultExtras(true);
                    if (results.getString(Constant.LOCATION_SENT_INDICATOR) == null) {
                        Log.w(TAG, "Main activity seems to be killed");
                        if (config.getStopOnTerminate() == false) {
                            bgLocation.setDebug(false);
                            // persistLocation(bgLocation);
                            Log.d(TAG, "Persisting location. Reason: Main activity was killed.");
                        }
                    }
              }
            }, null, Activity.RESULT_OK, null, null);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to broadcast location");
        }
    }

    public Intent registerActionReceiver () {
        if (isActionReceiverRegistered) { return null; }

        isActionReceiverRegistered = true;
        return registerReceiver(actionReceiver, new IntentFilter(Constant.ACTION_FILTER));
    }

    public void unregisterActionReceiver () {
        if (!isActionReceiverRegistered) { return; }

        unregisterReceiver(actionReceiver);
        isActionReceiverRegistered = false;
    }

    public void startDelayed () {
        Class serviceProviderClass = null;
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        try {
            Intent serviceIntent = new Intent(this, ServiceProvider.getClass(config.getServiceProvider()));
            serviceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            serviceIntent.putExtra("config", config.toParcel().marshall());
            PendingIntent pintent = PendingIntent.getService(this, 0, serviceIntent, 0);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, pintent);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Service restart failed");
        }
    }

    protected abstract void cleanUp();

    protected abstract void startRecording();

    protected abstract void stopRecording();


    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        cleanUp();
        if (config.isDebugging()) {
            Toast.makeText(this, "Background location tracking stopped", Toast.LENGTH_SHORT).show();
        }
        return super.stopService(intent); // not needed???
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Destroyed Location update Service");
        toneGenerator.release();
        unregisterActionReceiver();
        cleanUp();
        stopForeground(true);
        super.onDestroy();
    }

    // @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task has been removed");
        unregisterActionReceiver();
        if (config.getStopOnTerminate()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
        if (!config.getStopOnTerminate()) {
            sendBroadcast(new Intent("YouWillNeverKillMe"));
        }
    }

    private class PostPositionAsyncTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object... args) {
            try {
                Map<String, String> headers = (Map<String, String>) args[2];
                if (oauthToken != null && !oauthToken.isNull("access_token")) {
                    headers.put("Authorization", "Bearer " + oauthToken.getString("access_token"));
                }

                JSONObject result = invoke((String) args[0], (JSONObject) args[1], headers);

                if (result == null && oauthToken != null && !oauthToken.isNull("refresh_token")) {
                    oauthToken = refreshToken(config.getOauthUrl(), config.getUsername(), config.getPassword(), config.getClientId(), oauthToken.getString("refresh_token"));

                    if (oauthToken != null && !oauthToken.isNull("access_token")) {
                        headers.put("Authorization", "Bearer " + oauthToken.getString("access_token"));
                        result = invoke((String) args[0], (JSONObject) args[1], headers);
                    }
                }
            } catch (JSONException e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return null;
        }

        private static final String REFRESH_1 = "?grant_type=refresh_token&client_id=";
        private static final String REFRESH_2 = "&refresh_token=";

        private static final String LOGIN_1 = "?grant_type=password&client_id=";
        private static final String LOGIN_2 = "&response_type=token&username=";
        private static final String LOGIN_3 = "&password=";

        private JSONObject refreshToken(String url, String username, String password, String clientId, String refreshToken) {
            StringBuilder sb = new StringBuilder(url.length() + REFRESH_1.length() + clientId.length() + REFRESH_2.length() + refreshToken.length());
            sb.append(url).append(REFRESH_1).append(clientId).append(REFRESH_2).append(refreshToken);
            JSONObject res = invoke(sb.toString(), null, null);
            if (res == null) {
                sb = new StringBuilder(url.length() + LOGIN_1.length() + clientId.length() + LOGIN_2.length() + username.length() + LOGIN_3.length() + password.length());
                sb.append(url).append(LOGIN_1).append(clientId).append(LOGIN_2).append(username).append(LOGIN_3).append(password);
                res = invoke(sb.toString(), null, null);
            }

            return res;
        }

        private JSONObject invoke(String sUrl, JSONObject jsonData, Map<String, String> headers) {
            try {
                URL url = new URL(sUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                if (headers != null && headers.size() > 0) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                if (jsonData != null) {
                    // Send POST output.
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                    out.write(jsonData.toString());
                    out.close();
                }

                // read the response
                int responseCode = conn.getResponseCode();
                if (responseCode != 401 && responseCode != 403) {
                    //Get Response
                    InputStream is = conn.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append(System.getProperty("line.separator"));
                    }
                    rd.close();
                    String res = response.toString();

                    if (responseCode == 200 && res.isEmpty()) {
                        res = "{}";
                    }

                    try {
                        JSONObject jsonObject = new JSONObject(res);
                        return jsonObject;
                    } catch (JSONException e) {
                        Log.d(TAG, "Error creating result JSON due to: " + e.getLocalizedMessage());
                    }
                }
            } catch (MalformedURLException e) {
                Log.d(TAG, e.getLocalizedMessage());
            } catch (IOException e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return null;
        }
    }
}

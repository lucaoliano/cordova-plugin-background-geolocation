/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.accounts.Account;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.bgloc.sync.AccountHelper;
import com.marianhello.bgloc.sync.AuthenticatorService;
import com.marianhello.bgloc.sync.SyncService;
import com.marianhello.logging.LoggerManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Random;

public class LocationService extends Service {

    private LocationDAO dao;
    private Config config;
    private LocationProvider provider;
    private Account syncAccount;

    private org.slf4j.Logger log;

    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command sent by the service to
     * any registered clients with the new position.
     */
    public static final int MSG_LOCATION_UPDATE = 3;


    /**
     * Command sent by the service to
     * any registered clients with error.
     */
    public static final int MSG_ERROR = 4;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        log = LoggerManager.getLogger(LocationService.class);
        log.info("Creating LocationService");

        super.onCreate();
        dao = (DAOFactory.createLocationDAO(this));
        syncAccount = AccountHelper.CreateSyncAccount(this,
                AuthenticatorService.getAccount(getStringResource(Config.ACCOUNT_TYPE_RESOURCE)));
    }

    @Override
    public void onDestroy() {
        log.info("Destroying LocationService");
        provider.onDestroy();
//        stopForeground(true);
        super.onDestroy();
    }

    // @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        log.debug("Task has been removed");
        if (config.getStopOnTerminate()) {
            log.info("Stopping self");
            stopSelf();
        } else {
            log.info("Continue running in background");
//            Intent intent = new Intent( this, DummyActivity.class );
//            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
//            startActivity(intent);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.info("Received start startId: {} intent: {}", startId, intent);

        if (provider != null) {
            provider.onDestroy();
        }

        if (intent == null) {
            //service has been probably restarted so we need to load config from db
            ConfigurationDAO dao = DAOFactory.createConfigurationDAO(this);
            try {
                config = dao.retrieveConfiguration();
            } catch (JSONException e) {
                log.error("Config exception: {}", e.getMessage());
                config = new Config(); //using default config
            }
        } else {
            if (intent.hasExtra("config")) {
                config = (Config) intent.getParcelableExtra("config");
            } else {
                config = new Config(); //using default config
            }
        }

        log.debug("Will start service with: {}", config.toString());

        LocationProviderFactory spf = new LocationProviderFactory(this);
        provider = spf.getInstance(config.getLocationProvider());

        if (config.getStartForeground()) {
            // Build a Notification required for running service in foreground.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentTitle(config.getNotificationTitle());
            builder.setContentText(config.getNotificationText());
            if (config.getSmallNotificationIcon() != null) {
                builder.setSmallIcon(getDrawableResource(config.getSmallNotificationIcon()));
            } else {
                builder.setSmallIcon(android.R.drawable.ic_menu_mylocation);
            }
            if (config.getLargeNotificationIcon() != null) {
                builder.setLargeIcon(BitmapFactory.decodeResource(getApplication().getResources(), getDrawableResource(config.getLargeNotificationIcon())));
            }
            if (config.getNotificationIconColor() != null) {
                builder.setColor(this.parseNotificationIconColor(config.getNotificationIconColor()));
            }

            setClickEvent(builder);

            Notification notification = builder.build();
            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
            startForeground(startId, notification);
        }

        provider.startRecording();

        //We want this service to continue running until it is explicitly stopped
        return START_STICKY;
    }

    protected int getAppResource(String name, String type) {
        return getApplication().getResources().getIdentifier(name, type, getApplication().getPackageName());
    }

    protected Integer getDrawableResource(String resourceName) {
        return getAppResource(resourceName, "drawable");
    }

    protected String getStringResource(String name) {
        return getApplication().getString(getAppResource(name, "string"));
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
                log.error("Couldn't parse color from android options");
            }
        }
        return iconColor;
    }

    public void startRecording() {
        provider.startRecording();
    }

    public void stopRecording() {
        provider.stopRecording();
    }


    /**
     * Handle location from location location provider
     *
     * All locations updates are recorded in local db at all times.
     * Also location is also send to all messenger clients.
     *
     * If option.url is defined, each location is also immediately posted.
     * If post is successful, the location is deleted from local db.
     * All failed to post locations are coalesced and send in some time later in one single batch.
     * Batch sync takes place only when number of failed to post locations reaches syncTreshold.
     *
     * If only option.syncUrl is defined, locations are send only in single batch,
     * when number of locations reaches syncTreshold.
     *
     * @param location
     */
    public void handleLocation (BackgroundLocation location) {

        // for sake of simplicity we're intentionally one location behind
        if (config.hasUrl() || config.hasSyncUrl()) {
            log.debug("Locations count: {} threshold: {}", dao.getValidLocationsCount(), config.getSyncThreshold());
            if (dao.getValidLocationsCount() >= config.getSyncThreshold()) {
                log.debug("Attempt to sync locations");
                SyncService.sync(syncAccount, getStringResource(Config.CONTENT_AUTHORITY_RESOURCE));
            }
        }

        persistLocation(location);

        if (config.hasUrl()) {
            postLocation(location);
        }

        Bundle bundle = new Bundle();
        bundle.putParcelable("location", location);
        Message msg = Message.obtain(null, MSG_LOCATION_UPDATE);
        msg.setData(bundle);

        sendClientMessage(msg);
    }

    public void sendClientMessage(Message msg) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    public void handleError(JSONObject error) {
        Bundle bundle = new Bundle();
        bundle.putString("error", error.toString());
        Message msg = Message.obtain(null, MSG_ERROR);
        msg.setData(bundle);

        sendClientMessage(msg);
    }

    // method will mutate location
    public void persistLocation (BackgroundLocation location) {
        try {
            Long locationId = dao.persistLocationWithLimit(location, config.getMaxLocations());
            location.setLocationId(locationId);
            log.debug("Persisted location: {}", location.toString());
        } catch (SQLException e) {
            log.error("Failed to persist location: {} error: {}", location.toString(), e.getMessage());
        }
    }

    public void postLocation(BackgroundLocation location) {
        PostLocationTask task = new LocationService.PostLocationTask();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, location);
        }
        else {
            task.execute(location);
        }
    }

    /**
     * Forces the main activity to re-launch if it's unloaded.
     */
    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }

    public Config getConfig() {
        return this.config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    private class PostLocationTask extends AsyncTask<BackgroundLocation, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(BackgroundLocation... locations) {
            log.debug("Executing PostLocationTask#doInBackground");
            JSONArray jsonLocations = new JSONArray();
            for (BackgroundLocation location : locations) {
                try {
                    JSONObject jsonLocation = location.toJSONObject();
                    jsonLocations.put(jsonLocation);
                } catch (JSONException e) {
                    log.warn("Location to json failed: {}", location.toString());
                    return false;
                }
            }

            String url = config.getUrl();
            log.debug("Posting json to url: {} headers: {}", url, config.getHttpHeaders());
            int responseCode;

            try {
                responseCode = HttpPostService.postJSON(url, jsonLocations, config.getHttpHeaders());
            } catch (Throwable e) {
                log.warn("Error while posting locations: {}", e.getMessage());
                return false;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("Server error while posting locations responseCode: {}", responseCode);
                return false;
            }

            for (BackgroundLocation location : locations) {
                Long locationId = location.getLocationId();
                if (locationId != null) {
                    dao.deleteLocation(locationId);
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            log.debug("PostLocationTask#onPostExecute");
        }
    }
}

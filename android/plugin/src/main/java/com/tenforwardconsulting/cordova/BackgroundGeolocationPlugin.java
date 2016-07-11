/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. new methods isLocationEnabled, mMessageReciever, handleMessage
*/

package com.tenforwardconsulting.cordova;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.LocationService;
import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.cordova.PermissionHelper;
import com.marianhello.logging.LogEntry;
import com.marianhello.logging.LogReader;
import com.marianhello.logging.LoggerManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundGeolocationPlugin extends CordovaPlugin {
    private static final String TAG = BackgroundGeolocationPlugin.class.getSimpleName();

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_LOCATION_ENABLED_CHECK = "isLocationEnabled";
    public static final String ACTION_SHOW_LOCATION_SETTINGS = "showLocationSettings";
    public static final String REGISTER_MODE_CHANGED_RECEIVER = "watchLocationMode";
    public static final String UNREGISTER_MODE_CHANGED_RECEIVER = "stopWatchingLocationMode";
    public static final String ACTION_GET_ALL_LOCATIONS = "getLocations";
    public static final String ACTION_DELETE_LOCATION = "deleteLocation";
    public static final String ACTION_DELETE_ALL_LOCATIONS = "deleteAllLocations";
    public static final String ACTION_GET_CONFIG = "getConfig";
    public static final String ACTION_GET_LOG_ENTRIES = "getLogEntries";

    public static final int START_REQ_CODE = 0;
    public static final int PERMISSION_DENIED_ERROR = 20;
    public static final String[] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    /** Messenger for communicating with the service. */
    private Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    private Boolean mIsBound = false;
    private Boolean isServiceRunning = false;

    private LocationDAO dao;
    private Config config;
    private CallbackContext callbackContext;
    private CallbackContext actionStartCallbackContext;
    private CallbackContext locationModeChangeCallbackContext;
    private ExecutorService executorService;

    private org.slf4j.Logger log;

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationService.MSG_LOCATION_UPDATE:
                    try {
                        log.debug("Sending location update");
                        Bundle bundle = msg.getData();
                        bundle.setClassLoader(LocationService.class.getClassLoader());
                        JSONObject location = ((BackgroundLocation) bundle.getParcelable("location")).toJSONObject();
                        PluginResult result = new PluginResult(PluginResult.Status.OK, location);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    } catch (JSONException e) {
                        log.warn("Error converting message to json");
                        PluginResult result = new PluginResult(PluginResult.Status.JSON_EXCEPTION);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mIsBound = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        LocationService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mIsBound = false;
        }
    };

    private BroadcastReceiver locationModeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        log.debug("Received MODE_CHANGED_ACTION action");
        if (locationModeChangeCallbackContext != null) {
            PluginResult result;
            try {
                int isLocationEnabled = BackgroundGeolocationPlugin.isLocationEnabled(context) ? 1 : 0;
                result = new PluginResult(PluginResult.Status.OK, isLocationEnabled);
                result.setKeepCallback(true);
            } catch (SettingNotFoundException e) {
                result = new PluginResult(PluginResult.Status.ERROR, "Location setting error occured");
            }
            locationModeChangeCallbackContext.sendPluginResult(result);
        }
        }
    };

    @Override
    protected void pluginInitialize() {
        log = LoggerManager.getLogger(BackgroundGeolocationPlugin.class);
        log.debug(TAG, "initializing plugin");

        super.pluginInitialize();
        executorService =  Executors.newSingleThreadExecutor();
        dao = DAOFactory.createLocationDAO(getContext());
    }

    public boolean execute(String action, final JSONArray data, final CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        Context context = activity.getApplicationContext();

        if (ACTION_START.equals(action)) {
            if (!hasPermissions()) {
                actionStartCallbackContext = callbackContext;
                PermissionHelper.requestPermissions(this, START_REQ_CODE, permissions);
                return true;
            }

            executorService.execute(new Runnable() {
                public void run() {
                    if (config == null) {
                        callbackContext.error("Plugin not configured. Please call configure method first.");
                        return;
                    }

                    startAndBindBackgroundService();
                    callbackContext.success();
                }
            });

            return true;
        } else if (ACTION_STOP.equals(action)) {
            executorService.execute(new Runnable() {
                public void run() {
                    doUnbindService();
                    stopBackgroundService();
                    callbackContext.success();
                }
            });

            return true;
        } else if (ACTION_CONFIGURE.equals(action)) {
            this.callbackContext = callbackContext;
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        config = Config.fromJSONArray(data);
                        persistConfiguration(config);
                        log.debug("bg service configured: " + config.toString());
                        // callbackContext.success(); //we cannot do this
                    } catch (JSONException e) {
                        callbackContext.error("Configuration error: " + e.getMessage());
                    } catch (NullPointerException e) {
                        callbackContext.error("Configuration error: " + e.getMessage());
                    }

                    if (config != null) {
                        if (config.isDebugging()) {
                            LoggerManager.enableDBLogging();
                        } else {
                            LoggerManager.disableDBLogging();
                        }
                    }
                }
            });

            return true;
        } else if (ACTION_LOCATION_ENABLED_CHECK.equals(action)) {
            log.debug("location services enabled check");
            try {
                int isLocationEnabled = BackgroundGeolocationPlugin.isLocationEnabled(context) ? 1 : 0;
                callbackContext.success(isLocationEnabled);
            } catch (SettingNotFoundException e) {
                callbackContext.error("Location setting error occured");
            }

            return true;
        } else if (ACTION_SHOW_LOCATION_SETTINGS.equals(action)) {
            showLocationSettings();
            // TODO: call success/fail callback

            return true;
        } else if (REGISTER_MODE_CHANGED_RECEIVER.equals(action)) {
            registerLocationModeChangeReceiver(callbackContext);
            // TODO: call success/fail callback

            return true;
        } else if (UNREGISTER_MODE_CHANGED_RECEIVER.equals(action)) {
            unregisterLocationModeChangeReceiver();
            // TODO: call success/fail callback

            return true;
        } else if (ACTION_GET_ALL_LOCATIONS.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        callbackContext.success(getAllLocations());
                    } catch (JSONException e) {
                        callbackContext.error("Converting locations to JSON failed.");
                    }
                }
            });

            return true;
        } else if (ACTION_DELETE_LOCATION.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        deleteLocation(data.getLong(0));
                        callbackContext.success();
                    } catch (JSONException e) {
                        callbackContext.error("Configuration error: " + e.getMessage());
                    }
                }
            });

            return true;
        } else if (ACTION_DELETE_ALL_LOCATIONS.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    deleteAllLocations();
                    callbackContext.success();
                }
            });

            return true;
        } else if (ACTION_GET_CONFIG.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        callbackContext.success(retrieveConfiguration());
                    } catch (JSONException e) {
                        callbackContext.error("Configuration error: " + e.getMessage());
                    }
                }
            });

            return true;
        } else if (ACTION_GET_LOG_ENTRIES.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        callbackContext.success(getLogs(data.getInt(0)));
                    } catch (Exception e) {
                        callbackContext.error("Getting logs failed: " + e.getMessage());
                    }
                }
            });

            return true;
        }

        return false;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
     @Override
    public void onDestroy() {
        log.debug("destroying plugin");
        unregisterLocationModeChangeReceiver();
        // Unbind from the service
        doUnbindService();
        if (config.getStopOnTerminate()) {
            stopBackgroundService();
        }
        super.onDestroy();
    }

    public Context getContext() {
        return this.cordova.getActivity().getApplicationContext();
    }

    protected void startAndBindBackgroundService () {
        startBackgroundService();
        doBindService();
    }

    protected void startBackgroundService () {
        if (!isServiceRunning) {
            Activity activity = this.cordova.getActivity();
            Intent locationServiceIntent = new Intent(activity, LocationService.class);
            locationServiceIntent.putExtra("config", config);
            locationServiceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            // start service to keep service running even if no clients are bound to it
            activity.startService(locationServiceIntent);
            isServiceRunning = true;
        }
    }

    protected void stopBackgroundService() {
        if (isServiceRunning) {
            log.debug("Stopping bg service");
            Activity activity = this.cordova.getActivity();
            activity.stopService(new Intent(activity, LocationService.class));
            isServiceRunning = false;
        }
    }

    void doBindService () {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        if (!mIsBound) {
            Activity activity = this.cordova.getActivity();
            Intent locationServiceIntent = new Intent(activity, LocationService.class);
            locationServiceIntent.putExtra("config", config);
            activity.bindService(locationServiceIntent, mConnection, Context.BIND_IMPORTANT);
        }
    }

    void doUnbindService () {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            LocationService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }

                // Detach our existing connection.
                Activity activity = this.cordova.getActivity();
                activity.unbindService(mConnection);
                mIsBound = false;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public Intent registerLocationModeChangeReceiver (CallbackContext callbackContext) {
        if (locationModeChangeCallbackContext != null) {
            unregisterLocationModeChangeReceiver();
        }
        locationModeChangeCallbackContext = callbackContext;
        return getContext().registerReceiver(locationModeChangeReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
    }

    public void unregisterLocationModeChangeReceiver () {
        if (locationModeChangeCallbackContext == null) { return; }

        getContext().unregisterReceiver(locationModeChangeReceiver);
        locationModeChangeCallbackContext = null;
    }

    public void showLocationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        cordova.getActivity().startActivity(settingsIntent);
    }

    public static boolean isLocationEnabled(Context context) throws SettingNotFoundException {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    public JSONArray getAllLocations() throws JSONException {
        JSONArray jsonLocationsArray = new JSONArray();
        Collection<BackgroundLocation> locations = dao.getAllLocations();
        for (BackgroundLocation location : locations) {
            jsonLocationsArray.put(location.toJSONObject());
        }
        return jsonLocationsArray;
    }

    public void deleteLocation(Long locationId) {
        dao.deleteLocation(locationId);
    }

    public void deleteAllLocations() {
        dao.deleteAllLocations();
    }

    public void persistConfiguration(Config config) throws NullPointerException {
        Context context = this.cordova.getActivity().getApplicationContext();
        ConfigurationDAO dao = DAOFactory.createConfigurationDAO(context);

        dao.persistConfiguration(config);
    }

    public JSONObject retrieveConfiguration() throws JSONException {
        Context context = this.cordova.getActivity().getApplicationContext();
        ConfigurationDAO dao = DAOFactory.createConfigurationDAO(context);
        Config config = dao.retrieveConfiguration();
        if (config != null) {
            return config.toJSONObject();
        }
        return null;
    }

    public JSONArray getLogs(Integer limit) throws Exception {
        JSONArray jsonLogsArray = new JSONArray();
        LogReader logReader = new LogReader();
        Collection<LogEntry> logEntries = logReader.getEntries(limit);
        for (LogEntry logEntry : logEntries) {
            jsonLogsArray.put(logEntry.toJSONObject());
        }
        return jsonLogsArray;
    }

    public boolean hasPermissions() {
        for(String p : permissions) {
            if(!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
        int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                log.debug("Permission Denied!");
                actionStartCallbackContext.error(PERMISSION_DENIED_ERROR);
                actionStartCallbackContext = null;
                return;
            }
        }
        switch (requestCode) {
            case START_REQ_CODE:
                startAndBindBackgroundService();
                break;
        }
    }
}

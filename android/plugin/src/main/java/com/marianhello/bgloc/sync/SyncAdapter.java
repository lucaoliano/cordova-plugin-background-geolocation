package com.marianhello.bgloc.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.HttpPostService;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.logging.LoggerManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    ContentResolver mContentResolver;
    private LocationDAO locationDAO;
    private ConfigurationDAO configDAO;

    private org.slf4j.Logger log;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        log = LoggerManager.getLogger(SyncAdapter.class);

        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        configDAO = DAOFactory.createConfigurationDAO(context);
        locationDAO = DAOFactory.createLocationDAO(context);
    }


    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        log = LoggerManager.getLogger(SyncAdapter.class);

        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        configDAO = DAOFactory.createConfigurationDAO(context);
        locationDAO = DAOFactory.createLocationDAO(context);

    }

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        Config config;
        try {
            config = configDAO.retrieveConfiguration();
            log.debug("Will perform sync: {}", config.toString());
            if (config.hasUrl() || config.hasSyncUrl()) {
                Long locationsCount = locationDAO.getValidLocationsCount();
                Integer syncThreshold = config.getSyncThreshold();
                if (locationsCount >= syncThreshold) {
                    log.info("Performing sync locations: {}", locationsCount);
                    try {
                        JSONArray locations = locationDAO.getLocationsForSync();
                        String url = config.hasSyncUrl() ? config.getSyncUrl() : config.getUrl();
                        uploadLocations(locations, url, config.getHttpHeaders());
                    } catch (JSONException e) {
                        log.error("Error getting locations for sync: {}", e.getMessage());
                    }
                } else {
                    log.debug("Skipping locations sync locations: {} threshold: {}", locationsCount, syncThreshold);
                }
            }
        } catch (JSONException e) {
            log.error("Error retrieving config: {}", e.getMessage());
        }
    }

    private void uploadLocations(JSONArray locations, String url, HashMap httpHeaders) {
        try {
            Integer responseCode = HttpPostService.postJSON(url, locations, httpHeaders);
            log.info("Locations has been synced with responseCode: {}", responseCode);
        } catch (Throwable e) {
            log.warn("Error while syncing locations: {}", e.getMessage());
        }
    }
}

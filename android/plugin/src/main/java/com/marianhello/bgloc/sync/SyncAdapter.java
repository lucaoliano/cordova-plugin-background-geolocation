package com.marianhello.bgloc.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Pair;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.HttpPostService;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.logging.LoggerManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    ContentResolver mContentResolver;
    private ConfigurationDAO configDAO;
    private BatchStore store;

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
        store = new BatchStore(context);
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
        store = new BatchStore(context);

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

        try {
            Config config = configDAO.retrieveConfiguration();
            log.debug("Sync request: {}", config.toString());
            if (config.hasUrl() || config.hasSyncUrl()) {
                Pair<String, JSONArray> pair = store.peek();
                if (pair != null) {
                    JSONArray locations = pair.second;
                    log.info("Performing sync {} locations: {}", pair.first, locations.length());
                    try {
                        String url = config.hasSyncUrl() ? config.getSyncUrl() : config.getUrl();
                        int responseCode = uploadLocations(locations, url, config.getHttpHeaders());
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            log.info("Locations has been synced successfully");
                            store.remove(pair.first);
                        } else {
                            syncResult.stats.numIoExceptions++;;
                            log.warn("Error while syncing locations. Server responseCode: {}", responseCode);
                        }
                    } catch (IOException e) {
                        log.warn("Error while syncing locations: {}", e.getMessage());
                        syncResult.stats.numIoExceptions++;
                    }
                }
            }
        } catch (JSONException e) {
            log.error("Error retrieving config: {}", e.getMessage());
        }
    }

    private int uploadLocations(JSONArray locations, String url, HashMap httpHeaders) throws IOException {
        return HttpPostService.postJSON(url, locations, httpHeaders);
    }
}

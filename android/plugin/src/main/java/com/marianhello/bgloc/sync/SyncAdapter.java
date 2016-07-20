package com.marianhello.bgloc.sync;

import android.accounts.Account;
import android.app.NotificationManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.JsonWriter;
import android.util.Log;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.HttpPostService;
import com.marianhello.bgloc.UploadingCallback;
import com.marianhello.bgloc.data.ConfigurationDAO;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.sqlite.LocationContract;
import com.marianhello.bgloc.data.sqlite.SQLiteOpenHelper;
import com.marianhello.logging.LoggerManager;

import org.json.JSONException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter implements UploadingCallback {

    private static final int NOTIFICATION_ID = 1;

    /** Directory where to store location batches */
    public static final String SYNC_DIRECTORY = "sync";

    ContentResolver contentResolver;
    private ConfigurationDAO configDAO;
    private NotificationManager notifyManager;

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
        contentResolver = context.getContentResolver();
        configDAO = DAOFactory.createConfigurationDAO(context);
        notifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
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
        contentResolver = context.getContentResolver();
        configDAO = DAOFactory.createConfigurationDAO(context);
        notifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
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

                File file = getBatchForSync();
                if (file == null) {
                    file = createBatch();
                    if (file == null) {
                        log.info("Nothing to sync");
                        return;
                    }
                }

                log.info("Syncing file: {}", file.getName());
                String url = config.hasSyncUrl() ? config.getSyncUrl() : config.getUrl();
                HashMap<String, String> httpHeaders = new HashMap<String, String>();
                httpHeaders.putAll(config.getHttpHeaders());
                httpHeaders.put("x-batch-filename", file.getName());

                if (uploadLocations(file, url, httpHeaders)) {
                    log.info("Batch synced successfully");

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
                    builder.setContentTitle("Syncing locations");
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info);

                    builder.setContentText("Sync completed");
                    builder.setProgress(100, 0, false);
                    // Issues the notification
                    notifyManager.notify(NOTIFICATION_ID, builder.build());

                    if (file.delete()) {
                        log.info("Batch file has been deleted: {}", file.getAbsolutePath());
                    } else {
                        log.warn("Batch file has not been deleted: {}", file.getAbsolutePath());
                    }
                } else {
                    syncResult.stats.numIoExceptions++;

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
                    builder.setContentTitle("Syncing locations");
                    builder.setSmallIcon(android.R.drawable.ic_dialog_info);

                    builder.setContentText("Sync failed");
                    builder.setProgress(100, 0, false);
                    // Issues the notification
                    notifyManager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        } catch (JSONException e) {
            log.error("Error retrieving config: {}", e.getMessage());
        }
    }

    public File getBatchForSync() {
        File batch = null;
        File directory = this.getContext().getDir(SYNC_DIRECTORY, Context.MODE_PRIVATE);
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });

        long lastModified = Long.MAX_VALUE;
        for (File file : files) {
            if (file.lastModified() < lastModified) {
                batch = file;
                lastModified = file.lastModified();
            }
        }

        if (batch == null) {
            return null;
        }

        return batch;
    }

    private boolean uploadLocations(File file, String url, HashMap httpHeaders) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setContentTitle("Syncing locations");
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);

        builder.setContentText("Sync in progress");
        builder.setProgress(100, 0, true);
        // Issues the notification
        notifyManager.notify(NOTIFICATION_ID, builder.build());

        try {
            int responseCode = HttpPostService.postJSON(url, file, httpHeaders, this);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (IOException e) {
            log.warn("Error uploading locations: {}", e.getMessage());
        }

        return false;
    }

    public void uploadListener(int progress) {
        log.debug("Sync progress: {}", progress);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setContentTitle("Syncing locations");
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);

        builder.setContentText("Sync in progress");
        builder.setProgress(100, progress, true);
        // Issues the notification
        notifyManager.notify(NOTIFICATION_ID, builder.build());
    }

    public File createBatch() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = dateFormat.format(c.getTime());

        String filename = "locations_" + formattedDate + ".json";
        File directory = getContext().getDir(SYNC_DIRECTORY, Context.MODE_PRIVATE);
        File file = new File(directory, filename);

        SQLiteOpenHelper helper = SQLiteOpenHelper.getHelper(getContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        String[] columns = {
                LocationContract.LocationEntry._ID,
                LocationContract.LocationEntry.COLUMN_NAME_TIME,
                LocationContract.LocationEntry.COLUMN_NAME_ACCURACY,
                LocationContract.LocationEntry.COLUMN_NAME_SPEED,
                LocationContract.LocationEntry.COLUMN_NAME_BEARING,
                LocationContract.LocationEntry.COLUMN_NAME_ALTITUDE,
                LocationContract.LocationEntry.COLUMN_NAME_LATITUDE,
                LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE,
                LocationContract.LocationEntry.COLUMN_NAME_PROVIDER,
                LocationContract.LocationEntry.COLUMN_NAME_LOCATION_PROVIDER
        };

        String groupBy = null;
        String having = null;
        String orderBy = LocationContract.LocationEntry.COLUMN_NAME_TIME + " ASC";
        Cursor cursor = null;

        String whereClause = LocationContract.LocationEntry.COLUMN_NAME_VALID + " = ?";
        String[] whereArgs = { "1" };

        JsonWriter writer = null;
        try {
//            file.createNewFile();
            db.beginTransactionNonExclusive();

            FileOutputStream fs = new FileOutputStream(file);
            writer = new JsonWriter(new OutputStreamWriter(fs, "UTF-8"));

            cursor = db.query(
                    LocationContract.LocationEntry.TABLE_NAME,  // The table to query
                    columns,                   // The columns to return
                    whereClause,               // The columns for the WHERE clause
                    whereArgs,                 // The values for the WHERE clause
                    groupBy,                   // don't group the rows
                    having,                    // don't filter by row groups
                    orderBy                    // The sort order
            );
            writer.beginArray();
            while (cursor.moveToNext()) {
                writer.beginObject();
                writer.name("time").value(cursor.getLong(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_TIME)));
                writer.name("latitude").value(cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LATITUDE)));
                writer.name("longitude").value(cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LONGITUDE)));
                writer.name("accuracy").value(cursor.getFloat(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_ACCURACY)));
                writer.name("speed").value(cursor.getFloat(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_SPEED)));
                writer.name("altitude").value(cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_ALTITUDE)));
                writer.name("bearing").value(cursor.getFloat(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_BEARING)));
                writer.name("provider").value(cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_PROVIDER)));
                writer.name("locationProvider").value(cursor.getInt(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_LOCATION_PROVIDER)));
                writer.endObject();
            }
            writer.endArray();
            writer.close();
            fs.close();

            db.setTransactionSuccessful();

            return file;
        } catch (Exception e) {
            log.error("Failed to create sync batch: {}", e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //noop
                }
            }
            db.endTransaction();
        }

        return null;
    }
}

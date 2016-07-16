package com.marianhello.bgloc.data.sqlite;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.marianhello.bgloc.data.sqlite.LocationContract.LocationEntry;
import com.marianhello.bgloc.data.sqlite.ConfigurationContract.ConfigurationEntry;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class SQLiteOpenHelper extends android.database.sqlite.SQLiteOpenHelper {
    public static final String SQLITE_DATABASE_NAME = "cordova_bg_geolocation.db";
    public static final int DATABASE_VERSION = 11;
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_LOCATION_TABLE =
        "CREATE TABLE " + LocationEntry.TABLE_NAME + " (" +
        LocationEntry._ID + " INTEGER PRIMARY KEY," +
        LocationEntry.COLUMN_NAME_TIME + INTEGER_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_ACCURACY + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_SPEED + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_BEARING + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_ALTITUDE + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_LATITUDE + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_PROVIDER + TEXT_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_LOCATION_PROVIDER + INTEGER_TYPE + COMMA_SEP +
        LocationEntry.COLUMN_NAME_VALID + INTEGER_TYPE +
        " )";

    private static final String SQL_CREATE_CONFIG_TABLE =
        "CREATE TABLE " + ConfigurationEntry.TABLE_NAME + " (" +
        ConfigurationEntry._ID + " INTEGER PRIMARY KEY," +
        ConfigurationEntry.COLUMN_NAME_RADIUS + REAL_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_DISTANCE_FILTER + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_DESIRED_ACCURACY + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_DEBUG + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_NOTIF_TITLE + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_NOTIF_TEXT + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_SMALL + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_NOTIF_ICON_LARGE + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_NOTIF_COLOR + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_STOP_TERMINATE + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_STOP_ON_STILL + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_START_BOOT + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_START_FOREGROUND + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_LOCATION_PROVIDER + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_INTERVAL + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_FASTEST_INTERVAL + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_ACTIVITIES_INTERVAL + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_SYNC_URL + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_SYNC_THRESHOLD + INTEGER_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_HEADERS + TEXT_TYPE + COMMA_SEP +
        ConfigurationEntry.COLUMN_NAME_MAX_LOCATIONS + INTEGER_TYPE +
        " )";

    private static final String SQL_DROP_CONFIG_TABLE =
            "DROP TABLE IF EXISTS " + ConfigurationEntry.TABLE_NAME;

    private static final String SQL_DROP_LOCATION_TABLE =
            "DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME;

    private static final String SQL_CREATE_LOCATION_TABLE_TIME_IDX =
            "CREATE INDEX time_idx ON " + LocationEntry.TABLE_NAME + " (" + LocationEntry.COLUMN_NAME_TIME + ")";

    private static SQLiteOpenHelper instance;

    /**
     * Get SqliteOpenHelper instance (singleton)
     *
     * Use the application context, which will ensure that you
     * don't accidentally leak an Activity's context.
     * See this article for more information: http://bit.ly/6LRzfx
     *
     * @param context
     * @return
     */
    public static synchronized SQLiteOpenHelper getHelper(Context context) {
        if (instance == null)
            instance = new SQLiteOpenHelper(context);

        return instance;
    }

    /**
     * Constructor
     *
     * NOTE: Intended to use only for testing purposes.
     * Use factory method getHelper instead.
     *
     * @param context
     */
    public SQLiteOpenHelper(Context context) {
        super(context, SQLITE_DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(this.getClass().getName(), "Creating db: " + this.getDatabaseName());
        db.execSQL(SQL_CREATE_LOCATION_TABLE);
        Log.d(this.getClass().getName(), SQL_CREATE_LOCATION_TABLE);
        db.execSQL(SQL_CREATE_CONFIG_TABLE);
        Log.d(this.getClass().getName(), SQL_CREATE_CONFIG_TABLE);
        db.execSQL(SQL_CREATE_LOCATION_TABLE_TIME_IDX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 10) {
            Log.d(this.getClass().getName(), "Upgrading database oldVersion: " + oldVersion + " newVersion: " + newVersion);
            String alterSql[] = {
                    "ALTER TABLE " + LocationEntry.TABLE_NAME +
                            " ADD COLUMN " + LocationEntry.COLUMN_NAME_VALID + INTEGER_TYPE,
                    SQL_CREATE_LOCATION_TABLE_TIME_IDX,
                    SQL_DROP_CONFIG_TABLE,
                    SQL_CREATE_CONFIG_TABLE
            };

            for (String sql : alterSql) {
                db.execSQL(sql);
            }
        } else {
            // for all other scenarios drop table and start over
            db.execSQL(SQL_DROP_LOCATION_TABLE);
            Log.d(this.getClass().getName(), SQL_DROP_LOCATION_TABLE);
            db.execSQL(SQL_DROP_CONFIG_TABLE);
            Log.d(this.getClass().getName(), SQL_DROP_CONFIG_TABLE);
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // we don't support db downgrade yet, instead we drop table and start over
        db.execSQL(SQL_DROP_LOCATION_TABLE);
        Log.d(this.getClass().getName(), SQL_DROP_LOCATION_TABLE);
        db.execSQL(SQL_DROP_CONFIG_TABLE);
        Log.d(this.getClass().getName(), SQL_DROP_CONFIG_TABLE);
        onCreate(db);
    }
}

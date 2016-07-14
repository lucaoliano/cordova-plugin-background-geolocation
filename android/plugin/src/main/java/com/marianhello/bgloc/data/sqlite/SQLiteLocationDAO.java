package com.marianhello.bgloc.data.sqlite;

import java.util.ArrayList;
import java.util.Collection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.marianhello.bgloc.data.LocationDAO;
import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.bgloc.data.sqlite.LocationContract.LocationEntry;

public class SQLiteLocationDAO implements LocationDAO {
  private static final String TAG = SQLiteLocationDAO.class.getSimpleName();

  private SQLiteDatabase db;

  public SQLiteLocationDAO(Context context) {
    SQLiteOpenHelper helper = SQLiteOpenHelper.getHelper(context);
    this.db = helper.getWritableDatabase();
  }

  public SQLiteLocationDAO(SQLiteDatabase db) {
    this.db = db;
  }

  public long getLastInsertRowId(SQLiteDatabase db) {
    Cursor cur = db.rawQuery("SELECT last_insert_rowid()", null);
    cur.moveToFirst();
    long id = cur.getLong(0);
    cur.close();
    return id;
  }

  /**
   * Get all locations that match whereClause
   *
   * @param whereClause
   * @param whereArgs
   * @return collection of locations
     */
  private Collection<BackgroundLocation> getLocations(String whereClause, String[] whereArgs) {
    Collection<BackgroundLocation> locations = new ArrayList<BackgroundLocation>();

    String[] columns = {
      LocationEntry._ID,
      LocationEntry.COLUMN_NAME_TIME,
      LocationEntry.COLUMN_NAME_ACCURACY,
      LocationEntry.COLUMN_NAME_SPEED,
      LocationEntry.COLUMN_NAME_BEARING,
      LocationEntry.COLUMN_NAME_ALTITUDE,
      LocationEntry.COLUMN_NAME_LATITUDE,
      LocationEntry.COLUMN_NAME_LONGITUDE,
      LocationEntry.COLUMN_NAME_PROVIDER,
      LocationEntry.COLUMN_NAME_LOCATION_PROVIDER,
      LocationEntry.COLUMN_NAME_DEBUG
    };

    String groupBy = null;
    String having = null;
    String orderBy = LocationEntry.COLUMN_NAME_TIME + " ASC";
    Cursor cursor = null;

    try {
      cursor = db.query(
          LocationEntry.TABLE_NAME,  // The table to query
          columns,                   // The columns to return
          whereClause,               // The columns for the WHERE clause
          whereArgs,                 // The values for the WHERE clause
          groupBy,                   // don't group the rows
          having,                    // don't filter by row groups
          orderBy                    // The sort order
      );
      while (cursor.moveToNext()) {
        locations.add(hydrate(cursor));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return locations;
  }

  public Collection<BackgroundLocation> getAllLocations() {
    return getLocations(null, null);
  }

  public Collection<BackgroundLocation> getValidLocations() {
    String whereClause = LocationEntry.COLUMN_NAME_VALID + " = ?";
    String[] whereArgs = { "1" };

    return getLocations(whereClause, whereArgs);
  }

  public Collection<BackgroundLocation> getLocationsForSync() {
    Collection<BackgroundLocation> locations = null;
    String whereClause = LocationEntry.COLUMN_NAME_VALID + " = ?";
    String[] whereArgs = { "1" };

    db.beginTransactionNonExclusive();
    locations = getLocations(whereClause, whereArgs);

    ContentValues values = new ContentValues();
    values.put(LocationEntry.COLUMN_NAME_VALID, 0);
    db.update(LocationEntry.TABLE_NAME, values, null, null);
    db.setTransactionSuccessful();
    db.endTransaction();

    return locations;
  }

  public Long getLocationsCount() {
    return DatabaseUtils.queryNumEntries(db, LocationEntry.TABLE_NAME);
  }

  /**
   * Persist location into database
   *
   * @param location
   * @return rowId or -1 when error occured
   */
  public Long persistLocation(BackgroundLocation location) {
    ContentValues values = getContentValues(location);
    long rowId = db.insert(LocationEntry.TABLE_NAME, LocationEntry.COLUMN_NAME_NULLABLE, values);
    Log.d(TAG, "Location persisted with id=" + rowId);
    return rowId;
  }

  /**
   * Persist location into database with maximum row limit
   *
   * Method will ensure that there will be no more records than maxRows.
   * Instead old records will be replaced with newer ones.
   * If maxRows will change in time, method will delete excess records and vacuum table.
   *
   * @param location
   * @param maxRows
   * @return rowId or -1 when error occured
   */
  public Long persistLocationWithLimit(BackgroundLocation location, Integer maxRows) {
    Long rowId = null;
    String sql = null;
    Boolean shouldVacuum = false;

    long rowCount = DatabaseUtils.queryNumEntries(db, LocationEntry.TABLE_NAME);

    if (rowCount < maxRows) {
      ContentValues values = getContentValues(location);
      rowId = db.insert(LocationEntry.TABLE_NAME, LocationEntry.COLUMN_NAME_NULLABLE, values);
      Log.d(TAG, "Location persisted with id=" + rowId);

      return rowId;
    }

    db.beginTransactionNonExclusive();

    if (rowCount > maxRows) {
      sql = new StringBuilder("DELETE FROM ")
              .append(LocationEntry.TABLE_NAME)
              .append(" WHERE ").append(LocationEntry._ID)
              .append(" IN (SELECT ").append(LocationEntry._ID)
              .append(" FROM ").append(LocationEntry.TABLE_NAME)
              .append(" ORDER BY ").append(LocationEntry.COLUMN_NAME_TIME)
              .append(" LIMIT ?)")
              .toString();
      db.execSQL(sql, new Object[] {(rowCount - maxRows)});
      shouldVacuum = true;
    }

    sql = new StringBuilder("UPDATE ")
            .append(LocationEntry.TABLE_NAME).append(" SET ")
            .append(LocationEntry.COLUMN_NAME_ACCURACY).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_ALTITUDE).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_BEARING).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_LATITUDE).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_LONGITUDE).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_SPEED).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_TIME).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_PROVIDER).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_LOCATION_PROVIDER).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_DEBUG).append("= ?,")
            .append(LocationEntry.COLUMN_NAME_VALID).append("= 1")
            .append(" WHERE ").append(LocationEntry.COLUMN_NAME_TIME)
            .append("= (SELECT min(").append(LocationEntry.COLUMN_NAME_TIME).append(") FROM ")
            .append(LocationEntry.TABLE_NAME).append(")")
            .toString();
    db.execSQL(sql, new Object[] {
            location.getAccuracy(),
            location.getAltitude(),
            location.getBearing(),
            location.getLatitude(),
            location.getLongitude(),
            location.getSpeed(),
            location.getTime(),
            location.getProvider(),
            location.getLocationProvider(),
            location.getDebug() ? "1" : "0"
    });

    rowId = getLastInsertRowId(db);
    db.setTransactionSuccessful();
    db.endTransaction();

    if (shouldVacuum) { db.execSQL("VACUUM"); }

    return rowId;
  }

  /**
   * Delete location by given locationId
   *
   * Note: location is not actually deleted only flagged as non valid
   * @param locationId
   */
  public void deleteLocation(Long locationId) {
    ContentValues values = new ContentValues();
    values.put(LocationEntry.COLUMN_NAME_VALID, 0);

    String whereClause = LocationEntry._ID + " = ?";
    String[] whereArgs = { String.valueOf(locationId) };

    db.update(LocationEntry.TABLE_NAME, values, whereClause, whereArgs);
  }

  /**
   * Delete all locations
   *
   * Note: location are not actually deleted only flagged as non valid
   */
  public void deleteAllLocations() {
    ContentValues values = new ContentValues();
    values.put(LocationEntry.COLUMN_NAME_VALID, 0);

    db.update(LocationEntry.TABLE_NAME, values, null, null);
  }

  private BackgroundLocation hydrate(Cursor c) {
    BackgroundLocation l = new BackgroundLocation(c.getString(c.getColumnIndex(LocationEntry.COLUMN_NAME_PROVIDER)));
    l.setLocationId(c.getLong(c.getColumnIndex(LocationEntry._ID)));
    l.setTime(c.getLong(c.getColumnIndex(LocationEntry.COLUMN_NAME_TIME)));
    l.setAccuracy(c.getFloat(c.getColumnIndex(LocationEntry.COLUMN_NAME_ACCURACY)));
    l.setSpeed(c.getFloat(c.getColumnIndex(LocationEntry.COLUMN_NAME_SPEED)));
    l.setBearing(c.getFloat(c.getColumnIndex(LocationEntry.COLUMN_NAME_BEARING)));
    l.setAltitude(c.getDouble(c.getColumnIndex(LocationEntry.COLUMN_NAME_ALTITUDE)));
    l.setLatitude(c.getDouble(c.getColumnIndex(LocationEntry.COLUMN_NAME_LATITUDE)));
    l.setLongitude(c.getDouble(c.getColumnIndex(LocationEntry.COLUMN_NAME_LONGITUDE)));
    l.setLocationProvider(c.getInt(c.getColumnIndex(LocationEntry.COLUMN_NAME_LOCATION_PROVIDER)));
    l.setDebug( (c.getInt(c.getColumnIndex(LocationEntry.COLUMN_NAME_DEBUG)) == 1) ? true : false);

    return l;
  }

  private ContentValues getContentValues(BackgroundLocation location) {
    ContentValues values = new ContentValues();
    values.put(LocationEntry.COLUMN_NAME_TIME, location.getTime());
    values.put(LocationEntry.COLUMN_NAME_ACCURACY, location.getAccuracy());
    values.put(LocationEntry.COLUMN_NAME_SPEED, location.getSpeed());
    values.put(LocationEntry.COLUMN_NAME_BEARING, location.getBearing());
    values.put(LocationEntry.COLUMN_NAME_ALTITUDE, location.getAltitude());
    values.put(LocationEntry.COLUMN_NAME_LATITUDE, location.getLatitude());
    values.put(LocationEntry.COLUMN_NAME_LONGITUDE, location.getLongitude());
    values.put(LocationEntry.COLUMN_NAME_PROVIDER, location.getProvider());
    values.put(LocationEntry.COLUMN_NAME_LOCATION_PROVIDER, location.getLocationProvider());
    values.put(LocationEntry.COLUMN_NAME_DEBUG, (location.getDebug() == true) ? 1 : 0);
    values.put(LocationEntry.COLUMN_NAME_VALID, 1);

    return values;
  }
}

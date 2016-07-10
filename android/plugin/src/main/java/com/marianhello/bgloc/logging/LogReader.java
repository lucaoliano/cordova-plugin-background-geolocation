package com.marianhello.bgloc.logging;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.db.SQLBuilder;
import ch.qos.logback.classic.db.names.ColumnName;
import ch.qos.logback.classic.db.names.DefaultDBNameResolver;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.CommonPathUtil;

/**
 * Created by finch on 09/07/16.
 */
public class LogReader {

    private DefaultDBNameResolver dbNameResolver;
    private SQLiteDatabase db;

    public static Collection<LogEntry> getEntries(Integer limit) throws SQLException {
        LogReader reader = new LogReader();
        return reader._getEntries(limit);
    }

    private Collection<LogEntry> _getEntries(Integer limit) throws SQLException {
        Collection<LogEntry> entries = null;

        String packageName = null;
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();

        if (context != null) {
            packageName = context.getProperty(CoreConstants.PACKAGE_NAME_KEY);
        }

        if (packageName == null || packageName.length() == 0) {
            throw new SQLException("Cannot open database without package name");
        }

        boolean dbOpened = false;
        try {
            File dbfile = new File(CommonPathUtil.getDatabaseDirectoryPath(packageName), "logback.db");
            db = SQLiteDatabase.openDatabase(dbfile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            dbOpened = true;
        } catch (SQLiteException e) {
            throw new SQLException("Cannot open database", e);
        }

        Cursor cursor = null;
        if (dbOpened) {
            try {
                if (dbNameResolver == null) {
                    dbNameResolver = new DefaultDBNameResolver();
                }
                String sql = com.marianhello.bgloc.logging.SQLBuilder.buildSelectSQL(dbNameResolver);
                cursor = db.rawQuery(sql, new String[] { String.valueOf(limit) });
                while (cursor.moveToNext()) {
                    entries.add(hydrate(cursor));
                }
            } catch (SQLiteException e) {
                throw new SQLException("Cannot retrieve log entries", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }
        }

        return entries;
    }

    private LogEntry hydrate(Cursor c) {
        LogEntry entry = null;
        entry.setContext(0);
        entry.setLevel(c.getString(c.getColumnIndex(dbNameResolver.getColumnName(ColumnName.LEVEL_STRING))));
        entry.setMessage(c.getString(c.getColumnIndex(dbNameResolver.getColumnName(ColumnName.FORMATTED_MESSAGE))));
        entry.setTimestamp(c.getLong(c.getColumnIndex(dbNameResolver.getColumnName(ColumnName.TIMESTMP))));

        return entry;
    }
}

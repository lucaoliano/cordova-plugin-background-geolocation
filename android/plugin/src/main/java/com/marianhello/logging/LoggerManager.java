package com.marianhello.logging;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.android.SQLiteAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Created by finch on 08/07/16.
 */
public class LoggerManager {

    public static final String SQLITE_APPENDER_NAME = "sqlite";

    static {
//        BasicLogcatConfigurator.configureDefaultContext();

        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        context.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg");
        encoder.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setEncoder(encoder);
        logcatAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.TRACE);
        root.addAppender(logcatAppender);
    }

    public static void enableDBLogging() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (root.getAppender(SQLITE_APPENDER_NAME) == null) {
            LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            SQLiteAppender sqliteAppender = new SQLiteAppender();
            sqliteAppender.setName(SQLITE_APPENDER_NAME);
            sqliteAppender.setContext(context);
            sqliteAppender.start();
            root.addAppender(sqliteAppender);
        }
    }

    public static void disableDBLogging() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> sqliteAppender = root.getAppender(SQLITE_APPENDER_NAME);
        if (sqliteAppender != null) {
            sqliteAppender.stop();
            root.detachAppender(sqliteAppender);
        }
    }

    public static org.slf4j.Logger getLogger(Class forClass) {
        return org.slf4j.LoggerFactory.getLogger(forClass);
    }
}

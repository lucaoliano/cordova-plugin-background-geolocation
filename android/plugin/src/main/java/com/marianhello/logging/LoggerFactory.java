package com.marianhello.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.android.SQLiteAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

/**
 * Created by finch on 08/07/16.
 */
public class LoggerFactory {
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

        SQLiteAppender sqliteAppender = new SQLiteAppender();
        sqliteAppender.setContext(context);
        sqliteAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.TRACE);
        root.addAppender(logcatAppender);
        root.addAppender(sqliteAppender);
    }

    public static org.slf4j.Logger getLogger(Class forClass) {
        return org.slf4j.LoggerFactory.getLogger(forClass);
    }
}

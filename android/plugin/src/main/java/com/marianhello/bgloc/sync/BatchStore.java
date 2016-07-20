package com.marianhello.bgloc.sync;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by finch on 18/07/16.
 */
public class BatchStore {

    private static final String TAG = BatchStore.class.getName();

    public static final String SYNC_DIRECTORY = "sync";
    private Context context;

    public BatchStore(Context context) {
        this.context = context;
    }

    public boolean push(JSONArray data) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = dateFormat.format(c.getTime());

        String filename = "locations_" + formattedDate + ".json";
        File directory = context.getDir(SYNC_DIRECTORY, Context.MODE_PRIVATE);
        File file = new File(directory, filename);

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(data.toString(0));
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public Pair<String, JSONArray> peek() {
        File batch = null;
        File directory = context.getDir(SYNC_DIRECTORY, Context.MODE_PRIVATE);
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

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(batch));

            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            return new Pair(batch.getName(), new JSONArray(sb.toString()));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Peek failed: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Peek failed: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Peek failed: " + e.getMessage());
        }

        return null;
    }

    public boolean remove(String filename) {
        Log.d(TAG, "Will delete file: " + filename);
        File file = new File(SYNC_DIRECTORY, filename);
        Log.d(TAG, "Delete file: " + file.toString());
        return file.delete();
    }
}

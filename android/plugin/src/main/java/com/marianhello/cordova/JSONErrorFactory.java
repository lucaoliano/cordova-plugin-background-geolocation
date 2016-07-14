package com.marianhello.cordova;

import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by finch on 13/07/16.
 */
public class JSONErrorFactory {

    public static JSONObject getJSONError(Integer errorCode, String errorMessage) {
        JSONObject message = new JSONObject();
        try {
            message.put("code", errorCode);
            message.put("message", errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return message;
    }
}

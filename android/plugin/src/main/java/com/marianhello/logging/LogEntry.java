package com.marianhello.logging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by finch on 10/07/16.
 */
public class LogEntry {
    private Integer context;
    private String level;
    private String message;
    private Long timestamp;

    public Integer getContext() {
        return context;
    }

    public void setContext(Integer context) {
        this.context = context;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("context", this.context);
        json.put("level", this.level);
        json.put("message", this.message);
        json.put("timestamp", this.timestamp);

        return json;
    }
}

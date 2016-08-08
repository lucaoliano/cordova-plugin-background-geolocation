/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.cordova.bgloc;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Config class
 */
public class Config implements Parcelable
{
    private float stationaryRadius = 50;
    private Integer distanceFilter = 500;
    private Integer locationTimeout = 60;
    private Integer desiredAccuracy = 100;
    private Boolean debugging = false;
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String activityType; //not used
    private Boolean stopOnTerminate = false;
    private String notificationIcon;
    private String notificationIconColor;
    private ServiceProvider serviceProvider = ServiceProvider.ANDROID_DISTANCE_FILTER;
    private Integer interval = 600000; //milliseconds
    private Integer fastestInterval = 120000; //milliseconds
    private Integer activitiesInterval = 1000; //milliseconds
    private Boolean stopOnStillActivity = true;
    private String url;
    private String headers;
    private String params;
    private String oauthUrl;
    private String accessToken;
    private String refreshToken;
    private String clientId;
    private String username;
    private String password;

    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(getStationaryRadius());
        out.writeInt(getDistanceFilter());
        out.writeInt(getLocationTimeout());
        out.writeInt(getDesiredAccuracy());
        out.writeValue(isDebugging());
        out.writeString(getNotificationTitle());
        out.writeString(getNotificationText());
        out.writeString(getActivityType());
        out.writeValue(getStopOnTerminate());
        out.writeString(getNotificationIcon());
        out.writeString(getNotificationIconColor());
        out.writeInt(getServiceProvider().asInt());
        out.writeInt(getInterval());
        out.writeInt(getFastestInterval());
        out.writeInt(getActivitiesInterval());
        out.writeValue(getStopOnStillActivity());
        out.writeString(getUrl());
        out.writeString(getHeaders());
        out.writeString(getParams());
        out.writeString(getOauthUrl());
        out.writeString(getAccessToken());
        out.writeString(getRefreshToken());
        out.writeString(getClientId());
        out.writeString(getUsername());
        out.writeString(getPassword());
    }

    public static final Parcelable.Creator<Config> CREATOR
            = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config () {

    }

    public Config(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        if (jsonObject.has("stationaryRadius") && !jsonObject.isNull("stationaryRadius"))
            setStationaryRadius((float) jsonObject.getDouble("stationaryRadius"));

        if (jsonObject.has("distanceFilter") && !jsonObject.isNull("distanceFilter"))
            setDistanceFilter(jsonObject.getInt("distanceFilter"));

        if (jsonObject.has("locationTimeout") && !jsonObject.isNull("locationTimeout"))
            setLocationTimeout(jsonObject.getInt("locationTimeout"));

        if (jsonObject.has("desiredAccuracy") && !jsonObject.isNull("desiredAccuracy"))
            setDesiredAccuracy(jsonObject.getInt("desiredAccuracy"));

        if (jsonObject.has("debugging") && !jsonObject.isNull("debugging"))
            setDebugging(jsonObject.getBoolean("debugging"));

        if (jsonObject.has("notificationTitle") && !jsonObject.isNull("notificationTitle"))
            setNotificationTitle(jsonObject.getString("notificationTitle"));

        if (jsonObject.has("notificationText") && !jsonObject.isNull("notificationText"))
            setNotificationText(jsonObject.getString("notificationText"));

        if (jsonObject.has("activityType") && !jsonObject.isNull("activityType"))
            setActivityType(jsonObject.getString("activityType"));

        if (jsonObject.has("stopOnTerminate") && !jsonObject.isNull("stopOnTerminate"))
            setStopOnTerminate(jsonObject.getBoolean("stopOnTerminate"));

        if (jsonObject.has("notificationIcon") && !jsonObject.isNull("notificationIcon"))
            setNotificationIcon(jsonObject.getString("notificationIcon"));

        if (jsonObject.has("notificationIconColor") && !jsonObject.isNull("notificationIconColor"))
            setNotificationIconColor(jsonObject.getString("notificationIconColor"));

        if (jsonObject.has("serviceProvider") && !jsonObject.isNull("serviceProvider")) {
            String serviceProvider = jsonObject.getString("serviceProvider");
            setServiceProvider(ServiceProvider.valueOf(serviceProvider));
        }

        if (jsonObject.has("interval") && !jsonObject.isNull("interval"))
            setInterval(jsonObject.getInt("interval"));

        if (jsonObject.has("fastestInterval") && !jsonObject.isNull("fastestInterval"))
            setFastestInterval(jsonObject.getInt("fastestInterval"));

        if (jsonObject.has("activitiesInterval") && !jsonObject.isNull("activitiesInterval"))
            setActivitiesInterval(jsonObject.getInt("activitiesInterval"));
        
        if (jsonObject.has("stopOnStillActivity") && !jsonObject.isNull("stopOnStillActivity"))
            setStopOnStillActivity(jsonObject.getBoolean("stopOnStillActivity"));    

        if (jsonObject.has("url") && !jsonObject.isNull("url"))
            setUrl(jsonObject.getString("url"));

        if (jsonObject.has("headers") && !jsonObject.isNull("headers"))
            setHeaders(jsonObject.getString("headers"));

        if (jsonObject.has("params") && !jsonObject.isNull("params"))
            setParams(jsonObject.getString("params"));

        if (jsonObject.has("oauthUrl") && !jsonObject.isNull("oauthUrl"))
            setOauthUrl(jsonObject.getString("oauthUrl"));
        if (jsonObject.has("accessToken") && !jsonObject.isNull("accessToken"))
            setAccessToken(jsonObject.getString("accessToken"));
        if (jsonObject.has("refreshToken") && !jsonObject.isNull("refreshToken"))
            setRefreshToken(jsonObject.getString("refreshToken"));
        if (jsonObject.has("clientId") && !jsonObject.isNull("clientId"))
            setClientId(jsonObject.getString("clientId"));
        if (jsonObject.has("username") && !jsonObject.isNull("username"))
            setUsername(jsonObject.getString("username"));
        if (jsonObject.has("password") && !jsonObject.isNull("password"))
            setPassword(jsonObject.getString("password"));

    }

    private Config(Parcel in) {
        setStationaryRadius(in.readFloat());
        setDistanceFilter(in.readInt());
        setLocationTimeout(in.readInt());
        setDesiredAccuracy(in.readInt());
        setDebugging((Boolean) in.readValue(null));
        setNotificationTitle(in.readString());
        setNotificationText(in.readString());
        setActivityType(in.readString());
        setStopOnTerminate((Boolean) in.readValue(null));
        setNotificationIcon(in.readString());
        setNotificationIconColor(in.readString());
        setServiceProvider(in.readInt());
        setInterval(in.readInt());
        setFastestInterval(in.readInt());
        setActivitiesInterval(in.readInt());
        setStopOnStillActivity((Boolean) in.readValue(null));
        setUrl(in.readString());
        setHeaders(in.readString());
        setParams(in.readString());
        setOauthUrl(in.readString());
        setAccessToken(in.readString());
        setRefreshToken(in.readString());
        setClientId(in.readString());
        setUsername(in.readString());
        setPassword(in.readString());
    }

    public float getStationaryRadius() {
        return stationaryRadius;
    }

    public void setStationaryRadius(float stationaryRadius) {
        this.stationaryRadius = stationaryRadius;
    }

    public Integer getDesiredAccuracy() {
        return desiredAccuracy;
    }

    public void setDesiredAccuracy(Integer desiredAccuracy) {
        this.desiredAccuracy = desiredAccuracy;
    }

    public Integer getDistanceFilter() {
        return distanceFilter;
    }

    public void setDistanceFilter(Integer distanceFilter) {
        this.distanceFilter = distanceFilter;
    }

    public Integer getLocationTimeout() {
        return locationTimeout;
    }

    public void setLocationTimeout(Integer locationTimeout) {
        this.locationTimeout = locationTimeout;
    }

    public Boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(Boolean debugging) {
        this.debugging = debugging;
    }

    public String getNotificationIconColor() {
        return notificationIconColor;
    }

    public void setNotificationIconColor(String notificationIconColor) {
        if (!"null".equals(notificationIconColor)) {
            this.notificationIconColor = notificationIconColor;
        }
    }

    public String getNotificationIcon() {
        return notificationIcon;
    }

    public void setNotificationIcon(String notificationIcon) {
        if (!"null".equals(notificationIcon)) {
            this.notificationIcon = notificationIcon;
        }
    }

    public String getNotificationTitle() {
        return notificationTitle;
    }

    public void setNotificationTitle(String notificationTitle) {
        this.notificationTitle = notificationTitle;
    }

    public String getNotificationText() {
        return notificationText;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }

    public Boolean getStopOnTerminate() {
        return stopOnTerminate;
    }

    public void setStopOnTerminate(Boolean stopOnTerminate) {
        this.stopOnTerminate = stopOnTerminate;
    }

    public ServiceProvider getServiceProvider() {
        return this.serviceProvider;
    }

    public void setServiceProvider(Integer providerId) {
        this.serviceProvider = ServiceProvider.forInt(providerId);
    }

    public void setServiceProvider(ServiceProvider provider) {
        this.serviceProvider = provider;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getFastestInterval() {
        return fastestInterval;
    }

    public void setFastestInterval(Integer fastestInterval) {
        this.fastestInterval = fastestInterval;
    }

    public Integer getActivitiesInterval() {
        return activitiesInterval;
    }

    public void setActivitiesInterval(Integer activitiesInterval) {
        this.activitiesInterval = activitiesInterval;
    }

    public String getLargeNotificationIcon () {
        String iconName = getNotificationIcon();
        if (iconName != null) {
            iconName = iconName + "_large";
        }
        return iconName;
    }

    public String getSmallNotificationIcon () {
        String iconName = getNotificationIcon();
        if (iconName != null) {
            iconName = iconName + "_small";
        }
        return iconName;
    }

    private void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    private String getActivityType() {
        return activityType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setOauthUrl(String oauthUrl) {
        this.oauthUrl = oauthUrl;
    }

    public String getOauthUrl() {
        return oauthUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getStopOnStillActivity() {
        return stopOnStillActivity;
    }

    public void setStopOnStillActivity(Boolean stopOnStillActivity) {
        this.stopOnStillActivity = stopOnStillActivity;
    }

    @Override
    public String toString() {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("stationaryRadius", getStationaryRadius());
        config.put("desiredAccuracy", getDesiredAccuracy());
        config.put("distanceFilter", getDistanceFilter());
        config.put("locationTimeout", getLocationTimeout());
        config.put("debugging", isDebugging());
        config.put("notificationIcon", getNotificationIcon());
        config.put("notificationIconColor", getNotificationIconColor());
        config.put("notificationTitle", getNotificationTitle());
        config.put("notificationText", getNotificationText());
        config.put("stopOnTerminate", getStopOnTerminate());
        config.put("serviceProvider", getServiceProvider());
        config.put("interval", getInterval());
        config.put("fastestInterval", getFastestInterval());
        config.put("activitiesInterval", getActivitiesInterval());
        config.put("stopOnStillActivity", getStopOnStillActivity());
        config.put("url", getUrl());
        config.put("headers", getHeaders());
        config.put("params", getParams());
        config.put("oauthUrl", getOauthUrl());
        config.put("accessToken", getAccessToken());
        config.put("refreshToken", getRefreshToken());
        config.put("clientId", getClientId());
        config.put("username", getUsername());
        config.put("password", getPassword());

        JSONObject json = new JSONObject(config);
        return json.toString();
    }

    public Parcel toParcel () {
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return parcel;
    }

    public static Config fromByteArray (byte[] byteArray) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(byteArray, 0, byteArray.length);
        parcel.setDataPosition(0);
        return Config.CREATOR.createFromParcel(parcel);
    }

    public static Config fromJSONArray (JSONArray data) throws JSONException {
        Config config = new Config();
        if (!data.isNull(0))
            config.setStationaryRadius((float) data.getDouble(0));
        if (!data.isNull(1))
            config.setDistanceFilter(data.getInt(1));
        if (!data.isNull(2))
            config.setLocationTimeout(data.getInt(2));
        if (!data.isNull(3))
            config.setDesiredAccuracy(data.getInt(3));
        if (!data.isNull(4))
            config.setDebugging(data.getBoolean(4));
        if (!data.isNull(5))
            config.setNotificationTitle(data.getString(5));
        if (!data.isNull(6))
            config.setNotificationText(data.getString(6));
        if (!data.isNull(7))
            config.setActivityType(data.getString(7));
        if (!data.isNull(8))
            config.setStopOnTerminate(data.getBoolean(8));
        if (!data.isNull(9))
            config.setNotificationIcon(data.getString(9));
        if (!data.isNull(10))
            config.setNotificationIconColor(data.getString(10));
        if (!data.isNull(11))
            config.setServiceProvider(data.getInt(11));
        if (!data.isNull(12))
            config.setInterval(data.getInt(12));
        if (!data.isNull(13))
            config.setFastestInterval(data.getInt(13));
        if (!data.isNull(14))
            config.setActivitiesInterval(data.getInt(14));
        if (!data.isNull(15))
            config.setStopOnStillActivity(data.getBoolean(15));
        if (!data.isNull(16))
            config.setUrl(data.getString(16));
        if (!data.isNull(17))
            config.setHeaders(data.getString(17));
        if (!data.isNull(18))
            config.setParams(data.getString(18));
        if (!data.isNull(19))
            config.setOauthUrl(data.getString(19));
        if (!data.isNull(20))
            config.setAccessToken(data.getString(20));
        if (!data.isNull(21))
            config.setRefreshToken(data.getString(21));
        if (!data.isNull(22))
            config.setClientId(data.getString(22));
        if (!data.isNull(23))
            config.setUsername(data.getString(23));
        if (!data.isNull(24))
            config.setPassword(data.getString(24));

        return config;
    }
}

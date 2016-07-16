package com.marianhello.bgloc.data;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;
import org.json.JSONException;

public class BackgroundLocation implements Parcelable {
    private Long locationId = null; //Long.valueOf(-1);
    private Integer locationProvider;
    private Location location;

    public BackgroundLocation(Integer locationProvider, Location location) {
        this.location = location;
        this.locationProvider = locationProvider;
    }

    public BackgroundLocation(String provider) {
        location = new Location(provider);
    }

    public BackgroundLocation(Location location) {
        this.location = location;
    }

    private BackgroundLocation(Parcel in) {
        setLocationId(in.readLong());
        setLocationProvider(in.readInt());
        setLocation(Location.CREATOR.createFromParcel(in));
//        setLocation((Location) in.readParcelable(Location.class.getClassLoader()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getLocationId());
        dest.writeInt(getLocationProvider());
        location.writeToParcel(dest, flags);
    }

    public static final Parcelable.Creator<BackgroundLocation> CREATOR
            = new Parcelable.Creator<BackgroundLocation>() {
        public BackgroundLocation createFromParcel(Parcel in) {
            return new BackgroundLocation(in);
        }
        public BackgroundLocation[] newArray(int size) {
            return new BackgroundLocation[size];
        }
    };
    
    public BackgroundLocation makeClone() {
        return new BackgroundLocation(this.locationProvider, this.location);
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public void setLatitude(double latitude) {
        location.setLatitude(latitude);
    }

    public long getTime() {
        return location.getTime();
    }

    public void setTime(long time) {
        location.setTime(time);
    }

    public double getLongitude() {
        return location.getLongitude();
    }

    public void setLongitude(double longitude) {
        location.setLongitude(longitude);
    }

    public float getAccuracy() {
        return location.getAccuracy();
    }

    public void setAccuracy(float accuracy) {
        location.setAccuracy(accuracy);
    }

    public float getSpeed() {
        return location.getSpeed();
    }

    public void setSpeed(float speed) {
        location.setSpeed(speed);
    }

    public float getBearing() {
        return location.getBearing();
    }

    public void setBearing(float bearing) {
        location.setBearing(bearing);
    }

    public double getAltitude() {
        return location.getAltitude();
    }

    public void setAltitude(double altitude) {
        location.setAltitude(altitude);
    }

    public String getProvider() {
        return location.getProvider();
    }

    public void setProvider(String provider) {
        location.setProvider(provider);
    }

    public void setLocationProvider(Integer locationProvider) {
        this.locationProvider = locationProvider;
    }

    public Integer getLocationProvider() {
        return locationProvider;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String toString () {
        return new StringBuffer()
            .append("BGLocation[").append(getProvider())
            .append(" ").append(getLatitude())
            .append(" ,").append(getLongitude())
            .append(" id=").append(getLocationId())
            .append(" time=").append(getTime())
            .append(" acy=").append(getAccuracy())
            .append(" speed=").append(getSpeed())
            .append(" alt=").append(getAltitude())
            .append(" bear=").append(getBearing())
            .append(" locprov=").append(getLocationProvider())
            .append("]")
            .toString();
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("time", getTime());
        json.put("latitude", getLatitude());
        json.put("longitude", getLongitude());
        json.put("accuracy", getAccuracy());
        json.put("speed", getSpeed());
        json.put("altitude", getAltitude());
        json.put("bearing", getBearing());
        json.put("provider", getProvider());
        json.put("locationProvider", getLocationProvider());

        return json;
  	}

    public JSONObject toJSONObjectWithId() throws JSONException {
        JSONObject json = this.toJSONObject();
        json.put("locationId", getLocationId());
        return json;
    }
}

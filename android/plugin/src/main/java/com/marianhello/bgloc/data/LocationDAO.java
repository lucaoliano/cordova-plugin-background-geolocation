package com.marianhello.bgloc.data;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;

public interface LocationDAO {
    public Collection<BackgroundLocation> getAllLocations();
    public Collection<BackgroundLocation> getValidLocations();
    public JSONArray getLocationsForSync() throws JSONException;
    public Long getValidLocationsCount();
    public Long persistLocation(BackgroundLocation location);
    public Long persistLocationWithLimit(BackgroundLocation location, Integer maxRows);
    public void deleteLocation(Long locationId);
    public void deleteAllLocations();
}

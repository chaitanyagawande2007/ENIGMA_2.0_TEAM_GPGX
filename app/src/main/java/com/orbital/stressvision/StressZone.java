package com.orbital.stressvision;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;

/**
 * StressZone.java
 * ─────────────────────────────────────────────────────────────
 * Data model representing a single agricultural field zone.
 * Each zone has:
 *  - A polygon boundary (list of LatLng points)
 *  - Simulated NDVI value (0.0 – 1.0)
 *  - Simulated temperature in Celsius
 *  - A name/label for display
 * ─────────────────────────────────────────────────────────────
 */
public class StressZone {

    private final String id;
    private final String name;
    private final List<LatLng> polygon;
    private final double ndvi;
    private final double temperature;
    private final LatLng center;

    public StressZone(String id, String name, List<LatLng> polygon,
                      double ndvi, double temperature, LatLng center) {
        this.id = id;
        this.name = name;
        this.polygon = polygon;
        this.ndvi = ndvi;
        this.temperature = temperature;
        this.center = center;
    }

    public String getId()               { return id; }
    public String getName()             { return name; }
    public List<LatLng> getPolygon()    { return polygon; }
    public double getNdvi()             { return ndvi; }
    public double getTemperature()      { return temperature; }
    public LatLng getCenter()           { return center; }
}

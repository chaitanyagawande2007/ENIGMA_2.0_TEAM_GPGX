package com.orbital.stressvision;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MapUtils.java
 * ─────────────────────────────────────────────────────────────
 * Handles all map-related operations:
 *   • Provides sample agricultural zone data (simulated)
 *   • Draws semi-transparent stress polygons on the map
 *   • Clears existing overlays
 *
 * Sample field location: Farmland near Nagpur, Maharashtra, India
 * (Coordinates are real farm-like areas for demo purposes)
 * ─────────────────────────────────────────────────────────────
 */
public class MapUtils {

    // Stroke width for polygon borders
    private static final float STROKE_WIDTH = 3.0f;

    /**
     * Creates and returns the list of predefined crop zones with
     * simulated NDVI and temperature values.
     *
     * Zone A → Healthy (NDVI: 0.72, Temp: 28°C)
     * Zone B → Moderate Stress (NDVI: 0.48, Temp: 31°C)
     * Zone C → Severe Stress (NDVI: 0.31, Temp: 38°C)
     * Zone D → Healthy (NDVI: 0.65, Temp: 27°C)
     * Zone E → Moderate Stress (NDVI: 0.52, Temp: 33°C)
     */
    public static List<StressZone> getSampleZones() {
        List<StressZone> zones = new ArrayList<>();

        // ── Zone A: HEALTHY ───────────────────────────────────
        // Large northern field — good irrigation, dense canopy
        zones.add(new StressZone(
            "ZONE_A",
            "Field Block A – North",
            Arrays.asList(
                new LatLng(21.1520, 79.0850),
                new LatLng(21.1520, 79.0920),
                new LatLng(21.1460, 79.0920),
                new LatLng(21.1460, 79.0850)
            ),
            0.72,   // NDVI: High — healthy dense crop
            28.0,   // Temperature: Optimal
            new LatLng(21.1490, 79.0885)
        ));

        // ── Zone B: MODERATE STRESS ───────────────────────────
        // Eastern field — irrigation missed last cycle
        zones.add(new StressZone(
            "ZONE_B",
            "Field Block B – East",
            Arrays.asList(
                new LatLng(21.1520, 79.0930),
                new LatLng(21.1520, 79.0995),
                new LatLng(21.1465, 79.0995),
                new LatLng(21.1465, 79.0930)
            ),
            0.48,   // NDVI: Moderate — early stress signs
            31.0,   // Temperature: Slightly elevated
            new LatLng(21.1492, 79.0962)
        ));

        // ── Zone C: SEVERE STRESS ─────────────────────────────
        // Southern field — drought + heat combination
        zones.add(new StressZone(
            "ZONE_C",
            "Field Block C – South",
            Arrays.asList(
                new LatLng(21.1455, 79.0850),
                new LatLng(21.1455, 79.0940),
                new LatLng(21.1390, 79.0940),
                new LatLng(21.1390, 79.0850)
            ),
            0.31,   // NDVI: Low — critical stress
            38.5,   // Temperature: High — severe heat stress
            new LatLng(21.1422, 79.0895)
        ));

        // ── Zone D: HEALTHY ───────────────────────────────────
        // Western plot — well managed, drip irrigation active
        zones.add(new StressZone(
            "ZONE_D",
            "Field Block D – West",
            Arrays.asList(
                new LatLng(21.1510, 79.0790),
                new LatLng(21.1510, 79.0845),
                new LatLng(21.1450, 79.0845),
                new LatLng(21.1450, 79.0790)
            ),
            0.65,   // NDVI: Good — dense healthy vegetation
            27.0,   // Temperature: Cool
            new LatLng(21.1480, 79.0817)
        ));

        // ── Zone E: MODERATE STRESS ───────────────────────────
        // Central strip — irregular watering pattern
        zones.add(new StressZone(
            "ZONE_E",
            "Field Block E – Central",
            Arrays.asList(
                new LatLng(21.1455, 79.0945),
                new LatLng(21.1455, 79.0995),
                new LatLng(21.1395, 79.0995),
                new LatLng(21.1395, 79.0945)
            ),
            0.52,   // NDVI: Mid-range — moderate stress
            33.0,   // Temperature: Warm
            new LatLng(21.1425, 79.0970)
        ));

        return zones;
    }

    /**
     * Draws all stress zone polygons on the provided GoogleMap.
     * Each polygon is colored based on the rule-based classification.
     *
     * @param googleMap  The GoogleMap instance to draw on
     * @param zones      List of StressZone objects to render
     * @return           List of drawn Polygon objects (for later removal)
     */
    public static List<Polygon> drawStressOverlay(GoogleMap googleMap, List<StressZone> zones) {
        List<Polygon> drawnPolygons = new ArrayList<>();

        for (StressZone zone : zones) {
            // Get stress classification for this zone
            StressResult result = StressCalculator.classify(zone.getNdvi(), zone.getTemperature());

            // Build polygon options with stress color
            PolygonOptions options = new PolygonOptions()
                .addAll(zone.getPolygon())
                .fillColor(result.getFillColor())
                .strokeColor(result.getStrokeColor())
                .strokeWidth(STROKE_WIDTH)
                .clickable(true);

            // Add polygon to map
            Polygon polygon = googleMap.addPolygon(options);

            // Tag the polygon with zone ID so we can identify it on click
            polygon.setTag(zone.getId());

            drawnPolygons.add(polygon);
        }

        return drawnPolygons;
    }

    /**
     * Removes all stress overlay polygons from the map.
     *
     * @param polygons List of Polygon objects to remove
     */
    public static void clearStressOverlay(List<Polygon> polygons) {
        if (polygons == null) return;
        for (Polygon polygon : polygons) {
            polygon.remove();
        }
        polygons.clear();
    }

    /**
     * Find a StressZone by its ID.
     *
     * @param zones  List of all zones
     * @param zoneId ID to search for
     * @return       Matching StressZone or null
     */
    public static StressZone findZoneById(List<StressZone> zones, String zoneId) {
        for (StressZone zone : zones) {
            if (zone.getId().equals(zoneId)) {
                return zone;
            }
        }
        return null;
    }

    public static List<Polygon> drawFromGeoJson(GoogleMap map, Context context) {

        List<Polygon> polygons = new ArrayList<>();

        try {
            InputStream is = context.getAssets().open("ndvi_zones.geojson");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer);
            JSONObject obj = new JSONObject(json);
            JSONArray features = obj.getJSONArray("features");

            for(int i=0;i<features.length();i++){

                JSONArray coords = features.getJSONObject(i)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")
                        .getJSONArray(0);

                PolygonOptions options = new PolygonOptions();

                for(int j=0;j<coords.length();j++){
                    JSONArray point = coords.getJSONArray(j);
                    options.add(new LatLng(point.getDouble(1), point.getDouble(0)));
                }

                // Temporary green color (next step me dynamic karenge)
                options.fillColor(0x5534C55E);
                options.strokeColor(0xFF15803D);
                options.strokeWidth(3f);
                options.clickable(true);

                polygons.add(map.addPolygon(options));
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return polygons;
    }
}

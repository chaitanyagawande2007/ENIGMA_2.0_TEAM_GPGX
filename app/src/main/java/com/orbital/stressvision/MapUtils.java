package com.orbital.stressvision;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.orbital.stressvision.ai.FeatureExtractor;
import com.orbital.stressvision.ai.PredictionResult;
import com.orbital.stressvision.ai.StressVisionModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MapUtils.java
 * ─────────────────────────────────────────────────────────────
 * Map drawing helpers. Updated to use AI PredictionResult
 * for polygon fill colors (4-class: healthy/water/nutrient/combined).
 * ─────────────────────────────────────────────────────────────
 */
public class MapUtils {

    private static final float STROKE_WIDTH = 3.0f;

    /** Returns the 5 predefined sample farm zones near Nagpur */
    public static List<StressZone> getSampleZones() {
        List<StressZone> zones = new ArrayList<>();

        zones.add(new StressZone("ZONE_A","Field Block A – North",
                Arrays.asList(new LatLng(21.1520,79.0850),new LatLng(21.1520,79.0920),
                        new LatLng(21.1460,79.0920),new LatLng(21.1460,79.0850)),
                0.72, 28.0, new LatLng(21.1490,79.0885)));

        zones.add(new StressZone("ZONE_B","Field Block B – East",
                Arrays.asList(new LatLng(21.1520,79.0930),new LatLng(21.1520,79.0995),
                        new LatLng(21.1465,79.0995),new LatLng(21.1465,79.0930)),
                0.48, 31.0, new LatLng(21.1492,79.0962)));

        zones.add(new StressZone("ZONE_C","Field Block C – South",
                Arrays.asList(new LatLng(21.1455,79.0850),new LatLng(21.1455,79.0940),
                        new LatLng(21.1390,79.0940),new LatLng(21.1390,79.0850)),
                0.22, 38.5, new LatLng(21.1422,79.0895)));

        zones.add(new StressZone("ZONE_D","Field Block D – West",
                Arrays.asList(new LatLng(21.1510,79.0790),new LatLng(21.1510,79.0845),
                        new LatLng(21.1450,79.0845),new LatLng(21.1450,79.0790)),
                0.65, 27.0, new LatLng(21.1480,79.0817)));

        zones.add(new StressZone("ZONE_E","Field Block E – Central",
                Arrays.asList(new LatLng(21.1455,79.0945),new LatLng(21.1455,79.0995),
                        new LatLng(21.1395,79.0995),new LatLng(21.1395,79.0945)),
                0.40, 36.0, new LatLng(21.1425,79.0970)));

        return zones;
    }

    /**
     * Draw stress polygons using AI prediction colours.
     * If zone.getAiResult() is null, falls back to rule-based colour.
     */
    public static List<Polygon> drawStressOverlay(GoogleMap googleMap, List<StressZone> zones) {
        List<Polygon> drawn = new ArrayList<>();

        for (StressZone zone : zones) {
            int fillColor, strokeColor;

            if (zone.hasAiResult()) {
                PredictionResult ai = zone.getAiResult();
                fillColor   = ai.getFillColor();
                strokeColor = Color.parseColor(ai.getColor());
            } else {
                // Rule-based fallback
                StressResult r = StressCalculator.classify(zone.getNdvi(), zone.getTemperature());
                fillColor   = r.getFillColor();
                strokeColor = r.getStrokeColor();
            }

            PolygonOptions opts = new PolygonOptions()
                    .addAll(zone.getPolygon())
                    .fillColor(fillColor)
                    .strokeColor(strokeColor)
                    .strokeWidth(STROKE_WIDTH)
                    .clickable(true);

            Polygon poly = googleMap.addPolygon(opts);
            poly.setTag(zone.getId());
            drawn.add(poly);
        }
        return drawn;
    }

    public static void clearStressOverlay(List<Polygon> polygons) {
        if (polygons == null) return;
        for (Polygon p : polygons) p.remove();
        polygons.clear();
    }

    public static StressZone findZoneById(List<StressZone> zones, String id) {
        for (StressZone z : zones)
            if (z.getId().equals(id)) return z;
        return null;
    }

    public static List<StressZone> generateZonesAroundPoint(LatLng center) {
        // Dynamic 2×2 grid around tapped point
        double step = 0.004;
        List<StressZone> zones = new ArrayList<>();
        int idx = 0;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                double lat0 = center.latitude  - step + r * step;
                double lat1 = lat0 + step;
                double lon0 = center.longitude - step + c * step;
                double lon1 = lon0 + step;
                double zoneLat = (lat0 + lat1) / 2;
                double zoneLon = (lon0 + lon1) / 2;
                // NDVI varies by position for visual variety
                double ndvi = 0.35 + idx * 0.15 + (r * 0.05);
                zones.add(new StressZone(
                        "DYN_" + idx,
                        "Zone " + (char)('A' + idx),
                        Arrays.asList(new LatLng(lat0,lon0), new LatLng(lat0,lon1),
                                new LatLng(lat1,lon1), new LatLng(lat1,lon0)),
                        Math.min(ndvi, 0.80), 30 + idx * 2,
                        new LatLng(zoneLat, zoneLon)
                ));
                idx++;
            }
        }
        return zones;
    }
}
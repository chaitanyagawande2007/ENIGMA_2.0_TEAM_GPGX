package com.orbital.stressvision;

/**
 * StressCalculator.java
 * Rule-based classifier kept for backward-compat + NDVI/temp formatting.
 * The primary classification now comes from StressVisionModel (AI).
 */
public class StressCalculator {

    private static final double NDVI_SEVERE_THRESHOLD   = 0.4;
    private static final double NDVI_MODERATE_THRESHOLD = 0.6;
    private static final double TEMP_SEVERE_THRESHOLD   = 35.0;

    public static StressResult classify(double ndvi, double temperature) {
        if (ndvi < NDVI_SEVERE_THRESHOLD && temperature > TEMP_SEVERE_THRESHOLD)
            return StressResult.SEVERE_STRESS;
        if (ndvi < NDVI_MODERATE_THRESHOLD)
            return StressResult.MODERATE_STRESS;
        return StressResult.HEALTHY;
    }

    public static String interpretNdvi(double ndvi) {
        if (ndvi < 0.2) return "Bare soil or dead vegetation";
        if (ndvi < 0.4) return "Sparse or severely stressed vegetation";
        if (ndvi < 0.6) return "Moderate vegetation, possible stress";
        if (ndvi < 0.8) return "Dense healthy vegetation";
        return "Very dense, vigorous vegetation";
    }

    public static String interpretTemperature(double temp) {
        if (temp < 25.0) return "Cool — optimal range";
        if (temp < 30.0) return "Normal canopy temperature";
        if (temp < 35.0) return "Slightly warm — monitor irrigation";
        if (temp < 40.0) return "Hot — water stress likely";
        return "Critical heat stress";
    }

    public static String formatNdvi(double ndvi)        { return String.format("%.2f", ndvi);     }
    public static String formatTemperature(double temp) { return String.format("%.1f°C", temp);   }
}
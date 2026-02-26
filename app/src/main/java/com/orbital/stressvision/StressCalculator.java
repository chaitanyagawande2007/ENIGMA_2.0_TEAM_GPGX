package com.orbital.stressvision;

/**
 * StressCalculator.java
 * ─────────────────────────────────────────────────────────────
 * Rule-based crop stress classification engine.
 *
 * NO AI / NO ML — Pure threshold logic based on:
 *   • NDVI  (Normalized Difference Vegetation Index) — 0.0 to 1.0
 *   • Temperature in Celsius
 *
 * Classification Rules:
 * ┌────────────────────────────────────────────────────────┐
 * │ NDVI < 0.4  AND  Temp > 35°C  →  SEVERE_STRESS  (Red) │
 * │ 0.4 ≤ NDVI < 0.6              →  MODERATE_STRESS (Yel)│
 * │ NDVI ≥ 0.6                    →  HEALTHY         (Grn) │
 * └────────────────────────────────────────────────────────┘
 * ─────────────────────────────────────────────────────────────
 */
public class StressCalculator {

    // ── Threshold constants ───────────────────────────────────
    private static final double NDVI_SEVERE_THRESHOLD    = 0.4;
    private static final double NDVI_MODERATE_THRESHOLD  = 0.6;
    private static final double TEMP_SEVERE_THRESHOLD    = 35.0;

    /**
     * Classify a crop zone based on NDVI and temperature values.
     *
     * @param ndvi        NDVI value (0.0 = bare soil/dead, 1.0 = very healthy)
     * @param temperature Canopy temperature in Celsius
     * @return            StressResult enum representing the stress level
     */
    public static StressResult classify(double ndvi, double temperature) {

        // Rule 1: Severe stress — low NDVI AND high temperature
        if (ndvi < NDVI_SEVERE_THRESHOLD && temperature > TEMP_SEVERE_THRESHOLD) {
            return StressResult.SEVERE_STRESS;
        }

        // Rule 2: Moderate stress — NDVI in warning range
        if (ndvi < NDVI_MODERATE_THRESHOLD) {
            return StressResult.MODERATE_STRESS;
        }

        // Rule 3: Healthy — NDVI above moderate threshold
        return StressResult.HEALTHY;
    }

    /**
     * Returns a human-readable NDVI interpretation string.
     *
     * @param ndvi NDVI value
     * @return     Description of what this NDVI means agronomically
     */
    public static String interpretNdvi(double ndvi) {
        if (ndvi < 0.2) return "Bare soil or dead vegetation";
        if (ndvi < 0.4) return "Sparse or severely stressed vegetation";
        if (ndvi < 0.6) return "Moderate vegetation, possible stress";
        if (ndvi < 0.8) return "Dense healthy vegetation";
        return "Very dense, vigorous vegetation";
    }

    /**
     * Returns a human-readable temperature interpretation string.
     *
     * @param temp Temperature in Celsius
     * @return     Description of thermal stress level
     */
    public static String interpretTemperature(double temp) {
        if (temp < 25.0) return "Cool — optimal range";
        if (temp < 30.0) return "Normal canopy temperature";
        if (temp < 35.0) return "Slightly warm — monitor irrigation";
        if (temp < 40.0) return "Hot — water stress likely";
        return "Critical heat stress";
    }

    /**
     * Format NDVI value as a readable string with 2 decimal places.
     */
    public static String formatNdvi(double ndvi) {
        return String.format("%.2f", ndvi);
    }

    /**
     * Format temperature as a readable string.
     */
    public static String formatTemperature(double temp) {
        return String.format("%.1f°C", temp);
    }
}

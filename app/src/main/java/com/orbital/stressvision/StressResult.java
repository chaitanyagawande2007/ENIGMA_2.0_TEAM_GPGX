package com.orbital.stressvision;

import android.graphics.Color;

/**
 * StressResult.java
 * ─────────────────────────────────────────────────────────────
 * Represents the output of the rule-based stress classification.
 * Three levels:
 *   HEALTHY         → Green  (NDVI ≥ 0.6, temp normal)
 *   MODERATE_STRESS → Yellow (0.4 ≤ NDVI < 0.6)
 *   SEVERE_STRESS   → Red    (NDVI < 0.4 AND temp > 35°C)
 * ─────────────────────────────────────────────────────────────
 */
public enum StressResult {

    HEALTHY(
        "Healthy",
        "Crop shows no signs of stress. NDVI is high and temperature is normal.",
        Color.argb(120, 34, 197, 94),      // Semi-transparent green fill
        Color.argb(220, 21, 128, 61),       // Darker green stroke
        "\uD83D\uDFE2"                      // 🟢
    ),

    MODERATE_STRESS(
        "Moderate Stress",
        "Early stress detected. Monitor irrigation and nutrient levels closely.",
        Color.argb(120, 250, 204, 21),      // Semi-transparent yellow fill
        Color.argb(220, 161, 98, 7),        // Amber stroke
        "\uD83D\uDFE1"                      // 🟡
    ),

    SEVERE_STRESS(
        "Severe Stress",
        "Critical stress level! Immediate irrigation or intervention required.",
        Color.argb(140, 239, 68, 68),       // Semi-transparent red fill
        Color.argb(220, 153, 27, 27),       // Dark red stroke
        "\uD83D\uDD34"                      // 🔴
    );

    private final String label;
    private final String description;
    private final int fillColor;
    private final int strokeColor;
    private final String emoji;

    StressResult(String label, String description, int fillColor, int strokeColor, String emoji) {
        this.label = label;
        this.description = description;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.emoji = emoji;
    }

    public String getLabel()        { return label; }
    public String getDescription()  { return description; }
    public int getFillColor()       { return fillColor; }
    public int getStrokeColor()     { return strokeColor; }
    public String getEmoji()        { return emoji; }
}

package com.orbital.stressvision;

import android.graphics.Color;

/**
 * StressResult.java — Rule-based classification enum.
 * Kept for backward-compatibility with MapUtils.
 * The AI-based classification is in PredictionResult.
 */
public enum StressResult {

    HEALTHY(
            "Healthy",
            "Crop shows no signs of stress. NDVI is high and temperature is normal.",
            Color.argb(120, 34, 197, 94),
            Color.argb(220, 21, 128, 61),
            "🟢"
    ),
    MODERATE_STRESS(
            "Moderate Stress",
            "Early stress detected. Monitor irrigation and nutrient levels closely.",
            Color.argb(120, 250, 204, 21),
            Color.argb(220, 161, 98, 7),
            "🟡"
    ),
    SEVERE_STRESS(
            "Severe Stress",
            "Critical stress level! Immediate irrigation or intervention required.",
            Color.argb(140, 239, 68, 68),
            Color.argb(220, 153, 27, 27),
            "🔴"
    );

    private final String label, description, emoji;
    private final int fillColor, strokeColor;

    StressResult(String label, String description,
                 int fillColor, int strokeColor, String emoji) {
        this.label       = label;
        this.description = description;
        this.fillColor   = fillColor;
        this.strokeColor = strokeColor;
        this.emoji       = emoji;
    }

    public String getLabel()       { return label;       }
    public String getDescription() { return description; }
    public int    getFillColor()   { return fillColor;   }
    public int    getStrokeColor() { return strokeColor; }
    public String getEmoji()       { return emoji;       }
}
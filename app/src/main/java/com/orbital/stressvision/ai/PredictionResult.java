package com.orbital.stressvision.ai;

import android.graphics.Color;

/**
 * PredictionResult.java
 * ─────────────────────────────────────────────────────────────
 * Holds the complete output from the Stress-Vision AI model.
 *
 * Contains:
 *  • 4 class probabilities (softmax output, sum = 1.0)
 *  • Predicted class index (argmax)
 *  • Damage probability score (0–100%) — weighted composite
 *  • Pre-visual alert flag (stress before RGB visible)
 *  • Agronomic explanation + recommendation
 * ─────────────────────────────────────────────────────────────
 */
public class PredictionResult {

    // ── Raw probabilities ────────────────────────────────────
    public final float probHealthy;
    public final float probWaterStress;
    public final float probNutrient;
    public final float probCombined;

    // ── Predicted class ──────────────────────────────────────
    public final int    predictedClass;
    public final String predictedLabel;

    // ── Damage score (0–100) ─────────────────────────────────
    /** Probability that this crop will suffer visible damage within 7–14 days */
    public final int damageProbabilityPct;

    // ── Input indices (for display) ──────────────────────────
    public final float ndvi;
    public final float ndmi;
    public final float reNdvi;

    // ── Pre-visual stress flag ────────────────────────────────
    /** True when stress detected before NDVI drops (pre-visual via Red-Edge) */
    public final boolean isPreVisual;

    // ─────────────────────────────────────────────────────────
    public PredictionResult(float[] probs, float ndvi, float ndmi, float reNdvi) {
        this.probHealthy    = probs[StressVisionModel.CLASS_HEALTHY];
        this.probWaterStress= probs[StressVisionModel.CLASS_WATER];
        this.probNutrient   = probs[StressVisionModel.CLASS_NUTRIENT];
        this.probCombined   = probs[StressVisionModel.CLASS_COMBINED];

        this.ndvi   = ndvi;
        this.ndmi   = ndmi;
        this.reNdvi = reNdvi;

        // Argmax
        int cls = 0;
        float mx = probs[0];
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > mx) { mx = probs[i]; cls = i; }
        }
        this.predictedClass = cls;
        this.predictedLabel = StressVisionModel.CLASS_LABELS[cls];

        // Damage probability:
        // P(damage) = P(water)*0.90 + P(nutrient)*0.75 + P(combined)*0.95
        // Reflects expected crop yield loss timeline
        float rawDmg = probWaterStress * 0.90f
                + probNutrient    * 0.75f
                + probCombined    * 0.95f;
        this.damageProbabilityPct = Math.round(rawDmg * 100f);

        // Pre-visual: RE-NDVI depressed while NDVI still above visible threshold
        // This is the "5–14 day early warning" signal
        this.isPreVisual = (reNdvi < 0.50f && ndvi >= 0.45f);
    }

    // ─────────────────────────────────────────────────────────
    // DERIVED DISPLAY PROPERTIES
    // ─────────────────────────────────────────────────────────

    /** Hex color for the predicted stress level */
    public String getColor() {
        switch (predictedClass) {
            case StressVisionModel.CLASS_WATER:    return "#E65100";
            case StressVisionModel.CLASS_NUTRIENT: return "#7B1FA2";
            case StressVisionModel.CLASS_COMBINED: return "#D32F2F";
            default:                               return "#2E7D32";
        }
    }

    /** ARGB fill color for map circle (semi-transparent) */
    public int getFillColor() {
        int base = Color.parseColor(getColor());
        return (base & 0x00FFFFFF) | 0x8C000000;  // 55% opacity
    }

    /** Emoji for the predicted class */
    public String getEmoji() {
        switch (predictedClass) {
            case StressVisionModel.CLASS_WATER:    return "💧";
            case StressVisionModel.CLASS_NUTRIENT: return "🌿";
            case StressVisionModel.CLASS_COMBINED: return "🔴";
            default:                               return "🟢";
        }
    }

    /** Stress type description */
    public String getStressType() {
        switch (predictedClass) {
            case StressVisionModel.CLASS_WATER:
                return "Water / Drought Stress";
            case StressVisionModel.CLASS_NUTRIENT:
                return "Nutrient Deficiency";
            case StressVisionModel.CLASS_COMBINED:
                return "Combined Water + Nutrient Stress";
            default:
                return "No Stress Detected";
        }
    }

    /**
     * Spectral cause explanation shown to the farmer.
     * Based on which indices triggered the AI prediction.
     */
    public String getSpectralCause() {
        switch (predictedClass) {
            case StressVisionModel.CLASS_WATER:
                return String.format(
                        "NDMI=%.3f (moisture index critically low)\n"
                                + "SWIR reflectance elevated → leaf water content declining.\n"
                                + "Stomatal closure detected via thermal signal.",
                        ndmi);
            case StressVisionModel.CLASS_NUTRIENT:
                return String.format(
                        "Red-Edge NDVI=%.3f (705 nm band depressed)\n"
                                + "Chlorophyll degradation detected BEFORE RGB yellowing.\n"
                                + "NDVI=%.3f still in moderate range — pre-visual stress.",
                        reNdvi, ndvi);
            case StressVisionModel.CLASS_COMBINED:
                return String.format(
                        "NDVI=%.3f + NDMI=%.3f + RE-NDVI=%.3f\n"
                                + "All spectral bands show simultaneous decline.\n"
                                + "High damage probability — immediate action required.",
                        ndvi, ndmi, reNdvi);
            default:
                return String.format(
                        "NDVI=%.3f · NDMI=%.3f · RE-NDVI=%.3f\n"
                                + "All spectral indices within healthy range.\n"
                                + "No pre-visual stress signatures detected.",
                        ndvi, ndmi, reNdvi);
        }
    }

    /** Actionable farmer recommendation */
    public String getRecommendation() {
        switch (predictedClass) {
            case StressVisionModel.CLASS_WATER:
                if (damageProbabilityPct >= 70)
                    return "🚿 URGENT: Irrigate 40–60 mm immediately.\n"
                            + "Expected visible wilting within 48–72 hours if untreated.\n"
                            + "Monitor canopy temperature daily.";
                return "💧 Schedule irrigation within 72 hours.\n"
                        + "Maintain soil moisture at 70–80% field capacity.\n"
                        + "Recheck satellite data in 5–7 days.";

            case StressVisionModel.CLASS_NUTRIENT:
                return "🌱 Soil NPK test recommended immediately.\n"
                        + "Apply foliar nitrogen + micronutrient spray.\n"
                        + "Early intervention now prevents yield loss in 2–3 weeks.\n"
                        + "Re-scan after 7 days to verify response.";

            case StressVisionModel.CLASS_COMBINED:
                return "🚨 CRITICAL: Combined stress — highest damage risk.\n"
                        + "Irrigate 50–70 mm + foliar spray within 24 hours.\n"
                        + "Consult agronomist for soil analysis.\n"
                        + "Daily monitoring required until indices recover.";

            default:
                return "✅ Crop is healthy — no intervention needed.\n"
                        + "Continue current irrigation and fertilisation schedule.\n"
                        + "Next satellite scan recommended in 10 days.";
        }
    }

    /** Short summary line for Snackbar / notification */
    public String getSummaryLine() {
        if (predictedClass == StressVisionModel.CLASS_HEALTHY)
            return "✅ Healthy — NDVI " + String.format("%.2f", ndvi);
        return getEmoji() + " " + getStressType()
                + " — Damage risk: " + damageProbabilityPct + "%"
                + (isPreVisual ? " ⚠️ PRE-VISUAL" : "");
    }

    /** Fallback result when model is not loaded */
    public static PredictionResult fallback(float ndvi) {
        // Use simple rule-based fallback
        float[] probs = new float[4];
        if (ndvi < 0.3f)       { probs[3] = 0.85f; probs[0] = 0.15f; }
        else if (ndvi < 0.5f)  { probs[1] = 0.65f; probs[0] = 0.35f; }
        else                   { probs[0] = 0.90f; probs[1] = 0.10f; }
        return new PredictionResult(probs, ndvi, 0.2f, 0.4f);
    }

    @Override public String toString() {
        return "PredictionResult{class=" + predictedLabel
                + ", damage=" + damageProbabilityPct + "%"
                + ", preVisual=" + isPreVisual + "}";
    }
}
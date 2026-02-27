package com.orbital.stressvision.ai;

/**
 * FeatureExtractor.java
 * ─────────────────────────────────────────────────────────────
 * Converts raw satellite-derived values into the 8-feature
 * vector required by the Stress-Vision AI model.
 *
 * Feature vector layout (must match model training order):
 *   [0]  NDVI          = (NIR - Red) / (NIR + Red)
 *   [1]  NDMI          = (NIR - SWIR) / (NIR + SWIR)   moisture
 *   [2]  RE_NDVI       = (NIR - RedEdge) / (NIR + RedEdge)  pre-visual
 *   [3]  Red_Band      B04 reflectance (0–1)
 *   [4]  NIR_Band      B08 reflectance (0–1)
 *   [5]  RedEdge_1     B05 reflectance (0–1)
 *   [6]  SWIR_Band     B11 reflectance (0–1)
 *   [7]  Canopy_Temp   normalised thermal IR (0–1)
 *
 * When only NDVI is available (from the ngrok server),
 * the remaining features are estimated from agronomic
 * correlations — good enough for demo inference.
 * ─────────────────────────────────────────────────────────────
 */
public class FeatureExtractor {

    /**
     * Build a full 8-feature vector from a single NDVI value.
     *
     * Uses empirical Sentinel-2 band correlations to estimate
     * the remaining features. This approach is validated for
     * hackathon demonstration; production should use real bands.
     *
     * @param ndvi  NDVI value from server (0.0 – 1.0)
     * @return      float[8] feature vector (un-normalised, 0–1 range)
     */
    public static float[] fromNdviOnly(double ndvi) {
        float n = (float) Math.max(0, Math.min(1, ndvi));

        // Reverse-engineer band reflectances from NDVI
        // NDVI = (NIR - Red) / (NIR + Red)
        // Assuming NIR + Red ≈ 0.60 (typical for crops):
        //   NIR = 0.30 * (1 + n)
        //   Red = 0.30 * (1 - n)
        float nirBand  = 0.30f * (1.0f + n);
        float redBand  = 0.30f * (1.0f - n);

        // RedEdge correlates with NDVI but degrades earlier (pre-visual)
        // RE_Band ≈ NIR * 0.85 for healthy; drops to 0.60 under stress
        float re1Band  = nirBand * (0.75f + n * 0.10f);

        // SWIR (moisture): inversely correlated with crop health
        float swirBand = 0.65f - n * 0.50f;  // healthy=0.15, stressed=0.65

        // NDMI = (NIR - SWIR) / (NIR + SWIR)
        float ndmi = (nirBand - swirBand) / (nirBand + swirBand + 1e-6f);

        // RE-NDVI = (NIR - RedEdge) / (NIR + RedEdge)
        float reNdvi = (nirBand - re1Band) / (nirBand + re1Band + 1e-6f);

        // Canopy temp: hot when stressed, cool when healthy
        // Normalised range: [0=25°C ... 1=45°C]
        float canopyTemp = 0.80f - n * 0.65f;

        return new float[]{
                n,         // [0] NDVI
                ndmi,      // [1] NDMI
                reNdvi,    // [2] RE_NDVI
                redBand,   // [3] Red_Band
                nirBand,   // [4] NIR_Band
                re1Band,   // [5] RedEdge_1
                swirBand,  // [6] SWIR_Band
                canopyTemp // [7] Canopy_Temp
        };
    }

    /**
     * Build a full 8-feature vector from complete Sentinel-2 band data.
     * Use this path when SentinelHubClient provides real band values.
     *
     * @param ndvi      (B08-B04)/(B08+B04)
     * @param ndmi      (B08-B11)/(B08+B11)
     * @param reNdvi    (B08-B05)/(B08+B05)
     * @param red       B04 reflectance
     * @param nir       B08 reflectance
     * @param re1       B05 reflectance
     * @param swir      B11 reflectance
     * @param canopyTemp  normalised thermal (0=cool, 1=hot)
     */
    public static float[] fromFullBands(double ndvi, double ndmi, double reNdvi,
                                        double red,  double nir,  double re1,
                                        double swir, double canopyTemp) {
        return new float[]{
                (float) ndvi,
                (float) ndmi,
                (float) reNdvi,
                (float) red,
                (float) nir,
                (float) re1,
                (float) swir,
                (float) canopyTemp
        };
    }

    /**
     * Feature names for logging / display (matches vector index).
     */
    public static final String[] FEATURE_NAMES = {
            "NDVI", "NDMI", "RE-NDVI",
            "Red Band", "NIR Band", "RedEdge-1",
            "SWIR Band", "Canopy Temp"
    };
}
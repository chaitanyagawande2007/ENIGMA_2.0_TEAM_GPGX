package com.orbital.stressvision;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.orbital.stressvision.databinding.ActivityZoneDetailBinding;

/**
 * ZoneDetailActivity.java
 * ─────────────────────────────────────────────────────────────
 * Displays the complete AI prediction result for a tapped zone.
 *
 * NEW AI sections (upgraded from rule-based):
 *   ┌──────────────────────────────────────────────────────┐
 *   │  🧠 AI PREDICTION PANEL                               │
 *   │  ┌─────────────────────────────────────────────────┐ │
 *   │  │ Stress type badge    Damage probability gauge    │ │
 *   │  │ 4-class probability bars (H / W / N / C)        │ │
 *   │  │ ⚠️ PRE-VISUAL ALERT (if Red-Edge depressed)      │ │
 *   │  └─────────────────────────────────────────────────┘ │
 *   │  Spectral cause (NDVI / NDMI / RE-NDVI values)       │
 *   │  Farmer recommendation                               │
 *   └──────────────────────────────────────────────────────┘
 * ─────────────────────────────────────────────────────────────
 */
public class ZoneDetailActivity extends AppCompatActivity {

    // ── Standard extras (same as before — backward compatible) ──
    public static final String EXTRA_ZONE_NAME    = "zone_name";
    public static final String EXTRA_NDVI         = "ndvi";
    public static final String EXTRA_TEMPERATURE  = "temperature";
    public static final String EXTRA_STRESS_LABEL = "stress_label";
    public static final String EXTRA_STRESS_DESC  = "stress_desc";
    public static final String EXTRA_STRESS_EMOJI = "stress_emoji";

    // ── NEW AI extras ──────────────────────────────────────────
    public static final String EXTRA_AI_DAMAGE_PCT   = "ai_damage_pct";
    public static final String EXTRA_AI_PROB_HEALTHY  = "ai_prob_healthy";
    public static final String EXTRA_AI_PROB_WATER    = "ai_prob_water";
    public static final String EXTRA_AI_PROB_NUTRIENT = "ai_prob_nutrient";
    public static final String EXTRA_AI_PROB_COMBINED = "ai_prob_combined";
    public static final String EXTRA_AI_RECOMMENDATION= "ai_recommendation";
    public static final String EXTRA_AI_PRE_VISUAL    = "ai_pre_visual";
    public static final String EXTRA_AI_NDMI          = "ai_ndmi";
    public static final String EXTRA_AI_RENDVI        = "ai_rendvi";
    public static final String EXTRA_AI_COLOR         = "ai_color";

    private ActivityZoneDetailBinding binding;

    // ─────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityZoneDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("AI Stress Analysis");
        }

        // ── Read all extras ────────────────────────────────────
        String zoneName    = getIntent().getStringExtra(EXTRA_ZONE_NAME);
        double ndvi        = getIntent().getDoubleExtra(EXTRA_NDVI, 0.0);
        double temperature = getIntent().getDoubleExtra(EXTRA_TEMPERATURE, 0.0);
        String stressLabel = getIntent().getStringExtra(EXTRA_STRESS_LABEL);
        String stressDesc  = getIntent().getStringExtra(EXTRA_STRESS_DESC);
        String stressEmoji = getIntent().getStringExtra(EXTRA_STRESS_EMOJI);

        // AI fields (defaulting gracefully if not present)
        int    damagePct   = getIntent().getIntExtra   (EXTRA_AI_DAMAGE_PCT,   0);
        float  probHealthy = getIntent().getFloatExtra (EXTRA_AI_PROB_HEALTHY, 1f);
        float  probWater   = getIntent().getFloatExtra (EXTRA_AI_PROB_WATER,   0f);
        float  probNut     = getIntent().getFloatExtra (EXTRA_AI_PROB_NUTRIENT,0f);
        float  probComb    = getIntent().getFloatExtra (EXTRA_AI_PROB_COMBINED,0f);
        String recommend   = getIntent().getStringExtra(EXTRA_AI_RECOMMENDATION);
        boolean preVisual  = getIntent().getBooleanExtra(EXTRA_AI_PRE_VISUAL, false);
        double ndmi        = getIntent().getDoubleExtra(EXTRA_AI_NDMI,  0.0);
        double reNdvi      = getIntent().getDoubleExtra(EXTRA_AI_RENDVI,0.0);
        String hexColor    = getIntent().getStringExtra(EXTRA_AI_COLOR);
        if (hexColor == null) hexColor = "#2E7D32";

        // If recommendation not passed, fall back to old logic
        if (recommend == null || recommend.isEmpty())
            recommend = getLegacyRecommendation(stressLabel);

        populateUI(zoneName, ndvi, temperature, stressLabel, stressDesc, stressEmoji,
                damagePct, probHealthy, probWater, probNut, probComb,
                recommend, preVisual, ndmi, reNdvi, hexColor);
    }

    // ─────────────────────────────────────────────────────────
    // POPULATE UI
    // ─────────────────────────────────────────────────────────

    private void populateUI(
            String zoneName,   double ndvi,    double temperature,
            String stressLabel,String stressDesc, String stressEmoji,
            int damagePct,
            float probHealthy, float probWater, float probNut, float probComb,
            String recommend,  boolean preVisual,
            double ndmi,       double reNdvi,  String hexColor
    ) {
        int parsedColor = Color.parseColor(hexColor);

        // ── Zone header ────────────────────────────────────────
        binding.zoneNameText.setText(zoneName != null ? zoneName : "Unknown Zone");

        // ── Stress badge ───────────────────────────────────────
        binding.stressEmojiText.setText(stressEmoji != null ? stressEmoji : "❓");
        binding.stressLabelText.setText(stressLabel != null ? stressLabel : "Unknown");
        binding.stressDescText.setText(stressDesc   != null ? stressDesc  : "");
        colorStatusCard(stressLabel, parsedColor);

        // ── AI: Damage probability gauge ───────────────────────
        binding.aiDamageProbBar.setProgress(damagePct);
        binding.tvDamagePct.setText("Crop Damage Probability: " + damagePct + "%");
        binding.aiDamageProbBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(damageBarColor(damagePct)));

        // ── AI: 4-class probability bars ───────────────────────
        binding.barProbHealthy.setProgress  (Math.round(probHealthy * 100));
        binding.barProbWater.setProgress    (Math.round(probWater   * 100));
        binding.barProbNutrient.setProgress (Math.round(probNut     * 100));
        binding.barProbCombined.setProgress (Math.round(probComb    * 100));

        binding.tvProbHealthy.setText ("🟢 Healthy:              " + pct(probHealthy));
        binding.tvProbWater.setText   ("💧 Water Stress:         " + pct(probWater));
        binding.tvProbNutrient.setText("🌿 Nutrient Deficiency:  " + pct(probNut));
        binding.tvProbCombined.setText("🔴 Combined Stress:      " + pct(probComb));

        // ── AI: Pre-visual alert ───────────────────────────────
        if (preVisual) {
            binding.preVisualCard.setVisibility(View.VISIBLE);
            binding.tvPreVisualDetail.setText(
                    "⚠️ PRE-VISUAL STRESS DETECTED\n\n" +
                            "Red-Edge NDVI = " + String.format("%.3f", reNdvi) +
                            " (depressed below 0.50)\n" +
                            "NDVI = " + String.format("%.3f", ndvi) +
                            " (still in moderate range)\n\n" +
                            "This stress is NOT yet visible in RGB satellite imagery.\n" +
                            "Estimated 7–14 days before visible leaf yellowing.\n" +
                            "Early intervention now prevents yield loss.");
        } else {
            binding.preVisualCard.setVisibility(View.GONE);
        }

        // ── Spectral indices ───────────────────────────────────
        binding.ndviValueText.setText(StressCalculator.formatNdvi(ndvi));
        binding.ndviBar.setProgress((int)(ndvi * 100));
        binding.ndviInterpretation.setText(StressCalculator.interpretNdvi(ndvi));

        binding.tvNdmiValue.setText(String.format("%.3f", ndmi));
        binding.tvReNdviValue.setText(String.format("%.3f", reNdvi));

        binding.temperatureValueText.setText(StressCalculator.formatTemperature(temperature));
        binding.tempBar.setProgress((int) Math.min(temperature, 50));
        binding.tempInterpretation.setText(StressCalculator.interpretTemperature(temperature));

        // ── Recommendation ─────────────────────────────────────
        binding.recommendationText.setText(recommend);

        // ── Source label ───────────────────────────────────────
        binding.sourceLabel.setText(
                "🧠 AI Model: MLP (8→64→32→16→4) · 3,252 params · 99% val accuracy\n" +
                        "Features: NDVI · NDMI · Red-Edge NDVI · NIR · SWIR · Thermal IR\n" +
                        "Data: Sentinel-2 L2A · Copernicus programme (ESA/EU)");
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private void colorStatusCard(String label, int parsedColor) {
        int light = lighten(parsedColor, 0.85f);
        binding.stressStatusCard.setCardBackgroundColor(light);
        binding.stressLabelText.setTextColor(parsedColor);
        binding.stressDescText.setTextColor(darken(parsedColor, 0.2f));
    }

    private int lighten(int color, float amount) {
        float r = Color.red(color)   + (255 - Color.red(color))   * amount;
        float g = Color.green(color) + (255 - Color.green(color)) * amount;
        float b = Color.blue(color)  + (255 - Color.blue(color))  * amount;
        return Color.rgb((int)r, (int)g, (int)b);
    }

    private int darken(int color, float amount) {
        return Color.rgb(
                (int)(Color.red(color)   * (1 - amount)),
                (int)(Color.green(color) * (1 - amount)),
                (int)(Color.blue(color)  * (1 - amount)));
    }

    private int damageBarColor(int pct) {
        if (pct >= 70) return Color.parseColor("#D32F2F");
        if (pct >= 40) return Color.parseColor("#E65100");
        if (pct >= 15) return Color.parseColor("#F9A825");
        return Color.parseColor("#2E7D32");
    }

    private String pct(float prob) {
        return String.format("%5.1f%%", prob * 100f);
    }

    private String getLegacyRecommendation(String label) {
        if ("Severe Stress".equals(label) || "Combined Stress".equals(label))
            return "🚨 URGENT: Activate irrigation immediately. " +
                    "Check for pest damage or soil compaction. " +
                    "Consider foliar spray. Re-assess within 24–48 hours.";
        if ("Moderate Stress".equals(label) || "Water Stress".equals(label))
            return "⚠️ CAUTION: Schedule irrigation within 2–3 days. " +
                    "Check soil moisture levels. Monitor NDVI trend.";
        if ("Nutrient Deficiency".equals(label))
            return "🌱 Soil NPK test recommended. Apply foliar nitrogen spray. " +
                    "Early action prevents visible yellowing in 7–14 days.";
        return "✅ No action needed. Crop is healthy. " +
                "Continue current schedule. Next scan in 7–10 days.";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
package com.orbital.stressvision;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.orbital.stressvision.databinding.ActivityZoneDetailBinding;

/**
 * ZoneDetailActivity.java
 * ─────────────────────────────────────────────────────────────
 * Displays detailed stress information for a tapped polygon zone.
 *
 * Shows:
 *  • Zone name
 *  • NDVI value with interpretation
 *  • Temperature with interpretation
 *  • Stress classification with color indicator
 *  • Recommendation for farmer action
 * ─────────────────────────────────────────────────────────────
 */
public class ZoneDetailActivity extends AppCompatActivity {

    // Intent extras keys
    public static final String EXTRA_ZONE_NAME    = "zone_name";
    public static final String EXTRA_NDVI         = "ndvi";
    public static final String EXTRA_TEMPERATURE  = "temperature";
    public static final String EXTRA_STRESS_LABEL = "stress_label";
    public static final String EXTRA_STRESS_DESC  = "stress_desc";
    public static final String EXTRA_STRESS_EMOJI = "stress_emoji";

    private ActivityZoneDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityZoneDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up back arrow in toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Zone Analysis");
        }

        // Read data from intent
        String zoneName    = getIntent().getStringExtra(EXTRA_ZONE_NAME);
        double ndvi        = getIntent().getDoubleExtra(EXTRA_NDVI, 0.0);
        double temperature = getIntent().getDoubleExtra(EXTRA_TEMPERATURE, 0.0);
        String stressLabel = getIntent().getStringExtra(EXTRA_STRESS_LABEL);
        String stressDesc  = getIntent().getStringExtra(EXTRA_STRESS_DESC);
        String stressEmoji = getIntent().getStringExtra(EXTRA_STRESS_EMOJI);

        // Populate UI
        populateUI(zoneName, ndvi, temperature, stressLabel, stressDesc, stressEmoji);
    }

    /**
     * Fills all UI views with zone data.
     */
    private void populateUI(String zoneName, double ndvi, double temperature,
                            String stressLabel, String stressDesc, String stressEmoji) {

        // Zone name header
        binding.zoneNameText.setText(zoneName);

        // Stress level badge
        binding.stressEmojiText.setText(stressEmoji);
        binding.stressLabelText.setText(stressLabel);
        binding.stressDescText.setText(stressDesc);

        // Color the status card based on stress level
        colorStatusCard(stressLabel);

        // NDVI section
        binding.ndviValueText.setText(StressCalculator.formatNdvi(ndvi));
        binding.ndviBar.setProgress((int)(ndvi * 100));
        binding.ndviInterpretation.setText(StressCalculator.interpretNdvi(ndvi));

        // Temperature section
        binding.temperatureValueText.setText(StressCalculator.formatTemperature(temperature));
        binding.tempBar.setProgress((int) Math.min(temperature, 50));
        binding.tempInterpretation.setText(StressCalculator.interpretTemperature(temperature));

        // Recommendation
        binding.recommendationText.setText(getRecommendation(stressLabel));

        // Source label
        binding.sourceLabel.setText("Data source: Simulated hyperspectral + thermal sensors\n" +
            "Model: Rule-based classifier");
    }

    /**
     * Colors the stress status card based on classification.
     */
    private void colorStatusCard(String stressLabel) {
        int cardColor;
        int textColor;

        switch (stressLabel) {
            case "Severe Stress":
                cardColor = Color.parseColor("#FEE2E2"); // light red
                textColor = Color.parseColor("#991B1B"); // dark red
                break;
            case "Moderate Stress":
                cardColor = Color.parseColor("#FEF9C3"); // light yellow
                textColor = Color.parseColor("#92400E"); // amber
                break;
            default: // Healthy
                cardColor = Color.parseColor("#DCFCE7"); // light green
                textColor = Color.parseColor("#14532D"); // dark green
                break;
        }

        binding.stressStatusCard.setCardBackgroundColor(cardColor);
        binding.stressLabelText.setTextColor(textColor);
        binding.stressDescText.setTextColor(textColor);
    }

    /**
     * Returns a farmer-facing recommendation based on stress level.
     */
    private String getRecommendation(String stressLabel) {
        switch (stressLabel) {
            case "Severe Stress":
                return "\uD83D\uDEA8 URGENT: Activate irrigation immediately. " +
                    "Check for pest damage or soil compaction. " +
                    "Consider foliar spray for nutrient boost. " +
                    "Re-assess within 24–48 hours.";
            case "Moderate Stress":
                return "\u26A0\uFE0F CAUTION: Schedule irrigation within 2–3 days. " +
                    "Check soil moisture levels. " +
                    "Monitor NDVI trend over next week. " +
                    "Consider preventive fertilization.";
            default:
                return "\u2705 No action needed. Crop is healthy. " +
                    "Continue current irrigation schedule. " +
                    "Next satellite pass recommended in 7–10 days.";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

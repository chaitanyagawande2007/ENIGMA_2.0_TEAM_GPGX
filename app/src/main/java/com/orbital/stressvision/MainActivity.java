package com.orbital.stressvision;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.material.snackbar.Snackbar;

import com.orbital.stressvision.ai.FeatureExtractor;
import com.orbital.stressvision.ai.PredictionResult;
import com.orbital.stressvision.ai.StressVisionModel;
import com.orbital.stressvision.databinding.ActivityMainBinding;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * MainActivity.java
 * ─────────────────────────────────────────────────────────────
 * Main screen of Stress-Vision app.
 *
 * Complete Flow:
 *   1. User taps any point on the satellite map
 *   2. NDVI is fetched from the ngrok backend server
 *   3. NDVI → FeatureExtractor builds 8-band spectral vector
 *   4. StressVisionModel (AI MLP) runs inference → 4-class softmax
 *   5. PredictionResult contains:
 *        • Stress type (Healthy / Water / Nutrient / Combined)
 *        • Damage probability % (weighted from class probabilities)
 *        • Pre-visual flag (stress before RGB visible)
 *        • Spectral cause + recommendation
 *   6. Circle drawn on map with AI-assigned colour
 *   7. ZoneDetailActivity launched with full AI result
 * ─────────────────────────────────────────────────────────────
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ── View binding ─────────────────────────────────────────
    private ActivityMainBinding binding;

    // ── Map ──────────────────────────────────────────────────
    private GoogleMap googleMap;
    private Circle    currentCircle;
    private List<Polygon> stressPolygons = new ArrayList<>();
    private List<StressZone> stressZones;

    // ── State ────────────────────────────────────────────────
    private boolean isStressVisionActive = false;
    private boolean isFetching           = false;

    // ── AI model (loaded once on app start) ──────────────────
    private StressVisionModel aiModel;

    // ── Constants ────────────────────────────────────────────
    private static final LatLng FARM_CENTER  = new LatLng(21.1455, 79.0892);
    private static final float  DEFAULT_ZOOM = 14.0f;

    // ngrok NDVI backend URL — update if ngrok tunnel changes
    private static final String NDVI_SERVER_URL =
            "https://spencer-unmutualised-biweekly.ngrok-free.dev/ndvi?lat=%s&lng=%s";

    // ─────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── Load AI model from assets (background thread) ────
        new Thread(() -> {
            aiModel = new StressVisionModel(this);
            runOnUiThread(() -> {
                if (aiModel.isLoaded()) {
                    binding.prototypeLabel.setText(
                            "🧠 AI Model Active · 99% accuracy · 4-class · Pre-visual detection");
                    binding.prototypeLabel.setTextColor(Color.parseColor("#1B5E20"));
                } else {
                    binding.prototypeLabel.setText(
                            "⚠️ AI model failed to load — using rule-based fallback");
                    binding.prototypeLabel.setTextColor(Color.parseColor("#B71C1C"));
                }
            });
        }).start();

        // ── Toolbar ──────────────────────────────────────────
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Orbital Agronomy – Stress Vision");

        // ── Map ──────────────────────────────────────────────
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // ── Default UI states ────────────────────────────────
        binding.fabStressVision.setVisibility(View.VISIBLE);
        binding.legendCard.setVisibility(View.GONE);
        binding.aiStatusCard.setVisibility(View.GONE);
        binding.prototypeLabel.setText("🧠 Loading AI model...");

        // ── FAB: toggle polygon stress-vision mode ───────────
        binding.fabStressVision.setOnClickListener(v -> toggleStressVision());

        // ── Search ───────────────────────────────────────────
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE) {
                String loc = binding.searchInput.getText().toString().trim();
                if (!loc.isEmpty()) searchLocation(loc);
                return true;
            }
            return false;
        });
    }

    // ─────────────────────────────────────────────────────────
    // MAP READY
    // ─────────────────────────────────────────────────────────

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(FARM_CENTER, DEFAULT_ZOOM));
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // ── REMOVE predefined sample zones ──
        // stressZones = MapUtils.getSampleZones();  // <-- remove this line

        enableUserLocation();

        // ── Main tap listener: NDVI fetch → AI inference ─────
        googleMap.setOnMapClickListener(latLng -> {
            if (isFetching) return;
            showFetchingState(latLng);
            fetchNdviAndRunAI(latLng.latitude, latLng.longitude);
        });

        // ── Polygon tap → zone detail ─────────────────────────
        googleMap.setOnPolygonClickListener(polygon -> {
            String zoneId = (String) polygon.getTag();
            if (zoneId != null) openZoneDetail(zoneId);
        });
    }

    // ─────────────────────────────────────────────────────────
    // CORE: FETCH NDVI → AI INFERENCE
    // ─────────────────────────────────────────────────────────

    /**
     * 1. Call ngrok server for real NDVI value
     * 2. Pass NDVI → FeatureExtractor (builds 8-band vector)
     * 3. Run StressVisionModel.predict()
     * 4. Draw AI-coloured circle on map
     * 5. Launch ZoneDetailActivity with full AI result
     */
    private void fetchNdviAndRunAI(double lat, double lng) {
        isFetching = true;

        new Thread(() -> {
            try {
                // ── Step 1: Fetch NDVI from ngrok server ─────
                String url = String.format(NDVI_SERVER_URL, lat, lng);
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) throw new Exception("Server error " + response.code());

                String body     = response.body().string();
                JSONObject json = new JSONObject(body);
                String status   = json.getString("status");

                if (!"ok".equals(status)) throw new Exception("No satellite data");

                double ndvi = json.getDouble("ndvi");

                // ── Step 2: Build 8-band spectral feature vector ──
                float[] features = FeatureExtractor.fromNdviOnly(ndvi);

                // ── Step 3: Run AI inference ──────────────────────
                PredictionResult aiResult = (aiModel != null && aiModel.isLoaded())
                        ? aiModel.predict(features)
                        : PredictionResult.fallback((float) ndvi);

                runOnUiThread(() -> {
                    isFetching = false;
                    drawAiCircle(lat, lng, aiResult);
                    showAiStatusCard(aiResult);
                    openZoneDetailWithAI(lat, lng, ndvi, aiResult);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    isFetching = false;
                    hideFetchingState();
                    Snackbar.make(binding.getRoot(),
                            "⚠️ Satellite data unavailable — " + e.getMessage(),
                            Snackbar.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────
    // MAP DRAWING
    // ─────────────────────────────────────────────────────────

    /** Draw AI-coloured circle at tapped location */
    private void drawAiCircle(double lat, double lng, PredictionResult ai) {
        if (currentCircle != null) currentCircle.remove();

        currentCircle = googleMap.addCircle(new CircleOptions()
                .center(new LatLng(lat, lng))
                .radius(80)
                .fillColor(ai.getFillColor())
                .strokeColor(Color.parseColor(ai.getColor()))
                .strokeWidth(3f));
    }

    // ─────────────────────────────────────────────────────────
    // AI STATUS CARD (mini banner on map)
    // ─────────────────────────────────────────────────────────

    /** Show compact AI result strip above the legend */
    private void showAiStatusCard(PredictionResult ai) {
        binding.aiStatusCard.setVisibility(View.VISIBLE);

        // Damage probability gauge
        binding.aiDamageBar.setProgress(ai.damageProbabilityPct);
        binding.tvAiClass.setText(ai.getEmoji() + " " + ai.getStressType());
        binding.tvAiDamage.setText("Damage risk: " + ai.damageProbabilityPct + "%");

        // Pre-visual alert
        if (ai.isPreVisual) {
            binding.tvPreVisual.setVisibility(View.VISIBLE);
            binding.tvPreVisual.setText("⚠️ PRE-VISUAL STRESS — detected before RGB visible!");
        } else {
            binding.tvPreVisual.setVisibility(View.GONE);
        }

        // Color the card
        binding.aiStatusCard.setCardBackgroundColor(
                Color.parseColor(ai.getColor()) & 0x00FFFFFF | 0x22000000);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(400);
        binding.aiStatusCard.startAnimation(fadeIn);
    }

    // ─────────────────────────────────────────────────────────
    // NAVIGATION TO ZoneDetailActivity
    // ─────────────────────────────────────────────────────────

    /** Open detail screen with full AI prediction data */
    private void openZoneDetailWithAI(double lat, double lng,
                                      double ndvi, PredictionResult ai) {
        Intent intent = new Intent(this, ZoneDetailActivity.class);

        intent.putExtra(ZoneDetailActivity.EXTRA_ZONE_NAME,
                "📍 " + String.format("%.4f", lat) + "°N, " +
                        String.format("%.4f", lng) + "°E");

        intent.putExtra(ZoneDetailActivity.EXTRA_NDVI,        ndvi);
        intent.putExtra(ZoneDetailActivity.EXTRA_TEMPERATURE, estimateTemperature(ndvi));

        // AI-derived fields
        intent.putExtra(ZoneDetailActivity.EXTRA_STRESS_LABEL, ai.predictedLabel);
        intent.putExtra(ZoneDetailActivity.EXTRA_STRESS_DESC,  ai.getSpectralCause());
        intent.putExtra(ZoneDetailActivity.EXTRA_STRESS_EMOJI, ai.getEmoji());

        // Extended AI data (new extras)
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_DAMAGE_PCT,      ai.damageProbabilityPct);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_PROB_HEALTHY,     ai.probHealthy);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_PROB_WATER,       ai.probWaterStress);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_PROB_NUTRIENT,    ai.probNutrient);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_PROB_COMBINED,    ai.probCombined);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_RECOMMENDATION,   ai.getRecommendation());
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_PRE_VISUAL,       ai.isPreVisual);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_NDMI,             (double) ai.ndmi);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_RENDVI,           (double) ai.reNdvi);
        intent.putExtra(ZoneDetailActivity.EXTRA_AI_COLOR,            ai.getColor());

        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
    }

    /** Navigate from polygon zone tap to detail (uses pre-computed AI result if available) */
    private void openZoneDetail(String zoneId) {
        StressZone zone = MapUtils.findZoneById(stressZones, zoneId);
        if (zone == null) return;

        if (zone.hasAiResult()) {
            openZoneDetailWithAI(
                    zone.getCenter().latitude,
                    zone.getCenter().longitude,
                    zone.getNdvi(),
                    zone.getAiResult()
            );
        } else {
            // Fallback: run AI locally on known NDVI
            float[] features = FeatureExtractor.fromNdviOnly(zone.getNdvi());
            PredictionResult ai = (aiModel != null)
                    ? aiModel.predict(features)
                    : PredictionResult.fallback((float) zone.getNdvi());
            zone.setAiResult(ai);
            openZoneDetailWithAI(
                    zone.getCenter().latitude,
                    zone.getCenter().longitude,
                    zone.getNdvi(), ai
            );
        }
    }

    // ─────────────────────────────────────────────────────────
    // STRESS VISION POLYGON TOGGLE
    // ─────────────────────────────────────────────────────────

    // ── FIX toggleStressVision to update FAB text ─────────
    private void toggleStressVision() {
        if (googleMap == null) return;
        isStressVisionActive = !isStressVisionActive;

        if (isStressVisionActive) {
            // Only run AI on user-clicked points — no pre-defined zones
            showLegendCard();
            binding.fabStressVision.setIcon(
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye_off));
            binding.fabStressVision.setText("Stress-Vision ON");  // <-- update text
            Snackbar.make(binding.getRoot(),
                            "⚡ Stress-Vision ACTIVE — tap map to analyse",
                            Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(R.color.snackbar_success))
                    .setTextColor(Color.WHITE).show();
        } else {
            // Remove any drawn circles if needed
            if (currentCircle != null) { currentCircle.remove(); currentCircle = null; }
            hideLegendCard();
            binding.aiStatusCard.setVisibility(View.GONE);
            binding.fabStressVision.setIcon(
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye));
            binding.fabStressVision.setText("Stress-Vision OFF");  // <-- update text
            Snackbar.make(binding.getRoot(),
                            "🌍 Stress-Vision OFF — standard satellite view",
                            Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(R.color.snackbar_neutral)).show();
        }
    }

    /** Pre-compute AI results for all predefined zones */
    private void runAiOnAllZones() {
        if (stressZones == null || aiModel == null) return;
        for (StressZone zone : stressZones) {
            if (!zone.hasAiResult()) {
                float[] features = FeatureExtractor.fromNdviOnly(zone.getNdvi());
                zone.setAiResult(aiModel.predict(features));
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────

    private void showFetchingState(LatLng latLng) {
        binding.prototypeLabel.setText("🛰️ Fetching satellite NDVI...");
        Snackbar.make(binding.getRoot(),
                "🛰️ Requesting NDVI at " + String.format("%.4f, %.4f", latLng.latitude, latLng.longitude),
                Snackbar.LENGTH_SHORT).show();
    }

    private void hideFetchingState() {
        binding.prototypeLabel.setText(
                aiModel != null && aiModel.isLoaded()
                        ? "🧠 AI Model Active · Tap map to analyse"
                        : "⚠️ AI model unavailable — rule-based mode");
    }

    private void showLegendCard() {
        binding.legendCard.setVisibility(View.VISIBLE);
        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(500); a.setFillAfter(true);
        binding.legendCard.startAnimation(a);
    }

    private void hideLegendCard() {
        AlphaAnimation a = new AlphaAnimation(1f, 0f);
        a.setDuration(300);
        a.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation a) {}
            public void onAnimationRepeat(Animation a) {}
            public void onAnimationEnd(Animation a) {
                binding.legendCard.setVisibility(View.GONE);
            }
        });
        binding.legendCard.startAnimation(a);
    }

    private void enableUserLocation() {
        if (googleMap == null) return;
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == 1001 && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED)
            enableUserLocation();
    }

    private void searchLocation(String name) {
        new Thread(() -> {
            try {
                Geocoder geo = new Geocoder(this);
                List<Address> list = geo.getFromLocationName(name, 1);
                if (list != null && !list.isEmpty()) {
                    LatLng ll = new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
                    runOnUiThread(() ->
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14f)));
                } else {
                    runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                            "Location not found", Snackbar.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(binding.getRoot(),
                        "Search failed", Snackbar.LENGTH_SHORT).show());
            }
        }).start();

        // Hide keyboard
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    /** Estimate canopy temperature from NDVI (inverse correlation) */
    private double estimateTemperature(double ndvi) {
        // Healthy crop (NDVI=0.8) → ~28°C; bare soil (NDVI=0.1) → ~42°C
        return 42.0 - (ndvi * 17.5);
    }

    // ─────────────────────────────────────────────────────────
    // MENU
    // ─────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_info) {
            boolean loaded = aiModel != null && aiModel.isLoaded();
            Snackbar.make(binding.getRoot(),
                    loaded
                            ? "🧠 AI MLP (8→64→32→16→4) · 3,252 params · 99% accuracy\n" +
                            "Features: NDVI · NDMI · Red-Edge NDVI · Thermal IR"
                            : "Rule-based classifier active (model not loaded)",
                    Snackbar.LENGTH_LONG).show();
            return true;
        }

        if (id == R.id.menu_reset) {
            if (isStressVisionActive) toggleStressVision();
            if (currentCircle != null) { currentCircle.remove(); currentCircle = null; }
            binding.aiStatusCard.setVisibility(View.GONE);
            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(FARM_CENTER, DEFAULT_ZOOM));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
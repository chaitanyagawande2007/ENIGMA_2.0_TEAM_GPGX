package com.orbital.stressvision;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.material.snackbar.Snackbar;

import com.orbital.stressvision.databinding.ActivityMainBinding;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity.java
 * ─────────────────────────────────────────────────────────────
 * Main screen of Stress-Vision app.
 *
 * Features:
 *  • Full-screen Google Maps in Satellite mode
 *  • FAB to toggle Stress-Vision overlay on/off
 *  • Bottom legend card showing color classification
 *  • Polygon click → opens ZoneDetailActivity with info popup
 *  • AppBar with menu for info and settings
 * ─────────────────────────────────────────────────────────────
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // ViewBinding reference
    private ActivityMainBinding binding;

    private com.google.android.gms.maps.model.Circle currentCircle;

    // Google Map reference
    private GoogleMap googleMap;

    // Stress overlay state
    private boolean isStressVisionActive = false;

    // Drawn polygon references (kept for removal)
    private List<Polygon> stressPolygons = new ArrayList<>();

    // Sample zone data (simulated)
    private List<StressZone> stressZones;

    // Default camera position — farmland near Nagpur, India
    private static final LatLng FARM_CENTER = new LatLng(21.1455, 79.0892);
    private static final float  DEFAULT_ZOOM = 14.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Inflate layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up AppBar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Orbital Agronomy – Stress Vision");
        }



        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
            getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up FAB click listener
        binding.fabStressVision.setVisibility(View.GONE);

        // Legend is hidden by default
        binding.legendCard.setVisibility(View.GONE);

        // Prototype label
        binding.prototypeLabel.setText("Prototype rule-based stress detection model.");
    }

    // ─────────────────────────────────────────────────────────
    // Google Maps callback
    // ─────────────────────────────────────────────────────────

    @Override
    public void onMapReady(GoogleMap map) {

        this.googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(FARM_CENTER, DEFAULT_ZOOM));

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // 🔥 REAL CLICK ANYWHERE NDVI
        googleMap.setOnMapClickListener(latLng -> {

            Snackbar.make(binding.getRoot(),
                    "Fetching satellite NDVI...",
                    Snackbar.LENGTH_SHORT).show();

            fetchNdviFromServer(latLng.latitude, latLng.longitude);
        });
    }

    private void fetchNdviFromServer(double lat, double lng) {

        new Thread(() -> {

            try {

                String urlString =
                        "https://spencer-unmutualised-biweekly.ngrok-free.dev/ndvi?lat="
                                + lat + "&lng=" + lng;

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(urlString)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new Exception("Server error");
                }

                String body = response.body().string();

                org.json.JSONObject json = new org.json.JSONObject(body);

                String status = json.getString("status");

                if (!status.equals("ok")) {
                    throw new Exception("No satellite data");
                }

                double ndvi = json.getDouble("ndvi");

                runOnUiThread(() -> {
                    showNdviPopup(ndvi);
                    drawStressCircle(lat, lng, ndvi);
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Snackbar.make(binding.getRoot(),
                                "Satellite data unavailable",
                                Snackbar.LENGTH_LONG).show()
                );
            }

        }).start();
    }

    private void drawStressCircle(double lat, double lng, double ndvi) {

        if (currentCircle != null) {
            currentCircle.remove();
        }

        int color;

        if (ndvi < 0.3) {
            color = android.graphics.Color.argb(140, 239, 68, 68);
        } else if (ndvi < 0.5) {
            color = android.graphics.Color.argb(140, 250, 204, 21);
        } else {
            color = android.graphics.Color.argb(140, 34, 197, 94);
        }

        currentCircle = googleMap.addCircle(
                new com.google.android.gms.maps.model.CircleOptions()
                        .center(new LatLng(lat, lng))
                        .radius(70)
                        .fillColor(color)
                        .strokeColor(color)
                        .strokeWidth(3f)
        );
    }

    private void showNdviPopup(double ndvi){

        String label;
        String desc;
        int color;

        if(ndvi < 0.3){
            label = "Low Vegetation";
            desc = "Possible water stress detected";
            color = android.graphics.Color.rgb(239,68,68);
        }else if(ndvi < 0.5){
            label = "Moderate Vegetation";
            desc = "Crop health should be monitored";
            color = android.graphics.Color.rgb(250,204,21);
        }else{
            label = "Healthy Vegetation";
            desc = "Vegetation density is strong";
            color = android.graphics.Color.rgb(34,197,94);
        }

        Snackbar.make(binding.getRoot(),
                        "NDVI: " + String.format("%.2f", ndvi)
                                + "\n" + label
                                + "\n" + desc,
                        Snackbar.LENGTH_LONG)
                .setBackgroundTint(color)
                .setTextColor(android.graphics.Color.WHITE)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    // Stress Vision Toggle
    // ─────────────────────────────────────────────────────────

    /**
     * Toggles the Stress-Vision overlay on/off.
     * First click: shows colored stress polygons + legend.
     * Second click: removes overlays.
     */
    private void toggleStressVision() {
        if (googleMap == null) {
            Snackbar.make(binding.getRoot(), "Map is still loading...", Snackbar.LENGTH_SHORT).show();
            return;
        }

        isStressVisionActive = !isStressVisionActive;

        if (isStressVisionActive) {
            // ── ENABLE STRESS VISION ────────────────────────
            enableStressVision();
        } else {
            // ── DISABLE STRESS VISION ───────────────────────
            disableStressVision();
        }
    }

    private void enableStressVision() {

        // NDVI based zones draw होंगे (real colours)
        stressPolygons = MapUtils.drawStressOverlay(googleMap, stressZones);

        showLegendCard();

        binding.fabStressVision.setIcon(
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye_off)
        );
        binding.fabStressVision.setContentDescription("Disable Stress Vision");

        Snackbar.make(
                        binding.getRoot(),
                        "⚡ Stress-Vision ACTIVE — NDVI zones loaded",
                        Snackbar.LENGTH_LONG
                )
                .setBackgroundTint(getColor(R.color.snackbar_success))
                .setTextColor(getColor(android.R.color.white))
                .show();
    }
    private void disableStressVision() {
        // Remove all polygons
        MapUtils.clearStressOverlay(stressPolygons);

        // Hide legend card
        hideLegendCard();

        // Restore FAB icon
        binding.fabStressVision.setIcon(
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye)
        );
        binding.fabStressVision.setContentDescription("Enable Stress Vision");

        // Show snackbar
        Snackbar.make(
            binding.getRoot(),
            "\uD83C\uDF0D Stress-Vision OFF — standard satellite view",
            Snackbar.LENGTH_SHORT
        )
        .setBackgroundTint(getColor(R.color.snackbar_neutral))
        .show();
    }

    // ─────────────────────────────────────────────────────────
    // Legend Card Animations
    // ─────────────────────────────────────────────────────────

    private void showLegendCard() {
        binding.legendCard.setVisibility(View.VISIBLE);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.setFillAfter(true);
        binding.legendCard.startAnimation(fadeIn);
    }

    private void hideLegendCard() {
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.setFillAfter(false);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                binding.legendCard.setVisibility(View.GONE);
            }
        });
        binding.legendCard.startAnimation(fadeOut);
    }

    // ─────────────────────────────────────────────────────────
    // Zone Detail
    // ─────────────────────────────────────────────────────────

    /**
     * Opens ZoneDetailActivity with full info about the clicked zone.
     *
     * @param zoneId ID of the tapped polygon zone
     */
    private void openZoneDetail(String zoneId) {
        StressZone zone = MapUtils.findZoneById(stressZones, zoneId);
        if (zone == null) return;

        StressResult result = StressCalculator.classify(zone.getNdvi(), zone.getTemperature());

        Intent intent = new Intent(this, ZoneDetailActivity.class);
        intent.putExtra(ZoneDetailActivity.EXTRA_ZONE_NAME,   zone.getName());
        intent.putExtra(ZoneDetailActivity.EXTRA_NDVI,        zone.getNdvi());
        intent.putExtra(ZoneDetailActivity.EXTRA_TEMPERATURE, zone.getTemperature());
        intent.putExtra(ZoneDetailActivity.EXTRA_STRESS_LABEL, result.getLabel());
        intent.putExtra(ZoneDetailActivity.EXTRA_STRESS_DESC,  result.getDescription());
        intent.putExtra(ZoneDetailActivity.EXTRA_STRESS_EMOJI, result.getEmoji());
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
    }

    // ─────────────────────────────────────────────────────────
    // Menu
    // ─────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_info) {
            Snackbar.make(
                binding.getRoot(),
                "Prototype rule-based stress detection. No AI used.",
                Snackbar.LENGTH_LONG
            ).show();
            return true;
        }
        if (item.getItemId() == R.id.menu_reset) {
            if (isStressVisionActive) {
                toggleStressVision();
            }
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(FARM_CENTER, DEFAULT_ZOOM)
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

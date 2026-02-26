package com.orbital.stressvision;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import androidx.appcompat.app.AppCompatActivity;
import android.location.Address;
import android.location.Geocoder;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.material.snackbar.Snackbar;

import com.orbital.stressvision.databinding.ActivityMainBinding;

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

    // Location client for GPS
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    // ViewBinding reference
    private ActivityMainBinding binding;

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

        // Load sample zone data
        stressZones = MapUtils.getSampleZones();

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment)
            getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up FAB click listener
        binding.fabStressVision.setOnClickListener(v -> toggleStressVision());

        // Legend is hidden by default
        binding.legendCard.setVisibility(View.GONE);

        // Prototype label
        binding.prototypeLabel.setText("Prototype rule-based stress detection model.");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupSearch();
        binding.btnMyLocation.setOnClickListener(v -> goToMyLocation());
           }

    // ─────────────────────────────────────────────────────────
    // Google Maps callback
    // ─────────────────────────────────────────────────────────

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        // Set satellite map type for real-world crop field view
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // Move camera to farm area
        // Go to user's location on startup instead of Nagpur
        goToMyLocation();

        // Disable some UI controls for cleaner look
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Set polygon click listener
        googleMap.setOnPolygonClickListener(polygon -> {
            String zoneId = (String) polygon.getTag();
            if (zoneId != null) {
                openZoneDetail(zoneId);
            }
        });
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
        // Draw polygons on map
        stressPolygons = MapUtils.drawStressOverlay(googleMap, stressZones);

        // Animate legend card appearance
        showLegendCard();

        // Update FAB appearance
        binding.fabStressVision.setIcon(
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye_off)
        );
        binding.fabStressVision.setContentDescription("Disable Stress Vision");

        // Show snackbar confirmation
        Snackbar.make(
            binding.getRoot(),
            "\u26A1 Stress-Vision ACTIVE — " + stressZones.size() + " zones analysed",
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
                "Prototype rule-based stress detection.",
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

    // ─────────────────────────────────────────────────────────
// Search Location
// ─────────────────────────────────────────────────────────

    private void setupSearch() {

        // Show/hide clear button as user types
        binding.searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.searchClear.setVisibility(
                        s.length() > 0 ? View.VISIBLE : View.GONE
                );
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Clear button click
        binding.searchClear.setOnClickListener(v -> {
            binding.searchInput.setText("");
            binding.searchClear.setVisibility(View.GONE);
        });

        // When user presses Search on keyboard
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchLocation(query);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Searches for a location by name using Geocoder
     * and moves the map camera there.
     */
    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Move camera to searched location
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(latLng, 14.0f)
                );

                // Hide keyboard
                hideKeyboard();

                // Show confirmation
                Snackbar.make(
                        binding.getRoot(),
                        "📍 Moved to: " + address.getAddressLine(0),
                        Snackbar.LENGTH_SHORT
                ).show();

            } else {
                Snackbar.make(
                        binding.getRoot(),
                        "❌ Location not found. Try a different name.",
                        Snackbar.LENGTH_SHORT
                ).show();
            }

        } catch (IOException e) {
            Snackbar.make(
                    binding.getRoot(),
                    "❌ Search failed. Check internet connection.",
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }

// ─────────────────────────────────────────────────────────
// My Location (GPS)
// ─────────────────────────────────────────────────────────

    private void goToMyLocation() {

        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Enable blue dot on map
        googleMap.setMyLocationEnabled(true);

        // Get last known location and move camera there
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(userLatLng, 15.0f)
                );
                Snackbar.make(binding.getRoot(),
                        "📍 Moved to your location", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(binding.getRoot(),
                        "⚠️ Could not get location. Move outside or check GPS.",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                goToMyLocation();
            } else {
                Snackbar.make(binding.getRoot(),
                        "Location permission denied. Search manually instead.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

// ─────────────────────────────────────────────────────────
// Hide Keyboard Helper
// ─────────────────────────────────────────────────────────

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(
                    getCurrentFocus().getWindowToken(), 0);
        }
    }

}

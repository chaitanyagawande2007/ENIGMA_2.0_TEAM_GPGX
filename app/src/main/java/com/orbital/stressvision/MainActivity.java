package com.orbital.stressvision;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.material.snackbar.Snackbar;
import com.orbital.stressvision.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMainBinding binding;
    private GoogleMap googleMap;

    private boolean isStressVisionActive = false;

    private List<Polygon> stressPolygons = new ArrayList<>();
    private List<StressZone> stressZones = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // --------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment)getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment);

        if(mapFragment!=null) mapFragment.getMapAsync(this);

        binding.legendCard.setVisibility(android.view.View.GONE);

        binding.fabStressVision.setOnClickListener(v -> toggleStressVision());
        binding.btnMyLocation.setOnClickListener(v -> goToMyLocation());

        setupSearch();
    }

    // --------------------------------------------------
    @Override
    public void onMapReady(GoogleMap map) {

        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        goToMyLocation();

        // TAP MAP → CREATE ANALYSIS GRID
        googleMap.setOnMapClickListener(latLng -> {
            if(isStressVisionActive)
                refreshStressLayerAt(latLng);
        });

        // ✅ CITY / ZONE CLICK → ZOOM + OPEN DETAIL
        googleMap.setOnPolygonClickListener(polygon -> {

            zoomToPolygon(polygon);   // NEW LOGIC

            String zoneId = (String) polygon.getTag();
            if(zoneId!=null)
                openZoneDetail(zoneId);
        });
    }

    // --------------------------------------------------
    // ZOOM TO CLICKED CITY / POLYGON
    // --------------------------------------------------
    private void zoomToPolygon(Polygon polygon){

        if(googleMap==null || polygon==null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for(LatLng point : polygon.getPoints()){
            builder.include(point);
        }

        LatLngBounds bounds = builder.build();

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,150)
        );
    }

    // --------------------------------------------------
    private void refreshStressLayerAt(LatLng center){

        if(googleMap==null) return;

        MapUtils.clearStressOverlay(stressPolygons);

        stressZones =
                MapUtils.generateZonesAroundPoint(center);

        stressPolygons =
                MapUtils.drawStressOverlay(googleMap, stressZones);
    }

    // --------------------------------------------------
    private void toggleStressVision(){

        isStressVisionActive=!isStressVisionActive;

        if(isStressVisionActive){

            binding.fabStressVision.setIcon(getDrawable(R.drawable.ic_eye_off));
            showLegendCard();

            Snackbar.make(binding.getRoot(),
                    "Tap anywhere to analyse stress",
                    Snackbar.LENGTH_SHORT).show();

        }else{

            MapUtils.clearStressOverlay(stressPolygons);
            hideLegendCard();

            binding.fabStressVision.setIcon(getDrawable(R.drawable.ic_eye));
        }
    }

    // --------------------------------------------------
    private void setupSearch(){
        binding.searchInput.setOnEditorActionListener((v,actionId,event)->{
            if(actionId== EditorInfo.IME_ACTION_SEARCH){
                searchLocation(binding.searchInput.getText().toString());
                return true;
            }
            return false;
        });
    }

    // ✅ SEARCH CITY → AUTO ZOOM
    private void searchLocation(String name){

        if(googleMap==null) return;

        try{
            Geocoder geocoder=new Geocoder(this, Locale.getDefault());
            List<Address> list=geocoder.getFromLocationName(name,1);

            if(list==null||list.isEmpty()) return;

            LatLng latLng=new LatLng(
                    list.get(0).getLatitude(),
                    list.get(0).getLongitude());

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng,14f),
                    900,
                    new GoogleMap.CancelableCallback(){
                        public void onFinish(){
                            if(isStressVisionActive)
                                refreshStressLayerAt(latLng);
                        }
                        public void onCancel(){}
                    });

            hideKeyboard();

        }catch(IOException ignored){}
    }

    // --------------------------------------------------
    private void goToMyLocation(){

        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location->{
                    if(location==null) return;

                    LatLng user=new LatLng(
                            location.getLatitude(),
                            location.getLongitude());

                    googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(user,15f));
                });
    }

    // --------------------------------------------------
    private void openZoneDetail(String zoneId){

        StressZone zone=MapUtils.findZoneById(stressZones,zoneId);
        if(zone==null) return;

        StressResult result=
                StressCalculator.classify(zone.getNdvi(),zone.getTemperature());

        Intent i=new Intent(this,ZoneDetailActivity.class);
        i.putExtra(ZoneDetailActivity.EXTRA_ZONE_NAME,zone.getName());
        i.putExtra(ZoneDetailActivity.EXTRA_NDVI,zone.getNdvi());
        i.putExtra(ZoneDetailActivity.EXTRA_TEMPERATURE,zone.getTemperature());
        i.putExtra(ZoneDetailActivity.EXTRA_STRESS_LABEL,result.getLabel());
        startActivity(i);
    }

    // --------------------------------------------------
    private void showLegendCard(){
        binding.legendCard.setVisibility(android.view.View.VISIBLE);
        AlphaAnimation a=new AlphaAnimation(0f,1f);
        a.setDuration(400);
        binding.legendCard.startAnimation(a);
    }

    private void hideLegendCard(){
        AlphaAnimation a=new AlphaAnimation(1f,0f);
        a.setDuration(300);
        a.setAnimationListener(new Animation.AnimationListener(){
            public void onAnimationStart(Animation animation){}
            public void onAnimationRepeat(Animation animation){}
            public void onAnimationEnd(Animation animation){
                binding.legendCard.setVisibility(android.view.View.GONE);
            }
        });
        binding.legendCard.startAnimation(a);
    }

    private void hideKeyboard(){
        InputMethodManager imm=
                (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm!=null && getCurrentFocus()!=null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
    }

    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId()==R.id.menu_reset){
            MapUtils.clearStressOverlay(stressPolygons);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
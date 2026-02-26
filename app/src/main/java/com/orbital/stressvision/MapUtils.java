package com.orbital.stressvision;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;

import java.util.*;

public class MapUtils {

    private static final float STROKE_WIDTH = 3f;

    // --------------------------------------------------
    // CREATE STABLE RANDOM FROM LOCATION
    // (Same place = Same result)
    // --------------------------------------------------
    private static Random createLocationRandom(double lat,double lng){

        long seed =
                ((long)(lat * 100000)) ^
                        ((long)(lng * 100000) << 16);

        return new Random(seed);
    }

    // --------------------------------------------------
    // GENERATE ZONES AROUND CLICKED POINT
    // --------------------------------------------------
    public static List<StressZone> generateZonesAroundPoint(LatLng center){

        List<StressZone> zones=new ArrayList<>();

        double step=0.0012; // ~120m grid
        int id=1;

        for(int x=-1;x<=1;x++){
            for(int y=-1;y<=1;y++){

                double lat=center.latitude+(x*step);
                double lng=center.longitude+(y*step);
                double half=step/2;

                // ✅ Stable random based on grid location
                Random random=createLocationRandom(lat,lng);

                List<LatLng> polygon=Arrays.asList(
                        new LatLng(lat+half,lng-half),
                        new LatLng(lat+half,lng+half),
                        new LatLng(lat-half,lng+half),
                        new LatLng(lat-half,lng-half)
                );

                // SAME OUTPUT FOREVER FOR SAME LOCATION
                double ndvi=0.25+random.nextDouble()*0.5;
                double temp=26+random.nextDouble()*14;

                zones.add(new StressZone(
                        "POINT_ZONE_"+id++,
                        "Analyzed Area",
                        polygon,
                        ndvi,
                        temp,
                        new LatLng(lat,lng)
                ));
            }
        }
        return zones;
    }

    // --------------------------------------------------
    // DRAW OVERLAY
    // --------------------------------------------------
    public static List<Polygon> drawStressOverlay(
            GoogleMap map,List<StressZone> zones){

        List<Polygon> polygons=new ArrayList<>();

        for(StressZone zone:zones){

            StressResult result=
                    StressCalculator.classify(
                            zone.getNdvi(),
                            zone.getTemperature());

            Polygon polygon=map.addPolygon(
                    new PolygonOptions()
                            .addAll(zone.getPolygon())
                            .fillColor(result.getFillColor())
                            .strokeColor(result.getStrokeColor())
                            .strokeWidth(STROKE_WIDTH)
                            .clickable(true));

            polygon.setTag(zone.getId());
            polygons.add(polygon);
        }
        return polygons;
    }

    // --------------------------------------------------
    public static void clearStressOverlay(List<Polygon> polygons){
        if(polygons==null) return;
        for(Polygon p:polygons) if(p!=null) p.remove();
        polygons.clear();
    }

    // --------------------------------------------------
    public static StressZone findZoneById(List<StressZone> zones,String id){
        for(StressZone z:zones)
            if(z.getId().equals(id)) return z;
        return null;
    }
}
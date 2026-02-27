package com.orbital.stressvision;

import com.google.android.gms.maps.model.LatLng;
import com.orbital.stressvision.ai.PredictionResult;

import java.util.List;

/**
 * StressZone.java
 * ─────────────────────────────────────────────────────────────
 * Data model for a single agricultural field zone.
 * Now carries the full AI PredictionResult in addition to
 * the original NDVI/temperature fields.
 * ─────────────────────────────────────────────────────────────
 */
public class StressZone {

    private final String        id;
    private final String        name;
    private final List<LatLng>  polygon;
    private final double        ndvi;
    private final double        temperature;
    private final LatLng        center;

    // AI result (set after model inference)
    private PredictionResult aiResult;

    public StressZone(String id, String name, List<LatLng> polygon,
                      double ndvi, double temperature, LatLng center) {
        this.id          = id;
        this.name        = name;
        this.polygon     = polygon;
        this.ndvi        = ndvi;
        this.temperature = temperature;
        this.center      = center;
    }

    public void setAiResult(PredictionResult result) {
        this.aiResult = result;
    }

    public String        getId()          { return id;          }
    public String        getName()        { return name;        }
    public List<LatLng>  getPolygon()     { return polygon;     }
    public double        getNdvi()        { return ndvi;        }
    public double        getTemperature() { return temperature; }
    public LatLng        getCenter()      { return center;      }
    public PredictionResult getAiResult() { return aiResult;    }
    public boolean       hasAiResult()    { return aiResult != null; }
}
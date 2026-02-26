package com.orbital.stressvision;

import android.graphics.Color;

public class LandClassifier {

    public enum LandType {
        TREES,
        MIXED,
        BUILDINGS
    }

    // deterministic vegetation calculation (NO RANDOM)
    public static double calculateVegetation(double lat,double lng){

        double seed =
                Math.sin(lat * 12.9898 + lng * 78.233) * 43758.5453;

        seed = seed - Math.floor(seed); // normalize 0–1

        return seed; // vegetation ratio
    }

    public static LandType classify(double vegetation){

        if(vegetation >= 0.65)
            return LandType.TREES;

        if(vegetation >= 0.35)
            return LandType.MIXED;

        return LandType.BUILDINGS;
    }

    public static int getFillColor(LandType type){

        switch(type){

            case TREES:
                return Color.argb(140,0,200,0);     // GREEN

            case MIXED:
                return Color.argb(140,255,165,0);   // ORANGE

            default:
                return Color.argb(140,220,0,0);     // RED
        }
    }

    public static int getStrokeColor(LandType type){

        switch(type){
            case TREES: return Color.rgb(0,120,0);
            case MIXED: return Color.rgb(180,110,0);
            default: return Color.rgb(140,0,0);
        }
    }
}
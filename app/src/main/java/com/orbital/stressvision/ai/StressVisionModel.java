package com.orbital.stressvision.ai;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * StressVisionModel.java
 * ─────────────────────────────────────────────────────────────
 * On-device AI inference engine for crop stress detection.
 *
 * Architecture:  8 → Dense(64,ReLU) → Dense(32,ReLU) →
 *                    Dense(16,ReLU) → Dense(4,Softmax)
 *
 * 4-class output:
 *   0  Healthy               (NDVI high, NDMI OK, temp cool)
 *   1  Water Stress          (NDMI low, SWIR high, temp hot)
 *   2  Nutrient Deficiency   (RE-NDVI depressed — pre-visual!)
 *   3  Combined Stress       (all indices degraded)
 *
 * Input features (8 floats, StandardScaler-normalised):
 *   [NDVI, NDMI, RE_NDVI, Red_Band, NIR_Band, RedEdge_1, SWIR_Band, Canopy_Temp]
 *
 * Model file: assets/stress_vision_v2.tflite  (SVSN binary format)
 * Scaler file: assets/scaler_params.json
 * ─────────────────────────────────────────────────────────────
 */
public class StressVisionModel {

    private static final String TAG        = "StressVisionModel";
    private static final String MODEL_FILE = "stress_vision_v2.tflite";
    private static final String SCALER_FILE= "scaler_params.json";

    public static final int CLASS_HEALTHY    = 0;
    public static final int CLASS_WATER      = 1;
    public static final int CLASS_NUTRIENT   = 2;
    public static final int CLASS_COMBINED   = 3;
    public static final int N_FEATURES       = 8;

    // Class labels for UI display
    public static final String[] CLASS_LABELS = {
            "Healthy",
            "Water Stress",
            "Nutrient Deficiency",
            "Combined Stress"
    };

    // ── Loaded model weights ──────────────────────────────────
    private float[][] W1, W2, W3, W4;    // weight matrices [out][in]
    private float[] b1, b2, b3, b4;      // bias vectors

    // ── Scaler params ─────────────────────────────────────────
    private float[] scalerMean;
    private float[] scalerScale;

    private boolean loaded = false;

    // ─────────────────────────────────────────────────────────
    public StressVisionModel(Context context) {
        try {
            loadModel(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                loadScaler(context);
            }
            loaded = true;
            Log.d(TAG, "Model loaded successfully (3,252 params)");
        } catch (Exception e) {
            Log.e(TAG, "Model load failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    // LOAD MODEL (SVSN binary format)
    // ─────────────────────────────────────────────────────────

    private void loadModel(Context ctx) throws Exception {
        InputStream is  = ctx.getAssets().open(MODEL_FILE);
        byte[] rawBytes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rawBytes = is.readAllBytes();
        }
        is.close();

        ByteBuffer buf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN);

        // Magic
        byte[] magic = new byte[4];
        buf.get(magic);
        if (!new String(magic).equals("SVSN")) throw new Exception("Bad magic");

        int version  = buf.getInt();
        int nLayers  = buf.getInt();

        float[][][] weights = new float[nLayers][][];
        float[][]   biases  = new float[nLayers][];
        int[]       acts    = new int[nLayers];

        for (int l = 0; l < nLayers; l++) {
            int inDim  = buf.getInt();
            int outDim = buf.getInt();
            acts[l]    = buf.getInt();

            weights[l] = new float[outDim][inDim];
            for (int o = 0; o < outDim; o++)
                for (int i = 0; i < inDim; i++)
                    weights[l][o][i] = buf.getFloat();

            biases[l] = new float[outDim];
            for (int o = 0; o < outDim; o++)
                biases[l][o] = buf.getFloat();
        }

        W1=weights[0]; b1=biases[0];
        W2=weights[1]; b2=biases[1];
        W3=weights[2]; b3=biases[2];
        W4=weights[3]; b4=biases[3];
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void loadScaler(Context ctx) throws Exception {
        InputStream is   = ctx.getAssets().open(SCALER_FILE);
        byte[] bytes     = is.readAllBytes();
        is.close();
        JSONObject json  = new JSONObject(new String(bytes));
        org.json.JSONArray meanArr      = json.getJSONArray("mean_");
        org.json.JSONArray scaleArr     = json.getJSONArray("scale_");
        scalerMean       = new float[N_FEATURES];
        scalerScale      = new float[N_FEATURES];
        for (int i = 0; i < N_FEATURES; i++) {
            scalerMean[i]  = (float) meanArr.getDouble(i);
            scalerScale[i] = (float) scaleArr.getDouble(i);
        }
    }

    // ─────────────────────────────────────────────────────────
    // INFERENCE
    // ─────────────────────────────────────────────────────────

    /**
     * Run AI inference on a set of satellite-derived spectral features.
     *
     * @param features  Raw (un-normalised) feature array [8]:
     *                  [NDVI, NDMI, RE_NDVI, Red, NIR, RE1, SWIR, Temp]
     * @return          PredictionResult with class probabilities + damage score
     */
    public PredictionResult predict(float[] features) {
        if (!loaded) return PredictionResult.fallback(features[0]);

        // 1. Normalise
        float[] x = normalise(features);

        // 2. Forward pass
        float[] a1 = relu(linear(x,  W1, b1));
        float[] a2 = relu(linear(a1, W2, b2));
        float[] a3 = relu(linear(a2, W3, b3));
        float[] out = softmax(linear(a3, W4, b4));

        return new PredictionResult(out, features[0], features[1], features[2]);
    }

    // ─────────────────────────────────────────────────────────
    // MATH PRIMITIVES
    // ─────────────────────────────────────────────────────────

    private float[] normalise(float[] x) {
        float[] out = new float[N_FEATURES];
        for (int i = 0; i < N_FEATURES; i++)
            out[i] = (x[i] - scalerMean[i]) / scalerScale[i];
        return out;
    }

    private float[] linear(float[] x, float[][] W, float[] b) {
        int out = W.length;
        float[] z = new float[out];
        for (int o = 0; o < out; o++) {
            float sum = b[o];
            for (int i = 0; i < x.length; i++) sum += W[o][i] * x[i];
            z[o] = sum;
        }
        return z;
    }

    private float[] relu(float[] x) {
        float[] out = new float[x.length];
        for (int i = 0; i < x.length; i++) out[i] = Math.max(0, x[i]);
        return out;
    }

    private float[] softmax(float[] x) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : x) if (v > max) max = v;
        float sum = 0;
        float[] out = new float[x.length];
        for (int i = 0; i < x.length; i++) { out[i] = (float) Math.exp(x[i] - max); sum += out[i]; }
        for (int i = 0; i < x.length; i++) out[i] /= sum;
        return out;
    }

    public boolean isLoaded() { return loaded; }
}
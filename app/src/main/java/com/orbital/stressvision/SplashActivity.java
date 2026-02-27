package com.orbital.stressvision;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import androidx.appcompat.app.AppCompatActivity;

import com.orbital.stressvision.databinding.ActivitySplashBinding;

/**
 * SplashActivity.java
 * ─────────────────────────────────────────────────────────────
 * Launch screen displayed for 2.5 seconds before MainActivity.
 * Shows app logo with fade + scale animation.
 * ─────────────────────────────────────────────────────────────
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2500;
    private ActivitySplashBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Animate logo and tagline
        animateSplashContent();

        // Navigate to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION_MS);
    }

    /**
     * Animates the splash screen elements with fade-in + scale effect.
     */
    private void animateSplashContent() {
        // Scale animation
        ScaleAnimation scale = new ScaleAnimation(
            0.7f, 1.0f,  // Scale X from 70% to 100%
            0.7f, 1.0f,  // Scale Y from 70% to 100%
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(700);

        // Fade animation
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(800);

        // Combine animations
        AnimationSet animSet = new AnimationSet(false);
        animSet.addAnimation(scale);
        animSet.addAnimation(fade);
        animSet.setFillAfter(true);

        binding.splashLogo.startAnimation(animSet);

        // Animate tagline slightly delayed
        AlphaAnimation taglineFade = new AlphaAnimation(0f, 1f);
        taglineFade.setDuration(800);
        taglineFade.setStartOffset(500);
        taglineFade.setFillAfter(true);
        binding.splashTagline.startAnimation(taglineFade);
        binding.splashSubtitle.startAnimation(taglineFade);
    }
}

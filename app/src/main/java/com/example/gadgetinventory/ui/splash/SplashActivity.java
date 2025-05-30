package com.example.gadgetinventory.ui.splash;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoImageView = findViewById(R.id.logoImageView);

        // Create animations
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logoImageView, View.SCALE_X, 0.0f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logoImageView, View.SCALE_Y, 0.0f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(logoImageView, View.ALPHA, 0.0f, 1.0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(1000);
        animatorSet.start();

        // Navigate to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            // Apply fade-out transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DELAY);
    }
} 
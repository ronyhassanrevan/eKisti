package com.pearttechnology.ekisti;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String WELCOME_TEXT = "Welcome To eKisti Microfinance Software";
    private ImageView appIcon;
    private TextView welcomeText;
    private Handler handler = new Handler();
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize and preload WebView
        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Ignore SSL certificate errors
                handler.proceed();
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://demo.ekisti.com"); // Replace with your URL

        // Find views
        appIcon = findViewById(R.id.app_icon);
        welcomeText = findViewById(R.id.welcome_text);

        // Load the animations
        Animation zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        Animation zoomOutAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        // Apply zoom in animation to the app icon
        appIcon.startAnimation(zoomInAnimation);

        // Show welcome text during zoom animation
        zoomInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                welcomeText.setVisibility(View.VISIBLE);
                startTextAnimation(welcomeText, WELCOME_TEXT);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Keep the app icon visible
                appIcon.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // Start MainActivity after a delay
        handler.postDelayed(() -> {
            appIcon.startAnimation(zoomOutAnimation);
            welcomeText.startAnimation(fadeOutAnimation);

            zoomOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    intent.putExtra("preloaded_webview", true);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    welcomeText.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }, 3000); // Delay to ensure the splash screen is visible for a certain time
    }

    private void startTextAnimation(TextView textView, String text) {
        long delay = 50; // milliseconds delay for each character
        for (int i = 0; i < text.length(); i++) {
            final int index = i;
            handler.postDelayed(() -> textView.setText(text.substring(0, index + 1)), delay * i);
        }
    }
}

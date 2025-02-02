package com.gratus.meditationtrakcer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import android.os.Build;
import android.graphics.Color;  // Added missing import

public class BaseActivity extends AppCompatActivity {

    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure status bar does not overlap the action bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));

            // Ensure the toolbar is positioned correctly below the status bar
            //View decorView = getWindow().getDecorView();
            //decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    // Disable all activity transition animations
    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        overridePendingTransition(0, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    // Toolbar and Menu Button Setup
    protected void setupToolbar(int toolbarId, int menuButtonId) {
        Toolbar toolbar = findViewById(toolbarId);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        Button menuButton = findViewById(menuButtonId);
        menuButton.setOnClickListener(v -> {
            if (this.getClass().getSimpleName().equals("MainActivity")) {
                if (isMenuOpen) {
                    finish(); // Close the menu
                } else {
                    openMenu();
                }
            } else {
                openMenu();
            }
        });
    }

    // Open Menu without animation
    protected void openMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);  // Disable transitions
        isMenuOpen = true;
    }
}

package com.safetour.ai;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.safetour.ai.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("safe_tour_prefs", android.content.Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        if (isDark) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Edge-To-Edge Screen Real Estate
        int flags = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (!isDark) {
            flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;

        // Active Icon Tint Color
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // checked
                new int[] { -android.R.attr.state_checked } // unchecked
        };

        int[] colors = new int[] {
                Color.parseColor("#007AFF"), // Electric Blue
                Color.parseColor("#9E9E9E")  // Grey
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);
        navView.setItemIconTintList(colorStateList);
        navView.setItemTextColor(colorStateList);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                if (destId == R.id.navigation_splash || destId == R.id.navigation_login || destId == R.id.navigation_setup) {
                    navView.setVisibility(View.GONE);
                } else {
                    navView.setVisibility(View.VISIBLE);
                    // Sync active tab
                    if (navView.getMenu().findItem(destId) != null) {
                        navView.getMenu().findItem(destId).setChecked(true);
                    }
                }
            });

            // Smooth Transitions for Bottom Nav
            navView.setOnItemSelectedListener(item -> {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setEnterAnim(R.anim.fade_through_in)
                        .setExitAnim(R.anim.fade_through_out)
                        .setPopEnterAnim(R.anim.fade_through_in)
                        .setPopExitAnim(R.anim.fade_through_out)
                        .setPopUpTo(R.id.navigation_home, false)
                        .build();

                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    navController.navigate(R.id.navigation_home, null, options);
                    return true;
                } else if (itemId == R.id.navigation_explore) {
                    navController.navigate(R.id.navigation_explore, null, options);
                    return true;
                } else if (itemId == R.id.navigation_assistant) {
                    navController.navigate(R.id.navigation_assistant, null, options);
                    return true;
                } else if (itemId == R.id.navigation_track) {
                    navController.navigate(R.id.navigation_track, null, options);
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    navController.navigate(R.id.navigation_profile, null, options);
                    return true;
                }
                return false;
            });
        }
    }
}

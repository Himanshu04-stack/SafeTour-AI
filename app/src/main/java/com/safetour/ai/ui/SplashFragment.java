package com.safetour.ai.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.safetour.ai.R;

public class SplashFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        View vRouteRed = view.findViewById(R.id.vRouteRed);
        View vRouteYellow = view.findViewById(R.id.vRouteYellow);
        View vRouteGreen = view.findViewById(R.id.vRouteGreen);
        ImageView ivLogo = view.findViewById(R.id.ivLogo);
        TextView tvAppName = view.findViewById(R.id.tvAppName);
        TextView tvStatus = view.findViewById(R.id.tvStatus);

        // Stage 1: Bad Routes Slide In
        ObjectAnimator redIn = ObjectAnimator.ofFloat(vRouteRed, "alpha", 0f, 1f);
        ObjectAnimator redMove = ObjectAnimator.ofFloat(vRouteRed, "translationY", 300f, 0f);
        ObjectAnimator yellowIn = ObjectAnimator.ofFloat(vRouteYellow, "alpha", 0f, 1f);
        ObjectAnimator yellowMove = ObjectAnimator.ofFloat(vRouteYellow, "translationY", 300f, 0f);
        
        AnimatorSet badRoutes = new AnimatorSet();
        badRoutes.playTogether(redIn, redMove, yellowIn, yellowMove);
        badRoutes.setDuration(1000);
        badRoutes.setInterpolator(new AccelerateDecelerateInterpolator());

        // Stage 2: Bad Routes fade, Green Route Slides In
        ObjectAnimator redOut = ObjectAnimator.ofFloat(vRouteRed, "alpha", 1f, 0.15f);
        ObjectAnimator yellowOut = ObjectAnimator.ofFloat(vRouteYellow, "alpha", 1f, 0.15f);
        ObjectAnimator greenIn = ObjectAnimator.ofFloat(vRouteGreen, "alpha", 0f, 1f);
        ObjectAnimator greenMove = ObjectAnimator.ofFloat(vRouteGreen, "translationY", 400f, 0f);
        
        AnimatorSet safeRoute = new AnimatorSet();
        safeRoute.playTogether(redOut, yellowOut, greenIn, greenMove);
        safeRoute.setDuration(800);
        safeRoute.setStartDelay(200);

        // Stage 3: Logo Reveal and Pulse
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.3f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.3f, 1f);
        ObjectAnimator textAlpha = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f);
        ObjectAnimator routesOut = ObjectAnimator.ofFloat(vRouteGreen, "alpha", 1f, 0f);
        
        AnimatorSet logoReveal = new AnimatorSet();
        logoReveal.playTogether(logoAlpha, logoScaleX, logoScaleY, textAlpha, routesOut,
                ObjectAnimator.ofFloat(vRouteRed, "alpha", 0.15f, 0f),
                ObjectAnimator.ofFloat(vRouteYellow, "alpha", 0.15f, 0f));
        logoReveal.setDuration(1000);
        logoReveal.setStartDelay(400);
        logoReveal.setInterpolator(new OvershootInterpolator(1.2f));

        // Sequence
        AnimatorSet master = new AnimatorSet();
        master.playSequentially(badRoutes, safeRoute, logoReveal);
        master.start();

        // Dialogue Tracker
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && tvStatus != null) tvStatus.setText("Safe route acquired.");
        }, 1500);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && tvStatus != null) {
                tvStatus.setText("Securing connection...");
                tvStatus.animate().alpha(0f).setStartDelay(500).setDuration(500).start();
            }
        }, 2800);

        // Auto-navigate to Login after animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getView() != null) {
                NavHostFragment.findNavController(this).navigate(R.id.action_splash_to_login);
            }
        }, 4500);
    }
}

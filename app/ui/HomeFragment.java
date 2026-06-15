package com.safetour.ai.ui;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.safetour.ai.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sosRunnable;
    private boolean isSosPressed = false;
    private AnimatorSet pulseAnimation;

    private static final int PERMISSION_REQUEST_CODE = 100;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupSosButton();
        requestPermissions();

        return root;
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        };
        boolean needRequest = false;
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (needRequest) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void setupSosButton() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.pulseRing, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.pulseRing, "scaleY", 1f, 1.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(binding.pulseRing, "alpha", 1f, 0f);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        pulseAnimation = new AnimatorSet();
        pulseAnimation.playTogether(scaleX, scaleY, alpha);
        pulseAnimation.setDuration(1000);

        sosRunnable = () -> {
            if (isSosPressed) {
                triggerSos();
                binding.pulseRing.setVisibility(View.INVISIBLE);
                if (pulseAnimation.isRunning()) {
                    pulseAnimation.cancel();
                }
            }
        };

        binding.btnSos.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isSosPressed = true;
                    binding.pulseRing.setVisibility(View.VISIBLE);
                    pulseAnimation.start();
                    vibrate(50);
                    handler.postDelayed(sosRunnable, 3000);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isSosPressed = false;
                    binding.pulseRing.setVisibility(View.INVISIBLE);
                    if (pulseAnimation.isRunning()) {
                        pulseAnimation.cancel();
                    }
                    handler.removeCallbacks(sosRunnable);
                    return true;
            }
            return false;
        });
    }

    private void vibrate(long duration) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void triggerSos() {
        vibrate(500);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    sendSms(location);
                } else {
                    Toast.makeText(getContext(), "Location unavailable. Sending SOS without location.", Toast.LENGTH_SHORT).show();
                    sendSms(null);
                }
            });
        } else {
            Toast.makeText(getContext(), "Permission denied. Sending SOS without location.", Toast.LENGTH_SHORT).show();
            sendSms(null);
        }
    }

    private void sendSms(Location location) {
        String phoneNumber = "1234567890"; // Hardcoded emergency contact
        String message = "EMERGENCY: I need help!";
        if (location != null) {
            message += " My location: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(getContext(), "SOS Sent Successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to send SOS.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

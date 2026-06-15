package com.safetour.ai.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "safetour_location_tracking";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    pushLocationToFirestore(location);
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if ("STOP_SERVICE".equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeTour AI")
                .setContentText("Actively monitoring your location in the Safe Circle.")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);

        // Instantly push a fallback heartbeat so the watcher instantly locks on even if the GPS chip takes a few seconds or is disabled.
        Location fallback = new Location("");
        fallback.setLatitude(0.0);
        fallback.setLongitude(0.0);
        pushLocationToFirestore(fallback);

        requestLocationUpdates();

        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void pushLocationToFirestore(Location location) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        
        Map<String, Object> locData = new HashMap<>();
        locData.put("latitude", location.getLatitude());
        locData.put("longitude", location.getLongitude());
        locData.put("timestamp", System.currentTimeMillis());
        locData.put("accuracy", location.getAccuracy());

        db.collection("users").document(uid).collection("live_tracking").document("current_location")
                .set(locData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location pushed to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to push location", e));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "SafeTour Live Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using bound service
    }
}

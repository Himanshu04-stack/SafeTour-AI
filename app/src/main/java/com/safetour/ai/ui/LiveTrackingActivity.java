package com.safetour.ai.ui;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.safetour.ai.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.preference.PreferenceManager;

public class LiveTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvTargetStatus;
    private Marker targetMarker;
    
    private FirebaseFirestore db;
    private ListenerRegistration locationListener;
    private String targetUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_live_tracking);

        mapView = findViewById(R.id.watcherMapView);
        tvTargetStatus = findViewById(R.id.tvTargetStatus);
        FloatingActionButton fabRecenter = findViewById(R.id.fabRecenter);

        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);

        if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix(new float[] {
                -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                 0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                 0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                 0.0f,  0.0f,  0.0f,  1.0f,   0.0f
            });
            mapView.getOverlayManager().getTilesOverlay().setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
        }

        db = FirebaseFirestore.getInstance();

        // 1. Intercept Deep Link or Direct Intent
        Uri data = getIntent().getData();
        if (getIntent().hasExtra("target_uid")) {
            targetUid = getIntent().getStringExtra("target_uid");
            if (targetUid != null && !targetUid.isEmpty()) {
                tvTargetStatus.setText("Connecting to friend's live stream...");
                checkAuthAndPermissions();
            } else {
                Toast.makeText(this, "Empty tracking ID provided.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (data != null && data.getLastPathSegment() != null && !data.getLastPathSegment().isEmpty()) {
            targetUid = data.getLastPathSegment();
            tvTargetStatus.setText("Connecting to friend's live stream...");
            checkAuthAndPermissions();
        } else {
            tvTargetStatus.setText("Error: Invalid Tracking Session.");
            Toast.makeText(this, "No valid tracking ID found.", Toast.LENGTH_LONG).show();
            finish();
        }

        fabRecenter.setOnClickListener(v -> {
            if (targetMarker != null) {
                mapView.getController().animateTo(targetMarker.getPosition());
            }
        });
    }

    private final androidx.activity.result.ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(), result -> {
                startListeningToTarget();
            });

    private void checkAuthAndPermissions() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to SafeTour first to view live tracking.", Toast.LENGTH_LONG).show();
            startActivity(new android.content.Intent(this, com.safetour.ai.MainActivity.class));
            finish();
            return;
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startListeningToTarget();
        } else {
            locationPermissionRequest.launch(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startListeningToTarget() {
        if (targetUid == null) return;
        
        DocumentReference docRef = db.collection("users").document(targetUid)
                .collection("live_tracking").document("current_location");

        locationListener = docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                tvTargetStatus.setText("Network error: " + e.getMessage());
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Double lat = snapshot.getDouble("latitude");
                Double lng = snapshot.getDouble("longitude");
                
                if (lat != null && lng != null) {
                    tvTargetStatus.setText("Live monitoring active.");
                    updateMapMarker(lat, lng);
                }
            } else {
                tvTargetStatus.setText("Waiting for target's GPS signal...");
                if (targetMarker != null) {
                    mapView.getOverlays().remove(targetMarker);
                    mapView.invalidate();
                }
            }
        });
    }

    private void updateMapMarker(double lat, double lng) {
        GeoPoint newPosition = new GeoPoint(lat, lng);

        if (targetMarker == null) {
            targetMarker = new Marker(mapView);
            targetMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_myplaces, null));
            targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            targetMarker.setTitle("Friend's Location");
            mapView.getOverlays().add(targetMarker);
            mapView.getController().setCenter(newPosition); // Initially center map on them
        }
        
        targetMarker.setPosition(newPosition);
        mapView.invalidate();
        tvTargetStatus.setText(String.format("Streaming Live: %.4f, %.4f", lat, lng));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationListener != null) {
            locationListener.remove();
        }
        mapView.onDetach();
    }
}

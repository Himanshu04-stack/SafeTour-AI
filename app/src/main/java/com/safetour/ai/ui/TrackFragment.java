package com.safetour.ai.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.safetour.ai.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.preference.PreferenceManager;

public class TrackFragment extends Fragment {

    private MapView mapView;
    private TextView tvOwnTrackingId;
    private TextView tvConnectionStatus;
    private EditText etTargetTrackingId;
    private Marker targetMarker;
    
    private FirebaseFirestore db;
    private ListenerRegistration locationListener;
    private String currentTrackingTargetUid = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_track, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mapView = view.findViewById(R.id.trackMapView);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        tvOwnTrackingId = view.findViewById(R.id.tvOwnTrackingId);
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        etTargetTrackingId = view.findViewById(R.id.etTargetTrackingId);
        Button btnStartTracking = view.findViewById(R.id.btnStartTracking);
        FloatingActionButton fabRecenter = view.findViewById(R.id.fabTrackRecenter);
        ImageView btnCopyId = view.findViewById(R.id.btnCopyId);

        db = FirebaseFirestore.getInstance();

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

        // Setup Own Tracking ID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            tvOwnTrackingId.setText(myUid);
        } else {
            tvOwnTrackingId.setText("Not Logged In");
        }

        btnCopyId.setOnClickListener(v -> {
            String id = tvOwnTrackingId.getText().toString();
            if (!id.equals("Not Logged In") && !id.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Tracking ID", id);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "Tracking ID Copied to Clipboard!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStartTracking.setOnClickListener(v -> {
            String input = etTargetTrackingId.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(requireContext(), "Please paste a Tracking ID or Link.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String parsedUid = parseTargetUid(input);
            if (parsedUid == null || parsedUid.isEmpty()) {
                Toast.makeText(requireContext(), "Invalid tracking ID format.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (parsedUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                Toast.makeText(requireContext(), "You cannot track yourself.", Toast.LENGTH_SHORT).show();
                return;
            }

            currentTrackingTargetUid = parsedUid;
            tvConnectionStatus.setText("Connecting to friend's live stream...");
            tvConnectionStatus.setTextColor(android.graphics.Color.parseColor("#FFA500")); // Orange
            checkAuthAndPermissions();
        });

        com.google.android.material.switchmaterial.SwitchMaterial switchBroadcast = view.findViewById(R.id.switchBroadcast);
        switchBroadcast.setChecked(isServiceRunning(com.safetour.ai.service.LocationTrackingService.class));
        switchBroadcast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.content.Intent serviceIntent = new android.content.Intent(requireContext(), com.safetour.ai.service.LocationTrackingService.class);
                    androidx.core.content.ContextCompat.startForegroundService(requireContext(), serviceIntent);
                    Toast.makeText(requireContext(), "Live Tracking Broadcast Started.", Toast.LENGTH_SHORT).show();
                } else {
                    switchBroadcast.setChecked(false);
                    Toast.makeText(requireContext(), "Location permission required to broadcast.", Toast.LENGTH_SHORT).show();
                }
            } else {
                android.content.Intent stopIntent = new android.content.Intent(requireContext(), com.safetour.ai.service.LocationTrackingService.class);
                stopIntent.setAction("STOP_SERVICE");
                requireContext().startService(stopIntent);
                Toast.makeText(requireContext(), "Live Tracking Broadcast Stopped.", Toast.LENGTH_SHORT).show();
            }
        });

        fabRecenter.setOnClickListener(v -> {
            if (targetMarker != null) {
                mapView.getController().animateTo(targetMarker.getPosition());
            }
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String parseTargetUid(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return null;
        // If it's a raw string containing the ID (like pasted SMS), grab the last word.
        int lastSpace = trimmed.lastIndexOf(" ");
        if (lastSpace != -1) {
            String extracted = trimmed.substring(lastSpace + 1).trim();
            // Optional: Strip slashes if they pasted a URL instead
            if(extracted.contains("/")) extracted = extracted.substring(extracted.lastIndexOf("/") + 1);
            return extracted;
        }
        if(trimmed.contains("/")) trimmed = trimmed.substring(trimmed.lastIndexOf("/") + 1);
        return trimmed;
    }

    private void checkAuthAndPermissions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to SafeTour first.", Toast.LENGTH_LONG).show();
            return;
        }

        // A watcher doesn't need to broadcast their own location, so we instantly attach the Firestore listener!
        startListeningToTarget();
    }

    private void startListeningToTarget() {
        if (currentTrackingTargetUid == null) return;
        
        // Clear previous listener if any
        if (locationListener != null) {
            locationListener.remove();
        }
        if (targetMarker != null) {
            mapView.getOverlays().remove(targetMarker);
            targetMarker = null;
        }
        
        DocumentReference docRef = db.collection("users").document(currentTrackingTargetUid)
                .collection("live_tracking").document("current_location");

        locationListener = docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                if (isAdded()) {
                    tvConnectionStatus.setText("Network error: " + e.getMessage());
                    tvConnectionStatus.setTextColor(android.graphics.Color.RED);
                }
                return;
            }

            if (!isAdded()) return;

            if (snapshot != null && snapshot.exists()) {
                Double lat = snapshot.getDouble("latitude");
                Double lng = snapshot.getDouble("longitude");
                
                if (lat != null && lng != null) {
                    tvConnectionStatus.setText("Live monitoring active.");
                    tvConnectionStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
                    updateMapMarker(lat, lng);
                } else {
                    tvConnectionStatus.setText("Doc exists but lat/lng missing. Data: " + snapshot.getData());
                    tvConnectionStatus.setTextColor(android.graphics.Color.RED);
                }
            } else {
                tvConnectionStatus.setText("Waiting for signal from " + (currentTrackingTargetUid.length() > 4 ? currentTrackingTargetUid.substring(0,4) : currentTrackingTargetUid) + "...");
                tvConnectionStatus.setTextColor(android.graphics.Color.parseColor("#FFA500")); // Orange
                if (targetMarker != null && mapView != null) {
                    mapView.getOverlays().remove(targetMarker);
                    mapView.invalidate();
                }
            }
        });
    }

    private void updateMapMarker(double lat, double lng) {
        if (!isAdded() || mapView == null) return;
        
        GeoPoint newPosition = new GeoPoint(lat, lng);

        if (targetMarker == null) {
            targetMarker = new Marker(mapView);
            targetMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_myplaces, null));
            targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            targetMarker.setTitle("Friend's Location");
            mapView.getOverlays().add(targetMarker);
            mapView.getController().setCenter(newPosition); // Initially center map on them
        } else {
            targetMarker.setPosition(newPosition);
        }
        
        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationListener != null) {
            locationListener.remove();
        }
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}

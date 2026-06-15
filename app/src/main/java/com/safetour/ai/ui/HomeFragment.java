package com.safetour.ai.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import android.preference.PreferenceManager;
import com.safetour.ai.R;
import com.safetour.ai.repository.ProfileRepository;
import com.safetour.ai.viewmodel.ProfileViewModel;
import com.safetour.ai.viewmodel.SharedLocationViewModel;

public class HomeFragment extends Fragment {

    private ObjectAnimator progressAnimator;
    private boolean isSosTriggered = false;
    
    private MapView mapView;
    private MyLocationNewOverlay mLocationOverlay;
    
    private ProfileViewModel profileViewModel;
    private SharedLocationViewModel locationViewModel;
    

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    enableMyLocation();
                } else {
                    Toast.makeText(requireContext(), "Location permission denied. Map functions limited.", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        locationViewModel = new ViewModelProvider(requireActivity()).get(SharedLocationViewModel.class);

        mapView = view.findViewById(R.id.mapView);
        if (mapView != null) {
            mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(15.0);
            
            if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix(new float[] {
                    -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                     0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                     0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                     0.0f,  0.0f,  0.0f,  1.0f,   0.0f
                });
                mapView.getOverlayManager().getTilesOverlay().setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
            }
            
            checkLocationPermissions();
        }

        View llQuickAlerts = view.findViewById(R.id.llQuickAlerts);
        View llSafeCircle = view.findViewById(R.id.llSafeCircle);
        if (llQuickAlerts != null) {
            llQuickAlerts.setOnClickListener(v -> showQuickAlertsDialog());
        }
        if (llSafeCircle != null) {
            llSafeCircle.setOnClickListener(v -> showSafeCircleDialog());
        }

        setupSosButton(view);
        startStatusDotAnimation(view);
        updateSafeCircleState(view);
        
        ProfileRepository repo = new ProfileRepository(requireContext());
        if (repo.getBoolean("is_sos_active", false)) {
            isSosTriggered = true;
            Button btnSos = view.findViewById(R.id.btnSos);
            TextView tvHoldText = view.findViewById(R.id.tvHoldText);
            if (btnSos != null) {
                btnSos.setText("CANCEL");
                btnSos.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 32f);
            }
            if (tvHoldText != null) tvHoldText.setText("Hold for 2 seconds to cancel");
            startSosRippleAnimation();
        }
    }
    
    private void updateSafeCircleState(View root) {
        TextView tvMonitoring = root.findViewById(R.id.tvSafeCircleMonitoring);
        if (tvMonitoring == null) return;
        
        ProfileRepository repo = new ProfileRepository(requireContext());
        int count = 0;
        if (!repo.getString("safe_circle_contact", "").isEmpty()) count++;
        if (!repo.getString("c1_phone", "").isEmpty()) count++;
        if (!repo.getString("c2_phone", "").isEmpty()) count++;
        if (!repo.getString("c3_phone", "").isEmpty()) count++;
        
        if (count == 1) {
            tvMonitoring.setText("1 contact monitoring");
        } else {
            tvMonitoring.setText(count + " contacts monitoring");
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
        
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!perms.isEmpty()) {
            locationPermissionRequest.launch(perms.toArray(new String[0]));
        }
    }
    
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mapView != null) {
                mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
                mLocationOverlay.enableMyLocation();
                mLocationOverlay.enableFollowLocation();
                mapView.getOverlays().add(mLocationOverlay);
            }
            
            locationViewModel.refreshLocation();
            
            locationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
                if (location != null && mapView != null) {
                    GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(startPoint);
                    
                    View root = getView();
                    if (root != null) {
                        TextView tvLocationTitle = root.findViewById(R.id.tvLocationTitle);
                        if (tvLocationTitle != null) {
                            tvLocationTitle.setText(String.format("Lat: %.4f, Lng: %.4f", location.getLatitude(), location.getLongitude()));
                        }
                    }
                }
            });
            
            locationViewModel.getCurrentCity().observe(getViewLifecycleOwner(), city -> {
                View root = getView();
                if (root != null && city != null) {
                    TextView tvStatusCity = root.findViewById(R.id.tvStatusCity);
                    TextView tvStatusLocation = root.findViewById(R.id.tvStatusLocation);
                    
                    if (tvStatusCity != null) tvStatusCity.setText("in " + city);
                    if (tvStatusLocation != null) {
                        String country = locationViewModel.getCurrentCountry().getValue();
                        tvStatusLocation.setText("Current Location: " + city + ", " + country);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDetach();
    }
    
    private void startStatusDotAnimation(View root) {
        View statusDotRipple = root.findViewById(R.id.statusDotRipple);
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(statusDotRipple, "scaleX", 1f, 2.5f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.RESTART);
        scaleX.setDuration(1500);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(statusDotRipple, "scaleY", 1f, 2.5f);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.RESTART);
        scaleY.setDuration(1500);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(statusDotRipple, "alpha", 0.6f, 0f);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.RESTART);
        alpha.setDuration(1500);

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void setupSosButton(View root) {
        Button btnSos = root.findViewById(R.id.btnSos);
        ProgressBar sosProgressRing = root.findViewById(R.id.sosProgressRing);

        btnSos.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).start();
                    
                    int targetTime = 2000; // Exact 2-second hold bounds requested
                    sosProgressRing.setMax(1000); // Enforce progress bound correctly
                    
                    progressAnimator = ObjectAnimator.ofInt(sosProgressRing, "progress", 0, 1000);
                    progressAnimator.setDuration(targetTime);
                    progressAnimator.setInterpolator(new LinearInterpolator());
                    
                    progressAnimator.addUpdateListener(animation -> {
                        int progress = (int) animation.getAnimatedValue();
                        if (progress >= 1000) {
                            progressAnimator.cancel();
                            sosProgressRing.setProgress(0);
                            if (isSosTriggered) {
                                cancelSos();
                            } else {
                                triggerSos();
                            }
                        }
                    });
                    progressAnimator.start();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    if (progressAnimator != null) {
                        progressAnimator.cancel();
                    }
                    sosProgressRing.setProgress(0);
                    return true;
            }
            return false;
        });
    }

    private void triggerSos() {
        if (isSosTriggered) return;
        isSosTriggered = true;
        new ProfileRepository(requireContext()).saveBoolean("is_sos_active", true);
        
        Button btnSos = requireView().findViewById(R.id.btnSos);
        TextView tvHoldText = requireView().findViewById(R.id.tvHoldText);
        if (btnSos != null) {
            btnSos.setText("CANCEL");
            btnSos.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 32f);
        }
        if (tvHoldText != null) tvHoldText.setText("Hold for 2 seconds to cancel");

        Vibrator vibrator = (Vibrator) requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        startSosRippleAnimation();

        // 1. Start Background Live Location Tracking automatically!
        android.content.Intent serviceIntent = new android.content.Intent(requireContext(), com.safetour.ai.service.LocationTrackingService.class);
        androidx.core.content.ContextCompat.startForegroundService(requireContext(), serviceIntent);
        Toast.makeText(requireContext(), "SOS Mode Active: Live location broadcasting to Safe Circle...", Toast.LENGTH_LONG).show();

        // 2. Dispatch the foolproof Intent Compose method
        dispatchSosMessage();
    }

    private void cancelSos() {
        if (!isSosTriggered) return;
        isSosTriggered = false;
        new ProfileRepository(requireContext()).saveBoolean("is_sos_active", false);
        
        Button btnSos = requireView().findViewById(R.id.btnSos);
        TextView tvHoldText = requireView().findViewById(R.id.tvHoldText);
        if (btnSos != null) {
            btnSos.setText("SOS");
            btnSos.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 56f);
        }
        if (tvHoldText != null) tvHoldText.setText("Hold for 2 seconds");

        Vibrator vibrator = (Vibrator) requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        stopSosRippleAnimation();
        
        android.content.Intent stopIntent = new android.content.Intent(requireContext(), com.safetour.ai.service.LocationTrackingService.class);
        stopIntent.setAction("STOP_SERVICE");
        requireContext().startService(stopIntent);
        
        Toast.makeText(requireContext(), "SOS Mode Disabled. Live tracking stopped.", Toast.LENGTH_SHORT).show();
    }

    private void dispatchSosMessage() {
        String phone = profileViewModel.getC1Phone().getValue();
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(requireContext(), "No Emergency Contact configured! Please set one in Profile.", Toast.LENGTH_LONG).show();
            return;
        }

        android.location.Location loc = locationViewModel.getCurrentLocation().getValue();
        String mapUrl = "https://maps.google.com/?q=live";
        if (loc != null) {
            mapUrl = "https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
        }
        
        String trackingIdText = "";
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            trackingIdText = "\n\nTrack me live by pasting this Tracking ID into the SafeTour App: " + auth.getCurrentUser().getUid();
        }
        
        String message = "EMERGENCY from SafeTour AI: I need immediate assistance! Here is my last static location: " + mapUrl + trackingIdText;
        
        android.content.Intent smsIntent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
        smsIntent.setData(android.net.Uri.parse("smsto:" + phone));
        smsIntent.putExtra("sms_body", message);
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No SMS app found. Unable to dispatch SOS.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startSosRippleAnimation() {
        View ripple1 = requireView().findViewById(R.id.sosRipple1);
        View ripple2 = requireView().findViewById(R.id.sosRipple2);
        
        if (ripple1 == null || ripple2 == null || ripple1.getVisibility() == View.VISIBLE) return;
        
        ripple1.setVisibility(View.VISIBLE);
        ripple2.setVisibility(View.VISIBLE);
        
        long duration = 2000;
        
        ObjectAnimator rx1 = ObjectAnimator.ofFloat(ripple1, "scaleX", 0.9f, 2.0f);
        ObjectAnimator ry1 = ObjectAnimator.ofFloat(ripple1, "scaleY", 0.9f, 2.0f);
        ObjectAnimator ra1 = ObjectAnimator.ofFloat(ripple1, "alpha", 1f, 0f);
        rx1.setRepeatCount(ValueAnimator.INFINITE);
        ry1.setRepeatCount(ValueAnimator.INFINITE);
        ra1.setRepeatCount(ValueAnimator.INFINITE);
        rx1.setDuration(duration);
        ry1.setDuration(duration);
        ra1.setDuration(duration);
        
        ObjectAnimator rx2 = ObjectAnimator.ofFloat(ripple2, "scaleX", 0.9f, 2.0f);
        ObjectAnimator ry2 = ObjectAnimator.ofFloat(ripple2, "scaleY", 0.9f, 2.0f);
        ObjectAnimator ra2 = ObjectAnimator.ofFloat(ripple2, "alpha", 1f, 0f);
        rx2.setRepeatCount(ValueAnimator.INFINITE);
        ry2.setRepeatCount(ValueAnimator.INFINITE);
        ra2.setRepeatCount(ValueAnimator.INFINITE);
        rx2.setDuration(duration);
        ry2.setDuration(duration);
        ra2.setDuration(duration);
        
        rx2.setStartDelay(1000);
        ry2.setStartDelay(1000);
        ra2.setStartDelay(1000);
        
        rx1.start(); ry1.start(); ra1.start();
        rx2.start(); ry2.start(); ra2.start();
    }

    private void stopSosRippleAnimation() {
        View ripple1 = requireView().findViewById(R.id.sosRipple1);
        View ripple2 = requireView().findViewById(R.id.sosRipple2);
        if (ripple1 != null) {
            ripple1.clearAnimation();
            ripple1.setVisibility(View.INVISIBLE);
        }
        if (ripple2 != null) {
            ripple2.clearAnimation();
            ripple2.setVisibility(View.INVISIBLE);
        }
    }
    
    private void showQuickAlertsDialog() {
        String c1Phone = profileViewModel.getC1Phone().getValue();
        if (c1Phone == null || c1Phone.isEmpty()) {
            Toast.makeText(requireContext(), "Please set an Emergency Contact in your Profile first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        android.content.Intent smsIntent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
        smsIntent.setData(android.net.Uri.parse("smsto:" + c1Phone));
        smsIntent.putExtra("sms_body", "URGENT from SafeTour AI: I need immediate assistance! Here is my live location: https://maps.google.com/?q=live");
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No SMS app found on this device.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showSafeCircleDialog() {
        String[] options = {"Share My Live Location", "Watch a Friend's Location", "Stop Live Tracking"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Safe Circle")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showShareLocationDialog();
                    } else if (which == 1) {
                        androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.navigation_track);
                    } else if (which == 2) {
                        try { cancelSos(); } catch (Exception ignored) {} // Updates UI if visible
                        android.content.Intent stopIntent = new android.content.Intent(requireContext(), com.safetour.ai.service.LocationTrackingService.class);
                        stopIntent.setAction("STOP_SERVICE");
                        requireContext().startService(stopIntent);
                        new ProfileRepository(requireContext()).saveBoolean("is_sos_active", false);
                        Toast.makeText(requireContext(), "Live Tracking Stopped.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
    
    private void showWatchFriendDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Paste tracking link or ID here");
        input.setPadding(60, 40, 60, 40);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Watch a Friend")
                .setMessage("Paste the secure tracking link or tracker ID you received via SMS to monitor their live GPS stream.")
                .setView(input)
                .setPositiveButton("Start Watching", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (!code.isEmpty()) {
                        String targetUid = code;
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[a-zA-Z0-9]{28}").matcher(code);
                        if (m.find()) {
                            targetUid = m.group();
                        } else if (code.contains("url=")) {
                            try { m = java.util.regex.Pattern.compile("[a-zA-Z0-9]{28}").matcher(java.net.URLDecoder.decode(code, "UTF-8")); } catch (Exception ignored) {}
                            if (m != null && m.find()) targetUid = m.group();
                        } else if (code.contains("/track/")) {
                            targetUid = code.substring(code.lastIndexOf("/") + 1);
                        }
                        
                        if (targetUid.isEmpty()) {
                            Toast.makeText(requireContext(), "Invalid tracking ID.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        android.content.Intent watchIntent = new android.content.Intent(requireContext(), com.safetour.ai.ui.LiveTrackingActivity.class);
                        watchIntent.putExtra("target_uid", targetUid);
                        startActivity(watchIntent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showShareLocationDialog() {
        ProfileRepository directRepo = new ProfileRepository(requireContext());
        String addedWatcher = directRepo.getString("safe_circle_contact", "");
        
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter phone number to invite (+1...)");
        input.setText(addedWatcher);
        input.setPadding(60, 40, 60, 40);

        new AlertDialog.Builder(requireContext())
                .setTitle("Manage My Safe Circle")
                .setMessage("Add a trusted contact to dynamically monitor your continuous live GPS stream.")
                .setView(input)
                .setPositiveButton("Invite & Start Tracking", (dialog, which) -> {
                    String newWatcher = input.getText().toString();
                    if (!newWatcher.isEmpty()) {
                        directRepo.saveString("safe_circle_contact", newWatcher);
                        
                        // 1. Start Tracker Background Service
                        android.content.Intent serviceIntent = new android.content.Intent(requireContext(), com.safetour.ai.service.LocationTrackingService.class);
                        androidx.core.content.ContextCompat.startForegroundService(requireContext(), serviceIntent);

                        // 2. Extract Firebase UID for manual tracker sharing
                        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null) {
                            String myUid = auth.getCurrentUser().getUid();

                            // 3. Open SMS App to Dispatch Tracking ID
                            String smsBody = "SafeTour Tracker: I am sharing my live GPS location! Open the SafeTour app, go to the 'Track' tab, and paste my Tracking ID to monitor me: " + myUid;
                            android.content.Intent smsIntent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
                            smsIntent.setData(android.net.Uri.parse("smsto:" + newWatcher));
                            smsIntent.putExtra("sms_body", smsBody);
                            try {
                                startActivity(smsIntent);
                                Toast.makeText(requireContext(), "Tracking Started! Send the text msg to share your ID.", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Tracking started! No SMS app found. Please share your Tracking ID manually.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "You must be logged in to dispatch a live tracking link.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

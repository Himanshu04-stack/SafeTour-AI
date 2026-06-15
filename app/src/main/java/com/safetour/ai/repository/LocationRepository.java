package com.safetour.ai.repository;

import android.app.Application;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import java.util.Locale;

public class LocationRepository {
    private final FusedLocationProviderClient fusedLocationClient;
    private final Application application;

    public LocationRepository(Application application) {
        this.application = application;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application);
    }

    public interface LocationCallback {
        void onLocationResult(Location location, String city, String country);
    }

    public void fetchLastLocation(LocationCallback callback) {
        if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(application, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String city = address.getLocality() != null ? address.getLocality() : address.getSubAdminArea();
                            String country = address.getCountryCode();
                            callback.onLocationResult(location, city, country);
                        } else {
                            callback.onLocationResult(location, "Unknown", "Unknown");
                        }
                    } catch (Exception e) {
                        callback.onLocationResult(location, "Unknown", "Unknown");
                    }
                }
            });
        }
    }
}

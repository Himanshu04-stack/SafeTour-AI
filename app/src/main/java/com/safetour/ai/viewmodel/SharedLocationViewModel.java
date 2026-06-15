package com.safetour.ai.viewmodel;

import android.app.Application;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.safetour.ai.repository.LocationRepository;

public class SharedLocationViewModel extends AndroidViewModel {
    
    private final LocationRepository repository;
    
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<String> currentCity = new MutableLiveData<>();
    private final MutableLiveData<String> currentCountry = new MutableLiveData<>();
    
    public SharedLocationViewModel(@NonNull Application application) {
        super(application);
        repository = new LocationRepository(application);
    }

    public void refreshLocation() {
        repository.fetchLastLocation((location, city, country) -> {
            currentLocation.setValue(location);
            currentCity.setValue(city);
            currentCountry.setValue(country);
        });
    }

    public LiveData<Location> getCurrentLocation() { return currentLocation; }
    public LiveData<String> getCurrentCity() { return currentCity; }
    public LiveData<String> getCurrentCountry() { return currentCountry; }
}

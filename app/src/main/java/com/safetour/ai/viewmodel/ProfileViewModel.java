package com.safetour.ai.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.safetour.ai.repository.ProfileRepository;

public class ProfileViewModel extends AndroidViewModel {
    private final ProfileRepository repository;
    
    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<String> userPhotoUri = new MutableLiveData<>();
    private final MutableLiveData<String> documentUri = new MutableLiveData<>();
    
    // Contacts: Contact 1
    private final MutableLiveData<String> c1Name = new MutableLiveData<>();
    private final MutableLiveData<String> c1Rel = new MutableLiveData<>();
    private final MutableLiveData<String> c1Phone = new MutableLiveData<>();
    
    // Contacts: Contact 2
    private final MutableLiveData<String> c2Name = new MutableLiveData<>();
    private final MutableLiveData<String> c2Rel = new MutableLiveData<>();
    private final MutableLiveData<String> c2Phone = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new ProfileRepository(application);
        refreshData();
    }

    public void refreshData() {
        userName.setValue(repository.getString("user_name", "Pratham"));
        userPhotoUri.setValue(repository.getString("user_photo_uri", ""));
        documentUri.setValue(repository.getString("document_uri", ""));
        
        c1Name.setValue(repository.getString("contact1_name", "Sarah Doe"));
        c1Rel.setValue(repository.getString("contact1_rel", "Relationship: Wife"));
        c1Phone.setValue(repository.getString("contact1_phone", "+15551234567"));
        
        c2Name.setValue(repository.getString("contact2_name", "Mark Lee"));
        c2Rel.setValue(repository.getString("contact2_rel", "Relationship: Brother"));
        c2Phone.setValue(repository.getString("contact2_phone", "+911124198000"));
    }

    // Getters
    public LiveData<String> getUserName() { return userName; }
    public LiveData<String> getUserPhotoUri() { return userPhotoUri; }
    public LiveData<String> getDocumentUri() { return documentUri; }
    public LiveData<String> getC1Name() { return c1Name; }
    public LiveData<String> getC1Rel() { return c1Rel; }
    public LiveData<String> getC1Phone() { return c1Phone; }
    public LiveData<String> getC2Name() { return c2Name; }
    public LiveData<String> getC2Rel() { return c2Rel; }
    public LiveData<String> getC2Phone() { return c2Phone; }
    
    // Setters
    public void setUserName(String name) {
        repository.saveString("user_name", name);
        userName.setValue(name);
    }
    public void setUserPhoto(String uri) {
        repository.saveString("user_photo_uri", uri);
        userPhotoUri.setValue(uri);
    }
    public void setDocumentPhoto(String uri) {
        repository.saveString("document_uri", uri);
        documentUri.setValue(uri);
    }
    public void saveContact1(String name, String rel, String phone) {
        repository.saveString("contact1_name", name);
        repository.saveString("contact1_rel", rel);
        repository.saveString("contact1_phone", phone);
        refreshData();
    }
    public void saveContact2(String name, String rel, String phone) {
        repository.saveString("contact2_name", name);
        repository.saveString("contact2_rel", rel);
        repository.saveString("contact2_phone", phone);
        refreshData();
    }
    
    public void markSetupComplete() { repository.saveBoolean("is_setup_complete", true); }
    public boolean isSetupComplete() { return repository.getBoolean("is_setup_complete", false); }

    public void logout() {
        repository.clearAllData();
        refreshData();
    }

    public void saveProfileFromFirestore(java.util.Map<String, Object> data) {
        if (data == null) return;
        
        if (data.containsKey("name")) setUserName((String) data.get("name"));
        if (data.containsKey("photoUri")) setUserPhoto((String) data.get("photoUri"));
        if (data.containsKey("documentUri")) setDocumentPhoto((String) data.get("documentUri"));
        
        if (data.containsKey("c1Name")) {
            saveContact1(
                data.containsKey("c1Name") ? (String) data.get("c1Name") : "",
                data.containsKey("c1Rel") ? (String) data.get("c1Rel") : "",
                data.containsKey("c1Phone") ? (String) data.get("c1Phone") : ""
            );
        }
        if (data.containsKey("c2Name")) {
            saveContact2(
                data.containsKey("c2Name") ? (String) data.get("c2Name") : "",
                data.containsKey("c2Rel") ? (String) data.get("c2Rel") : "",
                data.containsKey("c2Phone") ? (String) data.get("c2Phone") : ""
            );
        }
        
        markSetupComplete();
    }
}

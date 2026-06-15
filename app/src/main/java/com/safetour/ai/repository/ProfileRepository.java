package com.safetour.ai.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class ProfileRepository {
    private final SharedPreferences prefs;

    public ProfileRepository(Context context) {
        prefs = context.getSharedPreferences("SafeTourPrefs", Context.MODE_PRIVATE);
    }

    public String getString(String key, String defValue) { return prefs.getString(key, defValue); }
    public void saveString(String key, String value) { prefs.edit().putString(key, value).apply(); }
    
    public boolean getBoolean(String key, boolean defValue) { return prefs.getBoolean(key, defValue); }
    public void saveBoolean(String key, boolean value) { prefs.edit().putBoolean(key, value).apply(); }
    
    public void clearAllData() { prefs.edit().clear().apply(); }
}

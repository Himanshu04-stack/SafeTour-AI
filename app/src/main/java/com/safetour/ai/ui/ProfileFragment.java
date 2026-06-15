package com.safetour.ai.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.res.Configuration;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.safetour.ai.R;
import com.safetour.ai.viewmodel.ProfileViewModel;
import java.util.concurrent.Executor;

public class ProfileFragment extends Fragment {

    private ImageView ivDocumentVault;
    private View vDarkOverlay;
    private LinearLayout llLockOverlay;
    private LinearLayout llUnlockVault;
    private ImageView ivEditDocument;
    
    private TextView tvContact1Name;
    private TextView tvContact1Relation;
    private TextView tvContact2Name;
    private TextView tvContact2Relation;
    
    private ImageView ivUserProfile;
    private TextView tvUserName;

    private boolean isVaultUnlocked = false;
    private ProfileViewModel profileViewModel;

    private String contact1Phone = "+15551234567";
    private String contact2Phone = "+911124198000";
    
    private final ActivityResultLauncher<String> profilePhotoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try { requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception e) {}
                    profileViewModel.setUserPhoto(uri.toString());
                    Toast.makeText(requireContext(), "Profile Photo Updated", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try { requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception e) {}
                    profileViewModel.setDocumentPhoto(uri.toString());
                    Toast.makeText(requireContext(), "Document Updated", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private String currentPhotoTarget = "";
    private final ActivityResultLauncher<String> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    if ("profile".equals(currentPhotoTarget)) profilePhotoPickerLauncher.launch("image/*");
                    else photoPickerLauncher.launch("image/*");
                } else {
                    Toast.makeText(requireContext(), "Storage permission is required to access gallery.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void requestGalleryPermission(String target) {
        currentPhotoTarget = target;
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
            android.Manifest.permission.READ_MEDIA_IMAGES : android.Manifest.permission.READ_EXTERNAL_STORAGE;
            
        if (ContextCompat.checkSelfPermission(requireContext(), perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if ("profile".equals(target)) profilePhotoPickerLauncher.launch("image/*");
            else photoPickerLauncher.launch("image/*");
        } else {
            storagePermissionLauncher.launch(perm);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        ivDocumentVault = root.findViewById(R.id.ivDocumentVault);
        vDarkOverlay = root.findViewById(R.id.vDarkOverlay);
        llLockOverlay = root.findViewById(R.id.llLockOverlay);
        llUnlockVault = root.findViewById(R.id.llUnlockVault);
        ivEditDocument = root.findViewById(R.id.ivEditDocument);
        
        ImageView ivCallContact1 = root.findViewById(R.id.ivCallContact1);
        ImageView ivCallContact2 = root.findViewById(R.id.ivCallContact2);
        ImageView ivEditContacts = root.findViewById(R.id.ivEditContacts);
        
        tvContact1Name = root.findViewById(R.id.tvContact1Name);
        tvContact1Relation = root.findViewById(R.id.tvContact1Relation);
        tvContact2Name = root.findViewById(R.id.tvContact2Name);
        tvContact2Relation = root.findViewById(R.id.tvContact2Relation);

        ivUserProfile = root.findViewById(R.id.ivUserProfile);
        tvUserName = root.findViewById(R.id.tvUserName);
        ImageView ivEditProfileName = root.findViewById(R.id.ivEditProfileName);
        
        LinearLayout llShareProfile = root.findViewById(R.id.llShareProfile);
        View cvSettings = root.findViewById(R.id.cvSettings);
        View cvAbout = root.findViewById(R.id.cvAbout);
        LinearLayout llLogOut = root.findViewById(R.id.llLogOut);

        // MVVM Observers
        profileViewModel.getUserName().observe(getViewLifecycleOwner(), name -> tvUserName.setText(name));
        profileViewModel.getUserPhotoUri().observe(getViewLifecycleOwner(), uriString -> {
            if (uriString != null && !uriString.isEmpty()) {
                ivUserProfile.setImageURI(Uri.parse(uriString));
                ivUserProfile.setImageTintList(null);
                ivUserProfile.setPadding(0, 0, 0, 0);
            }
        });
        profileViewModel.getDocumentUri().observe(getViewLifecycleOwner(), uriString -> {
            if (uriString != null && !uriString.isEmpty()) {
                ivDocumentVault.setImageURI(Uri.parse(uriString));
            }
        });
        
        profileViewModel.getC1Name().observe(getViewLifecycleOwner(), name -> tvContact1Name.setText(name));
        profileViewModel.getC1Rel().observe(getViewLifecycleOwner(), rel -> tvContact1Relation.setText(rel));
        profileViewModel.getC1Phone().observe(getViewLifecycleOwner(), phone -> contact1Phone = phone);
        
        profileViewModel.getC2Name().observe(getViewLifecycleOwner(), name -> tvContact2Name.setText(name));
        profileViewModel.getC2Rel().observe(getViewLifecycleOwner(), rel -> tvContact2Relation.setText(rel));
        profileViewModel.getC2Phone().observe(getViewLifecycleOwner(), phone -> contact2Phone = phone);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ivDocumentVault.post(() -> {
                if (!isVaultUnlocked) {
                    ivDocumentVault.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP));
                }
            });
        }

        llUnlockVault.setOnClickListener(v -> showBiometricPrompt());
        ivEditDocument.setOnClickListener(v -> requestGalleryPermission("document"));
        ivCallContact1.setOnClickListener(v -> makePhoneCall(contact1Phone));
        ivCallContact2.setOnClickListener(v -> makePhoneCall(contact2Phone));
        ivEditContacts.setOnClickListener(v -> showEditContactsDialog());
        ivUserProfile.setOnClickListener(v -> requestGalleryPermission("profile"));
        ivEditProfileName.setOnClickListener(v -> showEditNameDialog());

        FrameLayout flThemeToggle = root.findViewById(R.id.flThemeToggle);
        ImageView ivThemeThumb = root.findViewById(R.id.ivThemeThumb);

        boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (isNightMode) {
            ivThemeThumb.setImageResource(R.drawable.ic_moon);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) ivThemeThumb.getLayoutParams();
            params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            ivThemeThumb.setLayoutParams(params);
        } else {
            ivThemeThumb.setImageResource(R.drawable.ic_sun);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) ivThemeThumb.getLayoutParams();
            params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            ivThemeThumb.setLayoutParams(params);
        }

        flThemeToggle.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("safe_tour_prefs", Context.MODE_PRIVATE);
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                prefs.edit().putBoolean("dark_mode", false).apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                prefs.edit().putBoolean("dark_mode", true).apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
        
        llShareProfile.setOnClickListener(v -> shareProfile());
        cvSettings.setOnClickListener(v -> showSettingsDialog());
        cvAbout.setOnClickListener(v -> showAboutDialog());
        llLogOut.setOnClickListener(v -> logOut());

        return root;
    }
    
    private void showEditNameDialog() {
        EditText etName = new EditText(requireContext());
        String currentName = profileViewModel.getUserName().getValue();
        etName.setText(currentName != null ? currentName : "Pratham");
        etName.setPadding(40, 40, 40, 40);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Update Name")
                .setView(etName)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        profileViewModel.setUserName(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditContactsDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_contacts, null);
        
        EditText et1Name = dialogView.findViewById(R.id.etContact1Name);
        EditText et1Rel = dialogView.findViewById(R.id.etContact1Relation);
        EditText et1Phone = dialogView.findViewById(R.id.etContact1Phone);
        
        EditText et2Name = dialogView.findViewById(R.id.etContact2Name);
        EditText et2Rel = dialogView.findViewById(R.id.etContact2Relation);
        EditText et2Phone = dialogView.findViewById(R.id.etContact2Phone);
        
        String c1RelVal = profileViewModel.getC1Rel().getValue();
        if(c1RelVal != null) c1RelVal = c1RelVal.replace("Relationship: ", "");
        
        String c2RelVal = profileViewModel.getC2Rel().getValue();
        if(c2RelVal != null) c2RelVal = c2RelVal.replace("Relationship: ", "");

        et1Name.setText(profileViewModel.getC1Name().getValue());
        et1Rel.setText(c1RelVal);
        et1Phone.setText(profileViewModel.getC1Phone().getValue());
        
        et2Name.setText(profileViewModel.getC2Name().getValue());
        et2Rel.setText(c2RelVal);
        et2Phone.setText(profileViewModel.getC2Phone().getValue());

        new AlertDialog.Builder(requireContext())
                .setTitle("Update Emergency Contacts")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    profileViewModel.saveContact1(et1Name.getText().toString(), "Relationship: " + et1Rel.getText().toString(), et1Phone.getText().toString());
                    profileViewModel.saveContact2(et2Name.getText().toString(), "Relationship: " + et2Rel.getText().toString(), et2Phone.getText().toString());
                    Toast.makeText(requireContext(), "Contacts Updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showSettingsDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_settings, null);
        bottomSheet.setContentView(view);
        
        view.findViewById(R.id.llSettingsNotifications).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showNotificationsSettings();
        });
        view.findViewById(R.id.llSettingsPrivacy).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showPrivacySettings();
        });
        view.findViewById(R.id.llSettingsData).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showDataSettings();
        });
        view.findViewById(R.id.llSettingsLanguage).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showLanguageSettings();
        });
        
        bottomSheet.show();
    }

    private void showNotificationsSettings() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_settings_notifications, null);
        bottomSheet.setContentView(view);
        view.findViewById(R.id.ivBackFromNotifications).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showSettingsDialog();
        });
        bottomSheet.show();
    }

    private void showPrivacySettings() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_settings_privacy, null);
        bottomSheet.setContentView(view);
        view.findViewById(R.id.ivBackFromPrivacy).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showSettingsDialog();
        });
        bottomSheet.show();
    }

    private void showDataSettings() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_settings_data, null);
        bottomSheet.setContentView(view);
        view.findViewById(R.id.ivBackFromData).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showSettingsDialog();
        });
        
        view.findViewById(R.id.btnClearCache).setOnClickListener(v -> {
            TextView tvCacheSize = view.findViewById(R.id.tvCacheSize);
            android.widget.ProgressBar pbStorageUse = view.findViewById(R.id.pbStorageUse);
            tvCacheSize.setText("0 MB");
            pbStorageUse.setProgress(45);
            Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        });
        bottomSheet.show();
    }

    private void showLanguageSettings() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_settings_language, null);
        bottomSheet.setContentView(view);
        view.findViewById(R.id.ivBackFromLanguage).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showSettingsDialog(); // Re-open the main settings sheet like iOS navigation
        });
        
        android.widget.RadioGroup rgLanguages = view.findViewById(R.id.rgLanguages);
        rgLanguages.setOnCheckedChangeListener((group, checkedId) -> {
            Toast.makeText(requireContext(), "Language saved successfully", Toast.LENGTH_SHORT).show();
        });
        bottomSheet.show();
    }
    
    private void shareProfile() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "SafeTour AI");
        String name = profileViewModel.getC1Name().getValue();
        if (name == null) name = "Sarah Doe";
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey, I am using SafeTour AI to stay safe while traveling. My emergency contact is " + name + " (" + contact1Phone + ").");
        startActivity(Intent.createChooser(shareIntent, "Share Profile"));
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About SafeTour AI")
                .setMessage("Version 1.0.0\n\nSafeTour AI is your intelligent travel companion equipped with real-time scam detection, safe zone mapping, and emergency tracking.")
                .setPositiveButton("Close", null)
                .show();
    }
    
    private void logOut() {
        FirebaseAuth.getInstance().signOut();
        
        com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireActivity(), gso).signOut();
        
        profileViewModel.logout();
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.navigation_login, null, navOptions);
    }
    
    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(ProfileFragment.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(requireContext(), "Auth error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(requireContext(), "Vault Unlocked!", Toast.LENGTH_SHORT).show();
                isVaultUnlocked = true;
                
                vDarkOverlay.setVisibility(View.GONE);
                llLockOverlay.setVisibility(View.GONE);
                llUnlockVault.setVisibility(View.GONE);
                ivEditDocument.setVisibility(View.VISIBLE);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ivDocumentVault.setRenderEffect(null);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(requireContext(), "Auth failed", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Document Vault")
                .setSubtitle("Authenticate via Biometrics to access securely stored documents")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}

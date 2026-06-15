package com.safetour.ai.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.safetour.ai.viewmodel.ProfileViewModel;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
import com.safetour.ai.R;

public class SetupFragment extends Fragment {

    private ImageView ivSetupProfile;
    private String selectedPhotoUri = null;
    private ProfileViewModel profileViewModel;

    private final ActivityResultLauncher<String> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    selectedPhotoUri = uri.toString();
                    ivSetupProfile.setImageURI(uri);
                    ivSetupProfile.setImageTintList(null); // Fix black mask issue on original icon
                    ivSetupProfile.setPadding(0, 0, 0, 0); // Remove placeholder padding
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_setup, container, false);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        ivSetupProfile = root.findViewById(R.id.ivSetupProfile);
        FrameLayout flProfilePhotoPicker = root.findViewById(R.id.flProfilePhotoPicker);
        EditText etSetupName = root.findViewById(R.id.etSetupName);
        EditText etSetupC1Name = root.findViewById(R.id.etSetupC1Name);
        EditText etSetupC1Rel = root.findViewById(R.id.etSetupC1Rel);
        EditText etSetupC1Phone = root.findViewById(R.id.etSetupC1Phone);
        
        ScrollView svSetupForm = root.findViewById(R.id.svSetupForm);
        LinearLayout llWelcomeOverlay = root.findViewById(R.id.llWelcomeOverlay);
        TextView tvWelcomeMessage = root.findViewById(R.id.tvWelcomeMessage);
        ImageView ivWelcomeAvatar = root.findViewById(R.id.ivWelcomeAvatar);
        View btnFinishSetup = root.findViewById(R.id.btnFinishSetup);

        flProfilePhotoPicker.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        btnFinishSetup.setOnClickListener(v -> {
            String name = etSetupName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name.", Toast.LENGTH_SHORT).show();
                return;
            }

            profileViewModel.setUserName(name);
            if (selectedPhotoUri != null) {
                profileViewModel.setUserPhoto(selectedPhotoUri);
                ivWelcomeAvatar.setImageURI(Uri.parse(selectedPhotoUri));
                ivWelcomeAvatar.setImageTintList(null);
                ivWelcomeAvatar.setPadding(0, 0, 0, 0);
            }
            profileViewModel.markSetupComplete();

            // Optional Contacts
            String c1Name = etSetupC1Name.getText().toString().trim();
            if (!c1Name.isEmpty()) {
                profileViewModel.saveContact1(
                    c1Name,
                    "Relationship: " + etSetupC1Rel.getText().toString().trim(),
                    etSetupC1Phone.getText().toString().trim()
                );
            }

            // Push to Firestore
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("name", name);
                if (selectedPhotoUri != null) userData.put("photoUri", selectedPhotoUri);
                if (!c1Name.isEmpty()) {
                    userData.put("c1Name", c1Name);
                    userData.put("c1Rel", "Relationship: " + etSetupC1Rel.getText().toString().trim());
                    userData.put("c1Phone", etSetupC1Phone.getText().toString().trim());
                }
                
                FirebaseFirestore.getInstance().collection("users")
                        .document(currentUser.getUid())
                        .set(userData, SetOptions.merge());
            }

            // Proceed to Welcome Animation
            tvWelcomeMessage.setText("Welcome " + name + "!");
            
            // Hide Keyboard
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (v != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            
            // Fade Out Form
            svSetupForm.animate().alpha(0f).setDuration(400).start();
            
            // Fade In Overlay
            llWelcomeOverlay.setAlpha(0f);
            llWelcomeOverlay.setVisibility(View.VISIBLE);
            llWelcomeOverlay.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .withEndAction(() -> {
                        // Wait 1.5 seconds, then navigate
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            NavHostFragment.findNavController(SetupFragment.this).navigate(R.id.action_setup_to_home);
                        }, 1500);
                    })
                    .start();
        });

        return root;
    }
}

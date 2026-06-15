package com.safetour.ai.ui;

import android.content.Context;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.safetour.ai.viewmodel.ProfileViewModel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import java.util.Map;
import com.safetour.ai.R;

public class LoginFragment extends Fragment {
    
    private FirebaseAuth mAuth;
    private ProfileViewModel profileViewModel;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        // Auto-login if already authenticated
        if (mAuth.getCurrentUser() != null) {
            proceedToNextScreen();
            return;
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        
        Button btnSignIn = view.findViewById(R.id.btnSignIn);
        View btnSignUp = view.findViewById(R.id.btnSignUp);
        View btnGoogle = view.findViewById(R.id.btnGoogle);
        TextInputEditText etEmail = view.findViewById(R.id.etEmail);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSignIn.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    btnSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Sign in completed!", Toast.LENGTH_SHORT).show();
                        proceedToNextScreen();
                    } else {
                        Toast.makeText(requireContext(), "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSignUp.setEnabled(false);
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    btnSignUp.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                        proceedToNextScreen();
                    } else {
                        Toast.makeText(requireContext(), "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(requireContext(), "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity(), task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(requireContext(), "Sign in completed!", Toast.LENGTH_SHORT).show();
                    proceedToNextScreen();
                } else {
                    Toast.makeText(requireContext(), "Firebase Auth failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void proceedToNextScreen() {
        if (mAuth.getCurrentUser() != null) {
            View root = getView();
            if (root != null) {
                View tilEmail = root.findViewById(R.id.tilEmail);
                if (tilEmail != null) tilEmail.setVisibility(View.GONE);
                View tilPassword = root.findViewById(R.id.tilPassword);
                if (tilPassword != null) tilPassword.setVisibility(View.GONE);
                View btnSignIn = root.findViewById(R.id.btnSignIn);
                if (btnSignIn != null) btnSignIn.setVisibility(View.GONE);
                View tvOr = root.findViewById(R.id.tvOr);
                if (tvOr != null) tvOr.setVisibility(View.GONE);
                View btnGoogle = root.findViewById(R.id.btnGoogle);
                if (btnGoogle != null) btnGoogle.setVisibility(View.GONE);
                View btnSignUp = root.findViewById(R.id.btnSignUp);
                if (btnSignUp != null) btnSignUp.setVisibility(View.GONE);
                
                android.widget.TextView tvWelcome = root.findViewById(R.id.tvWelcome);
                if (tvWelcome != null) {
                    tvWelcome.setText("Loading Profile...");
                }
            }
            
            FirebaseFirestore.getInstance().collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("name")) {
                        String name = documentSnapshot.getString("name");
                        if (name == null || name.isEmpty()) name = "Traveler";
                        
                        if (root != null) {
                            android.widget.TextView tvWelcome = root.findViewById(R.id.tvWelcome);
                            if (tvWelcome != null) {
                                tvWelcome.setText("Welcome back,\n" + name + "!");
                                tvWelcome.setTextSize(32f);
                            }
                        }
                        
                        if (profileViewModel != null) {
                            profileViewModel.saveProfileFromFirestore(documentSnapshot.getData());
                        }
                        
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isAdded()) {
                                try {
                                    NavHostFragment.findNavController(this).navigate(R.id.action_login_to_home);
                                } catch (Exception ex) {
                                    android.util.Log.e("SafeTourLogin", "Navigation to home failed", ex);
                                }
                            }
                        }, 1300);
                    } else {
                        checkLocalSetupAndProceed();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.w("SafeTourLogin", "Firestore profile check failed (likely offline/no db). Proceeding with local setup configuration.", e);
                    checkLocalSetupAndProceed();
                });
        } else {
            checkLocalSetupAndProceed();
        }
    }

    private void checkLocalSetupAndProceed() {
        try {
            if (profileViewModel != null && profileViewModel.isSetupComplete()) {
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_home);
            } else {
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_setup);
            }
        } catch (Exception ex) {
            android.util.Log.e("SafeTourLogin", "Navigation failed in checkLocalSetupAndProceed", ex);
            Toast.makeText(requireContext(), "Navigation failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

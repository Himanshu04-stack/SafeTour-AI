package com.safetour.ai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.Toast;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.safetour.ai.R;
import com.safetour.ai.viewmodel.AssistantViewModel;
import com.safetour.ai.viewmodel.SharedLocationViewModel;
import java.util.ArrayList;
import java.util.List;

public class AssistantFragment extends Fragment {

    private ChatAdapter chatAdapter;
    private RecyclerView recyclerView;
    private AssistantViewModel assistantViewModel;
    private SharedLocationViewModel locationViewModel;
    private Uri selectedImageUri = null;
    private DrawerLayout drawerLayout;

    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMedia =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                if (getView() != null) {
                    ImageView ivPaperclip = getView().findViewById(R.id.ivPaperclip);
                    if (ivPaperclip != null) ivPaperclip.setColorFilter(Color.parseColor("#0A7AF5"));
                }
                Toast.makeText(requireContext(), "Image actively staged for Vision API processing.", Toast.LENGTH_SHORT).show();
            }
        });

    private final ActivityResultLauncher<android.content.Intent> speechRecognizerLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                java.util.ArrayList<String> matches = result.getData().getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                if (matches != null && !matches.isEmpty()) {
                    if (getView() != null) {
                        EditText etMessage = getView().findViewById(R.id.etMessage);
                        if (etMessage != null) {
                            etMessage.setText(etMessage.getText().toString() + " " + matches.get(0));
                            etMessage.setSelection(etMessage.getText().length());
                        }
                    }
                }
            }
        });

    private void animateClickAndRun(android.view.View view, Runnable action) {
        view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction(action).start();
        }).start();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_assistant, container, false);

        assistantViewModel = new ViewModelProvider(this).get(AssistantViewModel.class);
        locationViewModel = new ViewModelProvider(requireActivity()).get(SharedLocationViewModel.class);

        drawerLayout = root.findViewById(R.id.drawerLayout);
        recyclerView = root.findViewById(R.id.recyclerViewChat);
        chatAdapter = new ChatAdapter(requireContext(), new java.util.ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatAdapter);
        
        assistantViewModel.getMessages().observe(getViewLifecycleOwner(), msgs -> {
            chatAdapter.updateMessages(msgs);
            if (!msgs.isEmpty()) {
                recyclerView.scrollToPosition(msgs.size() - 1);
            }
        });
        
        assistantViewModel.getIsTyping().observe(getViewLifecycleOwner(), isTyping -> {
            if (isTyping) showTypingIndicator();
            else hideTypingIndicator();
        });
        
        assistantViewModel.initChat("Unknown GPS Location");
        
        locationViewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
            locationViewModel.getCurrentCity().observe(getViewLifecycleOwner(), city -> {
                String ctx = city != null ? city : (location != null ? "Lat " + location.getLatitude() + ", Lng " + location.getLongitude() : "Unknown location");
                assistantViewModel.updateLocationContext(ctx);
            });
        });

        EditText etMessage = root.findViewById(R.id.etMessage);
        ImageView btnSend = root.findViewById(R.id.btnSend);
        Chip chipScamCheck = root.findViewById(R.id.chipScamCheck);
        Chip chipHelp = root.findViewById(R.id.chipHelp);
        Chip chipTranslate = root.findViewById(R.id.chipTranslate);
        Chip chipAnalyze = root.findViewById(R.id.chipAnalyze);

        ImageView ivSearch = root.findViewById(R.id.ivSearch);
        ImageView ivMenu = root.findViewById(R.id.ivMenu);
        ImageView ivAssistantLogo = root.findViewById(R.id.ivAssistantLogo);

        ImageView ivPaperclip = root.findViewById(R.id.ivPaperclip);
        View flSparkleAction = root.findViewById(R.id.flSparkleAction);
        ImageView ivMic = root.findViewById(R.id.ivMic);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnSend.setImageTintList(ColorStateList.valueOf(Color.parseColor("#0A7AF5")));
                } else {
                    btnSend.setImageTintList(ColorStateList.valueOf(Color.parseColor("#999999")));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty() || selectedImageUri != null) {
                assistantViewModel.sendMessage(text, selectedImageUri, requireContext());
                etMessage.setText("");
                selectedImageUri = null;
                ivPaperclip.clearColorFilter();
            }
        });

        EditText etSearchChats = root.findViewById(R.id.etSearchChats);
        
        // Header click listeners
        ivSearch.setOnClickListener(v -> {
            animateClickAndRun(ivSearch, () -> {
                if (etSearchChats.getVisibility() == View.VISIBLE) {
                    etSearchChats.setVisibility(View.GONE);
                    etSearchChats.setText("");
                    chatAdapter.updateMessages(assistantViewModel.getMessages().getValue());
                    ivSearch.setColorFilter(Color.parseColor("#4A4A4A"));
                } else {
                    etSearchChats.setVisibility(View.VISIBLE);
                    etSearchChats.requestFocus();
                    ivSearch.setColorFilter(Color.parseColor("#0A7AF5"));
                }
            });
        });
        
        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                List<ChatMessage> fullList = assistantViewModel.getMessages().getValue();
                if (fullList == null) return;
                List<ChatMessage> filtered = new ArrayList<>();
                for (ChatMessage msg : fullList) {
                    if (msg.getText().toLowerCase().contains(query)) {
                        filtered.add(msg);
                    }
                }
                chatAdapter.updateMessages(filtered);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivMenu.setOnClickListener(v -> {
            ivMenu.setColorFilter(Color.parseColor("#0A7AF5"));
            animateClickAndRun(ivMenu, () -> {
                drawerLayout.openDrawer(GravityCompat.END);
                RecyclerView rvHistory = root.findViewById(R.id.recyclerViewHistoryDrawer);
                rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvHistory.setAdapter(new HistoryAdapter(assistantViewModel.getSessionList(requireContext()), sessionId -> {
                    assistantViewModel.loadSession(sessionId, requireContext());
                    drawerLayout.closeDrawer(GravityCompat.END);
                }));
            });
        });
        
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                ivMenu.setColorFilter(Color.parseColor("#4A4A4A"));
            }
        });
        
        View btnNewConversation = root.findViewById(R.id.btnNewConversation);
        btnNewConversation.setOnClickListener(v -> {
            animateClickAndRun(btnNewConversation, () -> {
                assistantViewModel.startNewSession();
                drawerLayout.closeDrawer(GravityCompat.END);
            });
        });
        
        ivAssistantLogo.setOnClickListener(v -> {
            animateClickAndRun(ivAssistantLogo, () -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Nexus AI Settings")
                    .setMessage("Configure your Nexus AI Assistant preferences.\n\n• Model: Gemini 1.5 Pro\n• Analytics: Enabled\n• Safe-Mode Filter: Strict")
                    .setPositiveButton("OK", null)
                    .show();
            });
        });

        // Chip click listeners
        chipScamCheck.setOnClickListener(v -> {
            etMessage.setText("Is this price a scam? ");
            etMessage.setSelection(etMessage.getText().length());
        });
        chipHelp.setOnClickListener(v -> {
            etMessage.setText("Can you help me with ");
            etMessage.setSelection(etMessage.getText().length());
        });
        chipTranslate.setOnClickListener(v -> {
            etMessage.setText("Translate this: ");
            etMessage.setSelection(etMessage.getText().length());
        });
        chipAnalyze.setOnClickListener(v -> {
            etMessage.setText("Analyze this: ");
            etMessage.setSelection(etMessage.getText().length());
        });

        // Input icon click listeners
        ivPaperclip.setOnClickListener(v -> {
            try {
                pickMedia.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Photo picker not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        });
        
        ivMic.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your query...");
            try {
                speechRecognizerLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Speech recognition not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        });
        
        flSparkleAction.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Nexus Sparkle Action activated!", Toast.LENGTH_SHORT).show();
            etMessage.setText("✨ Make this safe: ");
            etMessage.setSelection(etMessage.getText().length());
        });

        return root;
    }

    private void showTypingIndicator() {
        // Typing indicator logic can be implemented here later
    }

    private void hideTypingIndicator() {
        // Typing indicator logic can be implemented here later
    }

    // sendMessage deprecated for ViewModel strict handling
}

package com.safetour.ai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.safetour.ai.databinding.FragmentAssistantBinding;
import java.util.ArrayList;
import java.util.List;

public class AssistantFragment extends Fragment {

    private FragmentAssistantBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAssistantBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        messages = new ArrayList<>();
        messages.add(new ChatMessage("Hello! I am your AI Safety Assistant. Paste any taxi or market price, and I'll tell you if it's a scam.", false));

        chatAdapter = new ChatAdapter(messages);
        binding.recyclerViewChat.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewChat.setAdapter(chatAdapter);

        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                binding.etMessage.setText("");
            }
        });

        return root;
    }

    private void sendMessage(String text) {
        // Add User Message
        messages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.recyclerViewChat.scrollToPosition(messages.size() - 1);

        // Mock AI Response (Placeholder for Gemini API)
        binding.recyclerViewChat.postDelayed(() -> {
            messages.add(new ChatMessage("Based on recent data, that price seems higher than average. Be careful and try to negotiate.", false));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            binding.recyclerViewChat.scrollToPosition(messages.size() - 1);
        }, 1500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

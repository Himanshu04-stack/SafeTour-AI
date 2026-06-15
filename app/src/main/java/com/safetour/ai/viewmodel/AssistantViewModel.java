package com.safetour.ai.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.safetour.ai.BuildConfig;
import com.safetour.ai.ui.ChatMessage;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AssistantViewModel extends ViewModel {

    private final MutableLiveData<List<ChatMessage>> _messages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ChatMessage>> getMessages() { return _messages; }

    private final MutableLiveData<Boolean> _isTyping = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsTyping() { return _isTyping; }

    private ChatFutures chatSession;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private boolean isInitialized = false;
    private String lastKnownLocation = "Unknown GPS Location";
    private String currentSessionId = null;

    public AssistantViewModel() {
        initInitialGreeting();
    }
    
    private void initInitialGreeting() {
        List<ChatMessage> initial = new ArrayList<>();
        initial.add(new ChatMessage("Hello! I am Nexus, your intelligent SafeTour AI travel companion. I am currently offline because the Gemini API Key is missing. Please add it to `local.properties` to securely wake me up!", false));
        _messages.setValue(initial);
    }
    
    public void updateLocationContext(String loc) {
        this.lastKnownLocation = loc;
    }

    public void initChat(String liveLocationContext) {
        if (isInitialized) return;
        
        try {
            String apiKey = "AIzaSyCpzpYi0U6R6PT_ZOZo_cnGYvnYzXahXEg"; 
            if (apiKey.isEmpty()) {
                List<ChatMessage> errorState = new ArrayList<>();
                errorState.add(new ChatMessage("System Warning: Live Compiler Cache Error. The app compiler is injecting the string `" + apiKey + "` instead of your real AIza... key. Try forcing a Clean Project again or manually rebuilding the 'app' module.", false));
                _messages.postValue(errorState);
                return; // Engine blocked
            }

            String systemPrompt = "You are Nexus, an ultra-intelligent, professional, and slightly futuristic digital travel companion and safety assistant. " +
                    "You provide hyper-relevant local advice, verify taxi prices, warn about scams, and guide the user in emergency scenarios. " +
                    "You are concise but warm. The user's live physical GPS context right now is: " + liveLocationContext;
                    
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.7f;
            
            ArrayList<SafetySetting> safetySettings = new ArrayList<>();
            safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH));
            safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));

            GenerativeModel gm = new GenerativeModel(
                    "gemini-2.5-flash",
                    apiKey,
                    configBuilder.build(),
                    safetySettings
            );

            List<Content> history = new ArrayList<>();
            Content.Builder b1 = new Content.Builder();
            b1.setRole("user");
            b1.addText("[SYSTEM DIRECTIVE: " + systemPrompt + "]");
            history.add(b1.build());

            Content.Builder b2 = new Content.Builder();
            b2.setRole("model");
            b2.addText("Acknowledged. I am Nexus and I will adhere strictly to these directives.");
            history.add(b2.build());

            GenerativeModelFutures model = GenerativeModelFutures.from(gm);
            chatSession = model.startChat(history);
            isInitialized = true;
            
            List<ChatMessage> connectedState = new ArrayList<>();
            connectedState.add(new ChatMessage("Hello! I am Nexus, your intelligent SafeTour AI travel companion. I have linked to your GPS and am standing by to assist with your journey. How can I help you navigate safely today?", false));
            _messages.postValue(connectedState);
        } catch (Exception e) {
            List<ChatMessage> errorState = new ArrayList<>();
            errorState.add(new ChatMessage("CRITICAL CRASH: " + e.toString(), false));
            _messages.postValue(errorState);
        }
    }

    public void sendMessage(String userText, Uri imageUri, Context context) {
        List<ChatMessage> currentList = new ArrayList<>(_messages.getValue());
        currentList.add(new ChatMessage(userText.isEmpty() ? "[Image Attached]" : userText, true));
        _messages.setValue(currentList);

        if (chatSession == null) {
            currentList.add(new ChatMessage("Error: Neural Engine is offline. Please configure your Gemini API Key in `local.properties`.", false));
            _messages.setValue(currentList);
            return;
        }

        _isTyping.setValue(true);
        saveSession(context);

        Content.Builder cb = new Content.Builder();
        cb.setRole("user");
        
        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
                cb.addImage(bitmap);
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }
        
        cb.addText("[Slight context update: User is at " + lastKnownLocation + "] " + userText);
        Content content = cb.build();
        
        ListenableFuture<GenerateContentResponse> response = chatSession.sendMessage(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                _isTyping.postValue(false);
                List<ChatMessage> updatedList = new ArrayList<>(_messages.getValue());
                updatedList.add(new ChatMessage(result.getText().trim(), false));
                _messages.postValue(updatedList);
                saveSession(context);
            }

            @Override
            public void onFailure(Throwable t) {
                _isTyping.postValue(false);
                List<ChatMessage> updatedList = new ArrayList<>(_messages.getValue());
                updatedList.add(new ChatMessage("Connection failed: " + t.getMessage(), false));
                _messages.postValue(updatedList);
            }
        }, executor);
    }
    
    public void startNewSession() {
        currentSessionId = "Chat " + android.text.format.DateFormat.format("MMM dd, HH:mm:ss", new java.util.Date());
        chatSession = null;
        isInitialized = false;
        initInitialGreeting();
        initChat(lastKnownLocation);
    }

    public void saveSession(Context context) {
        if (currentSessionId == null) {
            currentSessionId = "Chat " + android.text.format.DateFormat.format("MMM dd, HH:mm:ss", new java.util.Date());
        }
        SharedPreferences prefs = context.getSharedPreferences("SafeTourAI_Chats", Context.MODE_PRIVATE);
        
        try {
            JSONArray array = new JSONArray();
            List<ChatMessage> currentMsgs = _messages.getValue();
            if (currentMsgs != null) {
                for (ChatMessage msg : currentMsgs) {
                    JSONObject obj = new JSONObject();
                    obj.put("text", msg.getText());
                    obj.put("isUser", msg.isUser());
                    array.put(obj);
                }
            }
            prefs.edit().putString(currentSessionId, array.toString()).apply();
            
            String listStr = prefs.getString("SessionList", "[]");
            JSONArray listArray = new JSONArray(listStr);
            boolean exists = false;
            for(int i=0; i<listArray.length(); i++) {
                if (listArray.getString(i).equals(currentSessionId)) exists = true;
            }
            if (!exists) {
                listArray.put(currentSessionId);
                prefs.edit().putString("SessionList", listArray.toString()).apply();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<String> getSessionList(Context context) {
        List<String> sessions = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences("SafeTourAI_Chats", Context.MODE_PRIVATE);
        try {
            String listStr = prefs.getString("SessionList", "[]");
            JSONArray array = new JSONArray(listStr);
            for (int i=0; i < array.length(); i++) {
                sessions.add(array.getString(i));
            }
        } catch (Exception e) {}
        return sessions;
    }

    public void loadSession(String sessionId, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SafeTourAI_Chats", Context.MODE_PRIVATE);
        String chatJson = prefs.getString(sessionId, null);
        if (chatJson == null) return;
        
        currentSessionId = sessionId;
        List<ChatMessage> loaded = new ArrayList<>();
        List<Content> history = new ArrayList<>();
        
        String systemPrompt = "You are Nexus, an ultra-intelligent, professional, and slightly futuristic digital travel companion and safety assistant. You provide hyper-relevant local advice, verify taxi prices, warn about scams, and guide the user in emergency scenarios. You are concise but warm. The user's live physical GPS context right now is: " + lastKnownLocation;
        Content.Builder b1 = new Content.Builder(); b1.setRole("user"); b1.addText("[SYSTEM DIRECTIVE: " + systemPrompt + "]"); history.add(b1.build());
        Content.Builder b2 = new Content.Builder(); b2.setRole("model"); b2.addText("Acknowledged. I am Nexus and I will adhere strictly to these directives."); history.add(b2.build());
        
        try {
            JSONArray array = new JSONArray(chatJson);
            for (int i=0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                boolean isUser = obj.getBoolean("isUser");
                String text = obj.getString("text");
                loaded.add(new ChatMessage(text, isUser));
                
                if (!text.startsWith("Hello! I am Nexus") && !text.startsWith("System Warning") && !text.startsWith("Error:") && !text.startsWith("CRITICAL CRASH:")) {
                    Content.Builder cb = new Content.Builder();
                    cb.setRole(isUser ? "user" : "model");
                    cb.addText(text);
                    history.add(cb.build());
                }
            }
            _messages.setValue(loaded);
            
            String apiKey = "AIzaSyCpzpYi0U6R6PT_ZOZo_cnGYvnYzXahXEg"; 
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.7f;
            ArrayList<SafetySetting> safetySettings = new ArrayList<>();
            safetySettings.add(new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH));
            safetySettings.add(new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH));

            GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", apiKey, configBuilder.build(), safetySettings);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);
            chatSession = model.startChat(history);
            isInitialized = true;
            
        } catch (Exception e) { e.printStackTrace(); }
    }
}

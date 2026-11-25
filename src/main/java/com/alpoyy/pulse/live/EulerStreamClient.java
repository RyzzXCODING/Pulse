package com.alpoyy.pulse.live;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class EulerStreamClient {
    private static final String API_BASE = "https://api.eulerstream.com";
    private static final String WS_BASE = "wss://api.eulerstream.com";
    
    private final HttpClient httpClient;
    private final ConfigManager configManager;
    private final Gson gson = new Gson();
    
    private WebSocket webSocket;
    private Consumer<LiveChatEvent> eventHandler;
    private String username;
    private String apiKey;
    private boolean connected = false;
    private boolean shouldReconnect = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int RECONNECT_BASE_DELAY = 5000; // 5 seconds
    
    public EulerStreamClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.configManager = ConfigManager.getInstance();
        loadConfig();
    }
    
    private void loadConfig() {
        PulseConfig config = configManager.getConfig();
        this.apiKey = config.eulerstreamApiKey;
        this.username = config.tiktokUsername;
    }
    
    public void setConfig(String apiKey, String username) {
        this.apiKey = apiKey;
        this.username = username;
        
        PulseConfig config = configManager.getConfig();
        config.eulerstreamApiKey = apiKey;
        config.tiktokUsername = username;
        configManager.saveAsync();
    }
    
    public void setEventHandler(Consumer<LiveChatEvent> handler) {
        this.eventHandler = handler;
    }
    
    public CompletableFuture<Boolean> connect() {
        if (connected) {
            return CompletableFuture.completedFuture(true);
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        if (username == null || username.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        shouldReconnect = true;
        return connectWebSocket();
    }
    
    private CompletableFuture<Boolean> connectWebSocket() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String wsUrl = WS_BASE + "/v1/connect?username=" + username + "&api_key=" + apiKey;
        
        httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    Pulse.LOGGER.info("EulerStream WebSocket connected");
                    EulerStreamClient.this.webSocket = webSocket;
                    connected = true;
                    reconnectAttempts = 0;
                    webSocket.request(1);
                    future.complete(true);
                }
                
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    try {
                        JsonObject json = JsonParser.parseString(data.toString()).getAsJsonObject();
                        handleMessage(json);
                    } catch (Exception e) {
                        Pulse.LOGGER.warn("Failed to parse WebSocket message", e);
                    }
                    webSocket.request(1);
                    return CompletableFuture.completedFuture(null);
                }
                
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    Pulse.LOGGER.error("EulerStream WebSocket error", error);
                    connected = false;
                    if (!future.isDone()) {
                        future.complete(false);
                    }
                    scheduleReconnect();
                }
                
                @Override
                public void onClose(WebSocket webSocket, int statusCode, String reason) {
                    Pulse.LOGGER.info("EulerStream WebSocket closed: {} - {}", statusCode, reason);
                    connected = false;
                    if (!future.isDone()) {
                        future.complete(false);
                    }
                    scheduleReconnect();
                }
            });
        
        return future;
    }
    
    private void handleMessage(JsonObject json) {
        String eventType = json.get("event").getAsString();
        
        switch (eventType) {
            case "chat" -> handleChatEvent(json);
            case "gift" -> handleGiftEvent(json);
            case "like" -> handleLikeEvent(json);
            case "follow" -> handleFollowEvent(json);
            case "join" -> handleJoinEvent(json);
            case "live_start" -> handleLiveStart(json);
            case "live_end" -> handleLiveEnd(json);
            default -> Pulse.LOGGER.debug("Unhandled EulerStream event: {}", eventType);
        }
    }
    
    private void handleChatEvent(JsonObject json) {
        LiveChatEvent event = new LiveChatEvent();
        event.setEventType("comment");
        event.setUsername(json.get("username").getAsString());
        event.setUserId(json.get("user_id").getAsString());
        event.setMessage(json.get("comment").getAsString());
        event.setModerator(json.get("is_moderator").getAsBoolean());
        event.setSubscriber(json.get("is_subscriber").getAsBoolean());
        
        if (json.has("badge")) {
            event.setBadge(json.get("badge").getAsString());
        }
        if (json.has("color")) {
            event.setColor(json.get("color").getAsString());
        }
        
        if (eventHandler != null) {
            eventHandler.accept(event);
        }
    }
    
    private void handleGiftEvent(JsonObject json) {
        LiveChatEvent event = new LiveChatEvent();
        event.setEventType("gift");
        event.setUsername(json.get("username").getAsString());
        event.setMessage("Sent gift: " + json.get("gift_name").getAsString());
        
        if (eventHandler != null) {
            eventHandler.accept(event);
        }
    }
    
    private void handleLikeEvent(JsonObject json) {
        // Like events typically don't have individual messages
    }
    
    private void handleFollowEvent(JsonObject json) {
        LiveChatEvent event = new LiveChatEvent();
        event.setEventType("follow");
        event.setUsername(json.get("username").getAsString());
        event.setMessage("Started following!");
        
        if (eventHandler != null) {
            eventHandler.accept(event);
        }
    }
    
    private void handleJoinEvent(JsonObject json) {
        LiveChatEvent event = new LiveChatEvent();
        event.setEventType("join");
        event.setUsername(json.get("username").getAsString());
        event.setMessage("Joined the live!");
        
        if (eventHandler != null) {
            eventHandler.accept(event);
        }
    }
    
    private void handleLiveStart(JsonObject json) {
        Pulse.LOGGER.info("TikTok LIVE started for @{}", username);
        connected = true;
    }
    
    private void handleLiveEnd(JsonObject json) {
        Pulse.LOGGER.info("TikTok LIVE ended for @{}", username);
        connected = false;
    }
    
    private void scheduleReconnect() {
        if (!shouldReconnect) return;
        
        PulseConfig config = configManager.getConfig();
        if (!config.autoReconnect) return;
        
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Pulse.LOGGER.warn("Max reconnect attempts reached for EulerStream");
            return;
        }
        
        reconnectAttempts++;
        int delay = RECONNECT_BASE_DELAY * reconnectAttempts;
        
        Pulse.LOGGER.info("Scheduling EulerStream reconnect in {}ms (attempt {})", delay, reconnectAttempts);
        
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                if (shouldReconnect) {
                    connectWebSocket();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "EulerStream-Reconnect").start();
    }
    
    public void disconnect() {
        shouldReconnect = false;
        connected = false;
        
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Disconnected by user");
            webSocket = null;
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void sendMessage(String message) {
        if (webSocket != null && connected) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "send_message");
            json.addProperty("message", message);
            webSocket.sendText(json.toString(), true);
        }
    }
}
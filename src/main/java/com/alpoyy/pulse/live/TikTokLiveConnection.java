package com.alpoyy.pulse.live;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.request.RequestManager;
import com.alpoyy.pulse.widget.ChatWidget;
import com.alpoyy.pulse.widget.NotificationWidget;

import java.util.concurrent.CompletableFuture;

public class TikTokLiveConnection {
    private final EulerStreamClient eulerStreamClient;
    private final ConfigManager configManager;
    private final RequestManager requestManager;
    
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private String lastError;
    
    public TikTokLiveConnection() {
        this.configManager = ConfigManager.getInstance();
        this.eulerStreamClient = new EulerStreamClient();
        this.requestManager = RequestManager.getInstance();
        
        setupEventHandler();
    }
    
    private void setupEventHandler() {
        eulerStreamClient.setEventHandler(event -> {
            // Forward to chat widget
            ChatWidget chatWidget = (ChatWidget) com.alpoyy.pulse.widget.WidgetManager.getInstance().getWidget("chat");
            if (chatWidget != null) {
                chatWidget.addMessage(event);
            }
            
            // Forward to notification widget
            NotificationWidget notifWidget = (NotificationWidget) com.alpoyy.pulse.widget.WidgetManager.getInstance().getWidget("notifications");
            if (notifWidget != null) {
                notifWidget.addNotification("New message from " + event.getUsername(), event.getMessage());
            }
            
            // Check for song requests
            if (event.isChatMessage()) {
                requestManager.processChatMessage(event.getUsername(), event.getMessage());
            }
        });
    }
    
    public CompletableFuture<Boolean> connect() {
        if (state == ConnectionState.CONNECTING || state == ConnectionState.CONNECTED) {
            return CompletableFuture.completedFuture(true);
        }
        
        state = ConnectionState.CONNECTING;
        lastError = null;
        
        return eulerStreamClient.connect().thenApply(success -> {
            if (success) {
                state = ConnectionState.CONNECTED;
                Pulse.LOGGER.info("TikTok LIVE connection established");
            } else {
                state = ConnectionState.ERROR;
                lastError = "Failed to connect to EulerStream. Check API key and username.";
                Pulse.LOGGER.error(lastError);
            }
            return success;
        }).exceptionally(ex -> {
            state = ConnectionState.ERROR;
            lastError = "Connection error: " + ex.getMessage();
            Pulse.LOGGER.error("TikTok LIVE connection failed", ex);
            return false;
        });
    }
    
    public void disconnect() {
        eulerStreamClient.disconnect();
        state = ConnectionState.DISCONNECTED;
        Pulse.LOGGER.info("TikTok LIVE disconnected");
    }
    
    public ConnectionState getState() {
        // Update state based on EulerStream client
        if (eulerStreamClient.isConnected()) {
            state = ConnectionState.CONNECTED;
        } else if (state == ConnectionState.CONNECTED) {
            state = ConnectionState.DISCONNECTED;
        }
        return state;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public String getUsername() {
        return eulerStreamClient.getUsername();
    }
    
    public String getApiKey() {
        return eulerStreamClient.getApiKey();
    }
    
    public void setCredentials(String apiKey, String username) {
        eulerStreamClient.setConfig(apiKey, username);
    }
    
    public boolean isLive() {
        return eulerStreamClient.isConnected();
    }
    
    public void sendChatMessage(String message) {
        eulerStreamClient.sendMessage(message);
    }
    
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}
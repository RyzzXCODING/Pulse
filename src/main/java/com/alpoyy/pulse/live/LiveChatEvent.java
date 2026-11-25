package com.alpoyy.pulse.live;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public class LiveChatEvent {
    @SerializedName("event_type")
    private String eventType;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("timestamp")
    private Instant timestamp;
    
    @SerializedName("is_moderator")
    private boolean isModerator;
    
    @SerializedName("is_subscriber")
    private boolean isSubscriber;
    
    @SerializedName("badge")
    private String badge;
    
    @SerializedName("color")
    private String color;
    
    public LiveChatEvent() {
        this.timestamp = Instant.now();
    }
    
    public LiveChatEvent(String eventType, String username, String message) {
        this();
        this.eventType = eventType;
        this.username = username;
        this.message = message;
    }
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isModerator() {
        return isModerator;
    }
    
    public void setModerator(boolean moderator) {
        isModerator = moderator;
    }
    
    public boolean isSubscriber() {
        return isSubscriber;
    }
    
    public void setSubscriber(boolean subscriber) {
        isSubscriber = subscriber;
    }
    
    public String getBadge() {
        return badge;
    }
    
    public void setBadge(String badge) {
        this.badge = badge;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public boolean isChatMessage() {
        return "comment".equals(eventType) || "chat".equals(eventType);
    }
    
    public boolean isGift() {
        return "gift".equals(eventType);
    }
    
    public boolean isLike() {
        return "like".equals(eventType);
    }
    
    public boolean isFollow() {
        return "follow".equals(eventType);
    }
    
    public boolean isJoin() {
        return "join".equals(eventType) || "member".equals(eventType);
    }
    
    @Override
    public String toString() {
        return "LiveChatEvent{" +
                "eventType='" + eventType + '\'' +
                ", username='" + username + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
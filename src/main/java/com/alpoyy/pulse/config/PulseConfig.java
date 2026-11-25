package com.alpoyy.pulse.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PulseConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "pulse_config.json";
    
    // UI Settings
    @SerializedName("ui_opacity")
    public float uiOpacity = 0.9f;
    
    @SerializedName("widget_scale")
    public float widgetScale = 1.0f;
    
    @SerializedName("glow_intensity")
    public float glowIntensity = 0.5f;
    
    @SerializedName("background_opacity")
    public float backgroundOpacity = 0.7f;
    
    @SerializedName("accent_color")
    public String accentColor = "#BB86FC"; // Purple
    
    @SerializedName("secondary_accent_color")
    public String secondaryAccentColor = "#03DAC6"; // Cyan
    
    // Widget Visibility
    @SerializedName("widget_visibility")
    public Map<String, Boolean> widgetVisibility = new HashMap<>();
    
    // Widget Positions
    @SerializedName("widget_positions")
    public Map<String, WidgetPosition> widgetPositions = new HashMap<>();
    
    // Audio Settings
    @SerializedName("master_volume")
    public float masterVolume = 1.0f;
    
    @SerializedName("music_volume")
    public float musicVolume = 0.8f;
    
    // Song Request Settings
    @SerializedName("request_command_prefix")
    public String requestCommandPrefix = "!";
    
    @SerializedName("request_command")
    public String requestCommand = "req";
    
    @SerializedName("queue_command")
    public String queueCommand = "queue";
    
    @SerializedName("myreq_command")
    public String myreqCommand = "myreq";
    
    @SerializedName("song_command")
    public String songCommand = "song";
    
    @SerializedName("skip_command")
    public String skipCommand = "skip";
    
    @SerializedName("clear_command")
    public String clearCommand = "clear";
    
    @SerializedName("help_command")
    public String helpCommand = "help";
    
    @SerializedName("max_queue_size")
    public int maxQueueSize = 50;
    
    @SerializedName("max_requests_per_user")
    public int maxRequestsPerUser = 3;
    
    @SerializedName("user_cooldown_seconds")
    public int userCooldownSeconds = 30;
    
    @SerializedName("duplicate_prevention")
    public boolean duplicatePrevention = true;
    
    @SerializedName("auto_clear_completed")
    public boolean autoClearCompleted = true;
    
    @SerializedName("queue_persistence")
    public boolean queuePersistence = false;
    
    // TikTok LIVE / EulerStream Settings
    @SerializedName("eulerstream_api_key")
    public String eulerstreamApiKey = "";
    
    @SerializedName("tiktok_username")
    public String tiktokUsername = "";
    
    @SerializedName("auto_reconnect")
    public boolean autoReconnect = true;
    
    @SerializedName("reconnect_interval_seconds")
    public int reconnectIntervalSeconds = 10;
    
    @SerializedName("show_chat_widget")
    public boolean showChatWidget = true;
    
    @SerializedName("max_chat_messages")
    public int maxChatMessages = 50;
    
    // Platform Connections
    @SerializedName("platform_tokens")
    public Map<String, String> platformTokens = new HashMap<>();
    
    // Local Music
    @SerializedName("local_music_directory")
    public String localMusicDirectory = "";
    
    // Keybinds (stored as strings for serialization)
    @SerializedName("keybind_open_dashboard")
    public String keybindOpenDashboard = "key.keyboard.p";
    
    @SerializedName("keybind_toggle_music_widget")
    public String keybindToggleMusicWidget = "key.keyboard.m";
    
    @SerializedName("keybind_toggle_request_widget")
    public String keybindToggleRequestWidget = "key.keyboard.n";
    
    @SerializedName("keybind_play_pause")
    public String keybindPlayPause = "key.keyboard.k";
    
    public PulseConfig() {
        // Initialize default widget visibility
        widgetVisibility.put("music", true);
        widgetVisibility.put("live", true);
        widgetVisibility.put("queue", true);
        widgetVisibility.put("chat", false);
        widgetVisibility.put("notifications", true);
        
        // Initialize default widget positions
        widgetPositions.put("music", new WidgetPosition(10, 10));
        widgetPositions.put("live", new WidgetPosition(10, 150));
        widgetPositions.put("queue", new WidgetPosition(10, 290));
        widgetPositions.put("chat", new WidgetPosition(10, 430));
        widgetPositions.put("notifications", new WidgetPosition(300, 10));
    }
    
    public static class WidgetPosition {
        public int x;
        public int y;
        
        public WidgetPosition() {
            this(0, 0);
        }
        
        public WidgetPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    public Color getAccentColor() {
        try {
            return Color.decode(accentColor);
        } catch (NumberFormatException e) {
            return new Color(187, 134, 252); // Default purple
        }
    }
    
    public Color getSecondaryAccentColor() {
        try {
            return Color.decode(secondaryAccentColor);
        } catch (NumberFormatException e) {
            return new Color(3, 218, 198); // Default cyan
        }
    }
    
    public void setAccentColor(Color color) {
        this.accentColor = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    public void setSecondaryAccentColor(Color color) {
        this.secondaryAccentColor = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
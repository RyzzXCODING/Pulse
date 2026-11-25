package com.alpoyy.pulse.config;

import com.alpoyy.pulse.Pulse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static ConfigManager instance;
    private final PulseConfig config;
    private final File configFile;
    private final Gson gson;
    
    private ConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configFile = new File(getConfigDir(), PulseConfig.CONFIG_FILE);
        this.config = loadConfig();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public PulseConfig getConfig() {
        return config;
    }
    
    public void load() {
        PulseConfig loaded = loadConfig();
        if (loaded != null) {
            copyConfig(loaded, config);
            Pulse.LOGGER.info("Configuration loaded from {}", configFile.getAbsolutePath());
        } else {
            Pulse.LOGGER.info("Using default configuration");
            save();
        }
    }
    
    private PulseConfig loadConfig() {
        if (!configFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            return gson.fromJson(reader, PulseConfig.class);
        } catch (IOException e) {
            Pulse.LOGGER.error("Failed to load configuration", e);
            return null;
        }
    }
    
    public void save() {
        try {
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }
            Pulse.LOGGER.debug("Configuration saved to {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            Pulse.LOGGER.error("Failed to save configuration", e);
        }
    }
    
    public void saveAsync() {
        new Thread(this::save, "Pulse-Config-Save").start();
    }
    
    private void copyConfig(PulseConfig from, PulseConfig to) {
        to.uiOpacity = from.uiOpacity;
        to.widgetScale = from.widgetScale;
        to.glowIntensity = from.glowIntensity;
        to.backgroundOpacity = from.backgroundOpacity;
        to.accentColor = from.accentColor;
        to.secondaryAccentColor = from.secondaryAccentColor;
        to.widgetVisibility.clear();
        to.widgetVisibility.putAll(from.widgetVisibility);
        to.widgetPositions.clear();
        to.widgetPositions.putAll(from.widgetPositions);
        to.masterVolume = from.masterVolume;
        to.musicVolume = from.musicVolume;
        to.requestCommandPrefix = from.requestCommandPrefix;
        to.requestCommand = from.requestCommand;
        to.queueCommand = from.queueCommand;
        to.myreqCommand = from.myreqCommand;
        to.songCommand = from.songCommand;
        to.skipCommand = from.skipCommand;
        to.clearCommand = from.clearCommand;
        to.helpCommand = from.helpCommand;
        to.maxQueueSize = from.maxQueueSize;
        to.maxRequestsPerUser = from.maxRequestsPerUser;
        to.userCooldownSeconds = from.userCooldownSeconds;
        to.duplicatePrevention = from.duplicatePrevention;
        to.autoClearCompleted = from.autoClearCompleted;
        to.queuePersistence = from.queuePersistence;
        to.eulerstreamApiKey = from.eulerstreamApiKey;
        to.tiktokUsername = from.tiktokUsername;
        to.autoReconnect = from.autoReconnect;
        to.reconnectIntervalSeconds = from.reconnectIntervalSeconds;
        to.showChatWidget = from.showChatWidget;
        to.maxChatMessages = from.maxChatMessages;
        to.platformTokens.clear();
        to.platformTokens.putAll(from.platformTokens);
        to.localMusicDirectory = from.localMusicDirectory;
        to.keybindOpenDashboard = from.keybindOpenDashboard;
        to.keybindToggleMusicWidget = from.keybindToggleMusicWidget;
        to.keybindToggleRequestWidget = from.keybindToggleRequestWidget;
        to.keybindPlayPause = from.keybindPlayPause;
    }
    
    private static File getConfigDir() {
        String minecraftDir = System.getProperty("minecraft.client.dir", ".");
        return Paths.get(minecraftDir, "config").toFile();
    }
    
    public File getConfigFile() {
        return configFile;
    }
}
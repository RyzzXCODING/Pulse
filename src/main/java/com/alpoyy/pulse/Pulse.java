package com.alpoyy.pulse;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pulse implements ModInitializer {
    public static final String MOD_ID = "pulse";
    public static final String MOD_NAME = "Pulse";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Pulse v{}", VERSION);
        
        // Initialize configuration
        com.alpoyy.pulse.config.ConfigManager.getInstance().load();
        
        // Initialize platform manager
        com.alpoyy.pulse.platform.PlatformManager.getInstance().initialize();
        
        // Initialize playback engine
        com.alpoyy.pulse.music.PlaybackController.getInstance().initialize();
        
        // Initialize request manager
        com.alpoyy.pulse.request.RequestManager.getInstance().initialize();
        
        // Initialize widget manager
        com.alpoyy.pulse.widget.WidgetManager.getInstance().initialize();
        
        LOGGER.info("Pulse initialized successfully!");
    }
}
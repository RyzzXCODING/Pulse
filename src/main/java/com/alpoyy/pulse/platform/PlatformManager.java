package com.alpoyy.pulse.platform;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.platform.google.YouTubeConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlatformManager {
    private static PlatformManager instance;
    
    private final Map<String, PlatformConnector> connectors = new HashMap<>();
    
    private PlatformManager() {
        // Register built-in connectors
        registerConnector(new YouTubeConnector());
        // Future: registerConnector(new SpotifyConnector());
        // Future: registerConnector(new SoundCloudConnector());
    }
    
    public static PlatformManager getInstance() {
        if (instance == null) {
            instance = new PlatformManager();
        }
        return instance;
    }
    
    public void initialize() {
        // Initialize all connectors
        for (PlatformConnector connector : connectors.values()) {
            try {
                if (connector.isAuthenticated()) {
                    connector.getConnectionStatus(); // Trigger token validation
                }
            } catch (Exception e) {
                Pulse.LOGGER.warn("Failed to initialize connector {}: {}", connector.getPlatformId(), e.getMessage());
            }
        }
        Pulse.LOGGER.info("PlatformManager initialized with {} connectors", connectors.size());
    }
    
    public void registerConnector(PlatformConnector connector) {
        if (connector != null) {
            connectors.put(connector.getPlatformId(), connector);
            Pulse.LOGGER.debug("Registered platform connector: {}", connector.getPlatformName());
        }
    }
    
    public void unregisterConnector(String platformId) {
        connectors.remove(platformId);
    }
    
    public PlatformConnector getConnector(String platformId) {
        return connectors.get(platformId);
    }
    
    public Map<String, PlatformConnector> getAllConnectors() {
        return new HashMap<>(connectors);
    }
    
    public CompletableFuture<PlatformConnector.AuthenticationResult> authenticate(String platformId) {
        PlatformConnector connector = connectors.get(platformId);
        if (connector == null) {
            return CompletableFuture.completedFuture(
                new PlatformConnector.AuthenticationResult(false, "Unknown platform: " + platformId, null));
        }
        return connector.authenticate();
    }
    
    public void disconnect(String platformId) {
        PlatformConnector connector = connectors.get(platformId);
        if (connector != null) {
            connector.disconnect();
        }
    }
    
    public boolean isConnected(String platformId) {
        PlatformConnector connector = connectors.get(platformId);
        return connector != null && connector.isAuthenticated();
    }
    
    public void shutdown() {
        for (PlatformConnector connector : connectors.values()) {
            try {
                connector.disconnect();
            } catch (Exception e) {
                Pulse.LOGGER.warn("Error disconnecting {}: {}", connector.getPlatformId(), e.getMessage());
            }
        }
        connectors.clear();
    }
}
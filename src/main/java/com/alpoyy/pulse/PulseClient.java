package com.alpoyy.pulse;

import com.alpoyy.pulse.config.ConfigManager;
import com.alpoyy.pulse.gui.DashboardScreen;
import com.alpoyy.pulse.keybind.PulseKeybinds;
import com.alpoyy.pulse.music.PlaybackController;
import com.alpoyy.pulse.platform.PlatformManager;
import com.alpoyy.pulse.request.RequestManager;
import com.alpoyy.pulse.util.ScreenUtils;
import com.alpoyy.pulse.widget.WidgetManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulseClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PulseClient");
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    
    private static boolean dashboardOpened = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Pulse Client");
        
        // Register keybindings
        PulseKeybinds.register();
        
        // Register client tick event for keybind handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeybinds(client);
        });
        
        // Register HUD render callback for widgets
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (client.player != null && client.world != null) {
                WidgetManager.getInstance().renderWidgets(drawContext, tickDelta);
            }
        });
        
        LOGGER.info("Pulse Client initialized successfully!");
    }
    
    private void handleKeybinds(MinecraftClient client) {
        // Open Dashboard
        while (PulseKeybinds.OPEN_DASHBOARD.wasPressed()) {
            if (client.currentScreen == null) {
                openDashboard(client);
            } else if (client.currentScreen instanceof DashboardScreen) {
                client.setScreen(null);
            }
        }
        
        // Toggle Music Widget
        while (PulseKeybinds.TOGGLE_MUSIC_WIDGET.wasPressed()) {
            WidgetManager.getInstance().toggleWidget("music");
        }
        
        // Toggle Song Request Widget
        while (PulseKeybinds.TOGGLE_REQUEST_WIDGET.wasPressed()) {
            WidgetManager.getInstance().toggleWidget("queue");
        }
        
        // Play/Pause
        while (PulseKeybinds.PLAY_PAUSE.wasPressed()) {
            PlaybackController.getInstance().togglePlayPause();
        }
    }
    
    public static void openDashboard(MinecraftClient client) {
        if (client.player != null) {
            client.setScreen(new DashboardScreen(Text.translatable("gui.pulse.dashboard.title")));
        }
    }
    
    public static boolean isDashboardOpened() {
        return dashboardOpened;
    }
    
    public static void setDashboardOpened(boolean opened) {
        dashboardOpened = opened;
    }
}
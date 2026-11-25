package com.alpoyy.pulse.keybind;

import com.alpoyy.pulse.Pulse;
import com.alpoyy.pulse.config.ConfigManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PulseKeybinds {
    public static KeyBinding OPEN_DASHBOARD;
    public static KeyBinding TOGGLE_MUSIC_WIDGET;
    public static KeyBinding TOGGLE_REQUEST_WIDGET;
    public static KeyBinding PLAY_PAUSE;
    
    private static final String CATEGORY = "key.category.pulse.general";
    
    public static void register() {
        ConfigManager configManager = ConfigManager.getInstance();
        
        OPEN_DASHBOARD = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pulse.open_dashboard",
            parseKeyCode(configManager.getConfig().keybindOpenDashboard),
            CATEGORY
        ));
        
        TOGGLE_MUSIC_WIDGET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pulse.toggle_music_widget",
            parseKeyCode(configManager.getConfig().keybindToggleMusicWidget),
            CATEGORY
        ));
        
        TOGGLE_REQUEST_WIDGET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pulse.toggle_request_widget",
            parseKeyCode(configManager.getConfig().keybindToggleRequestWidget),
            CATEGORY
        ));
        
        PLAY_PAUSE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pulse.play_pause",
            parseKeyCode(configManager.getConfig().keybindPlayPause),
            CATEGORY
        ));
        
        Pulse.LOGGER.debug("Keybindings registered");
    }
    
    private static int parseKeyCode(String keyString) {
        if (keyString == null || keyString.isEmpty()) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
        
        // Handle "key.keyboard.x" format
        if (keyString.startsWith("key.keyboard.")) {
            String keyName = keyString.substring("key.keyboard.".length()).toUpperCase();
            try {
                return GLFW.class.getField("GLFW_KEY_" + keyName).getInt(null);
            } catch (Exception e) {
                Pulse.LOGGER.warn("Invalid keybind: {}", keyString);
            }
        }
        
        // Handle direct GLFW key names
        try {
            return GLFW.class.getField("GLFW_KEY_" + keyString.toUpperCase()).getInt(null);
        } catch (Exception e) {
            // Try to parse as integer
            try {
                return Integer.parseInt(keyString);
            } catch (NumberFormatException ex) {
                Pulse.LOGGER.warn("Could not parse keybind: {}", keyString);
            }
        }
        
        return GLFW.GLFW_KEY_UNKNOWN;
    }
    
    public static void updateKeybind(KeyBinding keyBinding, String translationKey, String newKeyString) {
        int keyCode = parseKeyCode(newKeyString);
        keyBinding.setBoundKey(KeyBinding.fromKeyCode(keyCode));
    }
    
    public static String getKeyDisplayName(KeyBinding keyBinding) {
        return keyBinding.getBoundKeyLocalizedText().getString();
    }
}
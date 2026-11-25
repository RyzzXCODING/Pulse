package com.alpoyy.pulse.util;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ScreenUtils {
    
    public static void openScreen(Screen screen) {
        net.minecraft.client.MinecraftClient.getInstance().setScreen(screen);
    }
    
    public static void openScreenWithTitle(Screen screen, String title) {
        net.minecraft.client.MinecraftClient.getInstance().setScreen(screen);
    }
    
    public static Text literal(String text) {
        return Text.literal(text);
    }
    
    public static Text translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
}
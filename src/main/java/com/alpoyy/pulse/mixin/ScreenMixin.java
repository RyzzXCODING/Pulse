package com.alpoyy.pulse.mixin;

import com.alpoyy.pulse.PulseClient;
import com.alpoyy.pulse.gui.DashboardScreen;
import com.alpoyy.pulse.widget.WidgetManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        
        // If closing dashboard, reset state
        if (screen instanceof DashboardScreen) {
            PulseClient.setDashboardOpened(false);
            WidgetManager.getInstance().setEditorMode(false);
        }
    }
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onScreenInit(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        
        // If opening dashboard, set state
        if (screen instanceof DashboardScreen) {
            PulseClient.setDashboardOpened(true);
        }
    }
}
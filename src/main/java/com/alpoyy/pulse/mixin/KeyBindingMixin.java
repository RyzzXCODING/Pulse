package com.alpoyy.pulse.mixin;

import com.alpoyy.pulse.PulseClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Inject(method = "setPressed", at = @At("HEAD"))
    private void onKeyPressed(boolean pressed, CallbackInfo ci) {
        // This mixin ensures keybindings are properly handled
        // The actual handling is done in PulseClient via ClientTickEvents
    }
}
package com.mods.invfull;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

/** Automatic messages and sounds. They all honour the "Notifications" switch of the config screen. */
final class Notify {
    private Notify() {}

    static void message(LocalPlayer player, String text) {
        if (ConfigManager.getConfig().notificationsEnabled) {
            player.sendSystemMessage(Component.literal(text));
        }
    }

    static void sound(LocalPlayer player, SoundEvent sound, float volume, float pitch) {
        if (ConfigManager.getConfig().notificationsEnabled) {
            player.playSound(sound, volume, pitch);
        }
    }
}

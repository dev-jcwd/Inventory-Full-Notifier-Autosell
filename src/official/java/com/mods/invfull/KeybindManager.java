package com.mods.invfull;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class KeybindManager {
    private static KeyMapping configScreenKey;
    private static KeyMapping autosellSwitchKey;

    public static void registerKeybinds() {
        // The translation keys are kept exactly as in the 1.21.x versions so saved key bindings carry over.
        configScreenKey = KeyMappingHelper.registerKeyMapping(
                Compat.createKeyMapping("IFN+Autosell Config", "key.keyboard.grave.accent")); // ~ key
        autosellSwitchKey = KeyMappingHelper.registerKeyMapping(
                Compat.createKeyMapping("IFN+Autosell Switch", null)); // not bound by default

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configScreenKey.consumeClick()) {
                if (client.player != null && !(Compat.currentScreen(client) instanceof ConfigScreen)) {
                    Compat.openScreen(client, new ConfigScreen(null));
                }
            }

            while (autosellSwitchKey.consumeClick()) {
                switchAutosellMode(client);
            }
        });
    }

    /** Switches the autosell mode to the next available mode: all -> handall -> custom -> all. */
    private static void switchAutosellMode(Minecraft client) {
        IFNConfig config = ConfigManager.getConfig();

        switch (config.autosellMode) {
            case "all":
                config.autosellMode = "handall";
                break;
            case "handall":
                // skip "custom" while no custom command has been set, it would do nothing
                config.autosellMode = config.customCommand.isBlank() ? "all" : "custom";
                break;
            default:
                config.autosellMode = "all";
                break;
        }
        ConfigManager.save();

        if (client.player != null) {
            client.player.sendSystemMessage(
                    Component.literal("§6[IFN+Autosell]§r Autosell mode switched to: §e" + config.autosellMode));
        }
    }

    /** The localized display name of the config screen keybind. */
    public static String getConfigKeybindDisplayName() {
        if (configScreenKey == null) {
            return "~"; // Default fallback
        }
        return configScreenKey.getTranslatedKeyMessage().getString();
    }

    /** The localized display name of the autosell switch keybind. */
    public static String getAutosellSwitchKeybindDisplayName() {
        if (autosellSwitchKey == null) {
            return "Not Bound"; // Default fallback
        }
        return autosellSwitchKey.getTranslatedKeyMessage().getString();
    }
}

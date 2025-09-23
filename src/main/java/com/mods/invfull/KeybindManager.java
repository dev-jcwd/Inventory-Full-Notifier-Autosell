package com.mods.invfull;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindManager {
    private static KeyBinding configScreenKey;
    private static KeyBinding autosellSwitchKey;
    
    public static void registerKeybinds() {
        // Register config screen keybind
        configScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "IFN+Autosell Config", // Translation key
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT, // ~ key
            "IFN+Autosell" // Category
        ));
        
        // Register autosell mode switch keybind
        autosellSwitchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "IFN+Autosell Switch", // Translation key
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(), // Not bound by default
            "IFN+Autosell" // Category
        ));
        
        // Register tick event for handling keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (configScreenKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ConfigScreen(null));
            }
            
            if (autosellSwitchKey.wasPressed()) {
                switchAutosellMode();
            }
        });
    }
    
    /**
     * Switches the autosell mode to the next available mode
     */
    private static void switchAutosellMode() {
        IFNConfig config = ConfigManager.getConfig();
        
        // Cycle through autosell modes: all -> handall -> custom -> all
        switch (config.autosellMode) {
            case "all":
                config.autosellMode = "handall";
                break;
            case "handall":
                config.autosellMode = "custom";
                break;
            case "custom":
                config.autosellMode = "all";
                break;
            default:
                config.autosellMode = "all";
                break;
        }
        
        // Save the config
        ConfigManager.save();
        
        // Send feedback message to player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                net.minecraft.text.Text.literal("§6[IFN+Autosell]§r Autosell mode switched to: §e" + config.autosellMode),
                false
            );
        }
    }
    /**
     * Gets the localized display name of the config screen keybind
     * @return The display name of the currently bound key
     */
    public static String getConfigKeybindDisplayName() {
        if (configScreenKey == null) {
            return "~"; // Default fallback
        }
        return configScreenKey.getBoundKeyLocalizedText().getString();
    }
    
    /**
     * Gets the localized display name of the autosell switch keybind
     * @return The display name of the currently bound key
     */
    public static String getAutosellSwitchKeybindDisplayName() {
        if (autosellSwitchKey == null) {
            return "Not Bound"; // Default fallback
        }
        return autosellSwitchKey.getBoundKeyLocalizedText().getString();
    }
}
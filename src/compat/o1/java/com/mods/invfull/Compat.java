package com.mods.invfull;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/** Minecraft 26.1: screens live directly on Minecraft. */
public final class Compat {
    private Compat() {}

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("inventorynotifier_autosell", "main"));

    /** Keys are looked up by name at runtime because the numeric key codes changed in 26.3. A null name means "unbound". */
    public static KeyMapping createKeyMapping(String translationKey, String keyName) {
        int code = keyName == null ? InputConstants.UNKNOWN.getValue() : InputConstants.getKey(keyName).getValue();
        return new KeyMapping(translationKey, code, CATEGORY);
    }

    public static Screen currentScreen(Minecraft client) { return client.screen; }

    public static void openScreen(Minecraft client, Screen screen) { client.setScreen(screen); }
}

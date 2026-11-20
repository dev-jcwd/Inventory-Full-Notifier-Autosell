package com.mods.invfull;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;

public final class InventoryUtil {
    private InventoryUtil() {}

    /** Counts how many of the 36 main inventory slots (hotbar included) hold an item. One pass over the slots. */
    public static int countUsedSlots(LocalPlayer player) {
        if (player == null) return 0;
        Inventory inventory = player.getInventory();
        int used = 0;
        for (int i = 0; i < IFNConfig.SLOTS; i++) {
            if (!inventory.getItem(i).isEmpty()) used++;
        }
        return used;
    }
}

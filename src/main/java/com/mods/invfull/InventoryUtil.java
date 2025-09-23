package com.mods.invfull;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public final class InventoryUtil {
    private InventoryUtil() {}

    /**
     * Returns true if the player's main inventory (36 slots, including hotbar) has no empty slots.
     */
    public static boolean isInventoryWithoutEmptySlots(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack == null || stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if there are zero empty slots AND all stacks are at their max size.
     */
    public static boolean isHardFull(ClientPlayerEntity player) {
        if (!InventoryUtil.isInventoryWithoutEmptySlots(player)) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getCount() < stack.getMaxCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts how many of the 36 main inventory slots are empty.
     */
    public static int countFreeSlots(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return 0;
        int free = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack == null || stack.isEmpty()) {
                free++;
            }
        }
        return free;
    }

    /**
     * Counts how many of the 36 main inventory slots are currently occupied.
     */
    public static int countUsedSlots(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return 0;
        int used = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty()) {
                used++;
            }
        }
        return used;
    }
}

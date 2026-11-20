package com.mods.invfull;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * All the per-tick logic: "inventory full" notice, autosell, "inventory empty" notice and the milestones.
 * One instance, driven by one tick listener. Nothing here ever registers another listener.
 */
final class InventoryMonitor {
    private static final int JOIN_GRACE_TICKS = 40;      // the server sends the inventory shortly after joining
    private static final int AUTOSELL_WAIT_TICKS = 10;   // ~0.5 s for the server to process the sell command
    private static final int EMPTY_MESSAGE_DELAY_TICKS = 15;

    private int graceTicks = JOIN_GRACE_TICKS;

    private boolean wasFull = false;
    private boolean wasAtThreshold = false;

    private int autosellTicksLeft = -1; // -1 = no autosell running
    private int usedBeforeSell = 0;

    private boolean hit50 = false;
    private boolean hit90 = false;
    private boolean emptyNotified = false;
    private int emptyTicks = -1;

    /** Forget everything: called when joining or leaving a world so stale state never leaks into the next one. */
    void reset() {
        graceTicks = JOIN_GRACE_TICKS;
        wasFull = false;
        wasAtThreshold = false;
        autosellTicksLeft = -1;
        hit50 = false;
        hit90 = false;
        emptyNotified = false;
        emptyTicks = -1;
    }

    private boolean autosellRunning() {
        return autosellTicksLeft >= 0;
    }

    void tick(LocalPlayer player) {
        if (graceTicks > 0) {
            graceTicks--;
            return;
        }

        IFNConfig config = ConfigManager.getConfig();
        int used = InventoryUtil.countUsedSlots(player);
        boolean full = used >= IFNConfig.SLOTS;

        // --- "Inventory full" notice (only once each time it becomes full) ---
        if (full && !wasFull) {
            Notify.message(player, "§e[Inventory] Your inventory is full!");
            Notify.sound(player, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.8f);
        }
        wasFull = full;

        // --- Autosell: starts when the used slots reach the threshold (36 = completely full) ---
        boolean atThreshold = used >= config.autosellThreshold;
        if (config.autosellEnabled && atThreshold && !wasAtThreshold && !autosellRunning()) {
            startAutosell(player, config, used);
        }
        wasAtThreshold = atThreshold;

        if (autosellTicksLeft > 0) {
            autosellTicksLeft--;
        } else if (autosellTicksLeft == 0) {
            finishAutosell(player, config, used);
            autosellTicksLeft = -1;
        }

        // --- Empty notice and milestones ---
        if (config.milestonesEnabled) {
            tickMilestones(player, config, used);
        }
    }

    private void startAutosell(LocalPlayer player, IFNConfig config, int used) {
        String command = autosellCommand(config);
        if (command == null) {
            Notify.message(player, "§c[Autosell] No custom command set. Use §f/autosell set custom <command>");
            return;
        }
        Notify.message(player, "§b[Autosell] Selling items... Mode: " + config.autosellMode);
        player.connection.sendCommand(command);
        usedBeforeSell = used;
        autosellTicksLeft = AUTOSELL_WAIT_TICKS;
    }

    /** The command for the current mode, without the leading slash, or null if there is nothing to run. */
    private static String autosellCommand(IFNConfig config) {
        switch (config.autosellMode) {
            case "handall":
                return "sell handall";
            case "custom":
                String custom = config.customCommand.trim();
                if (custom.startsWith("/")) custom = custom.substring(1).trim();
                return custom.isEmpty() ? null : custom;
            default:
                return "sell all";
        }
    }

    private void finishAutosell(LocalPlayer player, IFNConfig config, int used) {
        int free = IFNConfig.SLOTS - used;
        if (used == 0) {
            Notify.message(player, "§a[Autosell] Inventory sold! Now Empty! §r(" + free + " slots free)");
            Notify.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);
            emptyNotified = true; // no separate "inventory is empty" notice right after selling
            emptyTicks = -1;
        } else if (used < usedBeforeSell) {
            Notify.message(player, "§a[Autosell] Items sold! §r(" + free + " slots free)");
        } else {
            Notify.message(player, "§c[Autosell] No sellable items. §rInventory unchanged.");
            return;
        }
        // Start the milestones from the new fill level so none of them fires just because items were sold.
        float percent = used * 100f / IFNConfig.SLOTS;
        hit50 = percent >= 50;
        hit90 = used >= config.milestoneThreshold;
    }

    private void tickMilestones(LocalPlayer player, IFNConfig config, int used) {
        int free = IFNConfig.SLOTS - used;
        float percent = used * 100f / IFNConfig.SLOTS;
        boolean suppress = false;

        // --- Inventory empty ---
        if (used == 0) {
            if (!emptyNotified) {
                emptyNotified = true;
                hit50 = false;
                hit90 = false;
                emptyTicks = 0;
            }

            if (emptyTicks >= EMPTY_MESSAGE_DELAY_TICKS) {
                Notify.message(player, "§a[Notifier] Inventory is empty! §r(" + free + " slots left)");
                Notify.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);
                emptyTicks = -1;
            } else if (emptyTicks >= 0) {
                emptyTicks++;
            }

            suppress = true; // while empty, don't spam milestones
        } else {
            emptyNotified = false;
            emptyTicks = -1;
        }

        // --- Milestones (not while empty and not while an autosell is running) ---
        if (suppress || autosellRunning()) return;

        if (percent >= 50 && !hit50) {
            hit50 = true;
            Notify.message(player, "§e[Notifier] Inventory 50% full! §r(" + free + " slots left)");
            Notify.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.2f);
        } else if (percent < 50 && hit50) {
            hit50 = false;
            Notify.message(player, "§a[Notifier] Inventory dropped below 50%! §r(" + free + " slots left)");
            Notify.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.8f);
        }

        // Configurable threshold instead of a hardcoded 90%
        int thresholdPercent = (int) (config.milestoneThreshold * 100f / IFNConfig.SLOTS);
        if (used >= config.milestoneThreshold && !hit90) {
            hit90 = true;
            Notify.message(player, "§6[Notifier] Inventory " + thresholdPercent + "% full! §r(" + free + " slots left)");
            Notify.sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 0.6f);
        } else if (used < config.milestoneThreshold && hit90) {
            hit90 = false;
            Notify.message(player, "§a[Notifier] Inventory dropped below " + thresholdPercent + "%! §r(" + free + " slots left)");
            Notify.sound(player, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.5f);
        }
    }
}

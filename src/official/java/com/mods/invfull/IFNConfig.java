package com.mods.invfull;

public class IFNConfig {
    public static final int SLOTS = 36; // main inventory including the hotbar

    public boolean autosellEnabled = false;
    public String autosellMode = "all"; // "all", "handall" or "custom"
    public boolean milestonesEnabled = true;
    public String customCommand = ""; // Custom command for autosell
    public int autosellThreshold = SLOTS; // Autosell starts once this many slots are used (36 = completely full)
    public boolean notificationsEnabled = true; // Toggle for all automatic messages and sounds
    public int milestoneThreshold = 32; // Default to 90% of inventory (32/36 slots)

    /** Repairs values from a hand-edited, empty or older file so the rest of the mod can trust them. */
    void sanitize() {
        if (!"all".equals(autosellMode) && !"handall".equals(autosellMode) && !"custom".equals(autosellMode)) {
            autosellMode = "all";
        }
        if (customCommand == null) customCommand = "";
        autosellThreshold = Math.max(1, Math.min(SLOTS, autosellThreshold));
        milestoneThreshold = Math.max(1, Math.min(SLOTS, milestoneThreshold));
    }
}

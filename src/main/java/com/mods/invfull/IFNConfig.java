package com.mods.invfull;

public class IFNConfig {
    public boolean autosellEnabled = false;
    public String autosellMode = "all"; // "all" or "handall"
    public boolean milestonesEnabled = true;
    public boolean slotNotifierEnabled = true; // optional
    public String customCommand = ""; // Custom command for autosell
    public int autosellThreshold = 36; // When to activate autosell (1-36 slots)
    public boolean notificationsEnabled = true; // Toggle for all notifications
    public String configKeybind = "grave"; // Default keybind for config screen (~ key)
    public int milestoneThreshold = 32; // Default to 90% of inventory (32/36 slots)
}

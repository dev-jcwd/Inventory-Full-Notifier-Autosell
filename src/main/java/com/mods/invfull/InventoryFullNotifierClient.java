package com.mods.invfull;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

    public class InventoryFullNotifierClient implements ClientModInitializer {
    // === Autosell ===
    private static boolean wasInventoryFull = false;
    private static boolean autosellActive = false;

    // === Milestones ===
    private static boolean hit50 = false;
    private static boolean hit90 = false;
    private static boolean milestonesPrevState = true;

    // === Inventory tracking ===
    private static int lastUsedSlots = 0;
    private static long lastInvUpdate = 0;

    // === Empty notifier ===
    private static boolean emptyNotified = false;
    private static int emptyMessageTicks = -1;

    @Override
    public void onInitializeClient() {
        System.out.println("[InventoryFullNotifier] Loaded. Use /invfull help for commands.");

        // === Load config first ===
        ConfigManager.load();
        
        // Register keybinds
        KeybindManager.registerKeybinds();

        // Add welcome message when player joins
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Wait a few ticks to ensure player is fully loaded
            client.execute(() -> {
                if (client.player != null) {
                    // Get the localized key name from the public method
                    String keybindName = KeybindManager.getConfigKeybindDisplayName();

                    client.player.sendMessage(
                        Text.literal("§6§lIFN+Autosell §r§aActive! §rPress §e[" + keybindName + "] §rto see config!"),
                        false
                    );
                }
            });
        });

        // === Tick event (pass player like old version) ===
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                checkInventory(client.player); // this one has milestones
                
                // Check for autosell based on threshold
                IFNConfig config = ConfigManager.getConfig();
                if (config.autosellEnabled) {
                    checkInventoryAndAutoSell(client.player);
                }
            }
        });

        // === Register commands ===
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerAutosellCommand(dispatcher);
            registerSlotNotifyCommand(dispatcher);
            registerHelpCommand(dispatcher);
            registerConfigCommand(dispatcher);
        });
    }
    
    // ========== /invfull config ==========
    private void registerConfigCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("invfull")
                .then(ClientCommandManager.literal("config")
                    .executes(ctx -> {
                        MinecraftClient.getInstance().setScreen(new ConfigScreen(null));
                        return 1;
                    })
                )
        );
    }

    // ========== /autosell ==========
    private void registerAutosellCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("autosell")
                .executes(ctx -> {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        IFNConfig config = ConfigManager.getConfig();
                        config.autosellEnabled = !config.autosellEnabled;

                        // === Save toggle ===
                        ConfigManager.save();

                        player.sendMessage(Text.literal(
                            config.autosellEnabled ? "§e[Autosell] §aEnabled! Mode: " + config.autosellMode
                                                   : "§e[Autosell] §cDisabled!"
                        ), false);
                    }
                    return 1;
                })
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.literal("all").executes(ctx -> {
                        setMode("all");
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("handall").executes(ctx -> {
                        setMode("handall");
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("custom")
                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                                if (player == null) return 1;

                                // Get the custom command and store it properly
                                String customCmd = StringArgumentType.getString(ctx, "command");
                                if (customCmd.startsWith("/")) {
                                    customCmd = customCmd.substring(1); // strip leading /
                                }

                                IFNConfig config = ConfigManager.getConfig();
                                config.autosellMode = "custom"; // Set mode to custom
                                config.customCommand = customCmd; // Store the actual command
                                config.autosellEnabled = true;

                                // === Save custom mode + toggle ===
                                ConfigManager.save();

                                player.sendMessage(Text.literal(
                                    "§e[Autosell] §aEnabled! Custom command set to: §f/" + customCmd
                                ), false);
                                return 1;
                            })
                        )
                    )
                )
        );
    }

    // Helper to change mode
    private static void setMode(String mode) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        IFNConfig config = ConfigManager.getConfig();
        config.autosellMode = mode;
        config.autosellEnabled = true;

        // === Save mode + toggle ===
        ConfigManager.save();

        player.sendMessage(Text.literal("§e[Autosell] §aEnabled! Mode: §b" + config.autosellMode), false);
    }

    // Runs the correct autosell command depending on mode
    private static void runAutosellCommand(ClientPlayerEntity player) {
        IFNConfig config = ConfigManager.getConfig();
        if (player == null || !config.autosellEnabled) return;

        if ("all".equalsIgnoreCase(config.autosellMode)) {
            player.networkHandler.sendChatCommand("sell all");
        } else if ("handall".equalsIgnoreCase(config.autosellMode)) {
            player.networkHandler.sendChatCommand("sell handall");
        } else if ("custom".equalsIgnoreCase(config.autosellMode)) {
            // Use custom command from config
            if (config.customCommand != null && !config.customCommand.isEmpty()) {
                String cmd = config.customCommand;
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1); // strip leading /
                }
                player.networkHandler.sendChatCommand(cmd);
            }
        }
    }

    // Inventory full helper (only check main inventory slots 0-35)
    private static boolean isInventoryFull(ClientPlayerEntity player) {
        if (player == null) return false;

        // Only loop through the 36 main inventory slots
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                return false; // found at least one empty slot
            }
        }
        return true; // all 36 slots filled
    }

    // Check inventory and autosell
    private static void checkInventoryAndAutoSell(ClientPlayerEntity player) {
        boolean currentlyFull = isInventoryFull(player);
        IFNConfig config = ConfigManager.getConfig();
        
        if (currentlyFull && !wasInventoryFull) {
            // Inventory just became full
            wasInventoryFull = true;
            
            if ("immediate".equals(config.autosellMode)) {
                performAutoSell(player);
            } else if ("delayed".equals(config.autosellMode)) {
                // Start delayed autosell (could implement timer here)
                performAutoSell(player); // For now, immediate
            }
        } else if (!currentlyFull) {
            wasInventoryFull = false;
        }
    }

    private static void performAutoSell(ClientPlayerEntity player) {
        runAutosellCommand(player);
    }

    // ========== /slotnotify ==========
    private void registerSlotNotifyCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("slotnotify")
            .executes(ctx -> {
                IFNConfig config = ConfigManager.getConfig();
                config.milestonesEnabled = !config.milestonesEnabled;

                // === Save setting to config ===
                ConfigManager.save();

                ctx.getSource().sendFeedback(Text.literal(
                        "§e[Notifier] §rMilestone notifications " +
                        (config.milestonesEnabled ? "§aenabled ✅" : "§cdisabled ❌")
                ));
                return 1;
            })
        );
    }

    // ========== /invfull help ==========
    private void registerHelpCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("invfull")
            .then(ClientCommandManager.literal("help")
                .executes(context -> {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        // Header
                        player.sendMessage(Text.literal("§e=========== IFN+AutoSell v4 ==========="), false);

                        // Links
                        player.sendMessage(Text.literal("§bModrinth: §7https://modrinth.com/mod/inventoryfullnotifier+autosell"), false);
                        player.sendMessage(Text.literal("§bSuggestions & Issues: §7https://github.com/jcwd-source/InventoryNotifier-Autosell/issues"), false);

                        // Spacer
                        player.sendMessage(Text.literal(""), false);

                        // Commands
                        player.sendMessage(Text.literal("§eCommands:"), false);
                        player.sendMessage(Text.literal("§a/autosell §r- Enable/Disables autosell"), false);
                        player.sendMessage(Text.literal("§a/autosell set all §r- Switch autosell to /sell all"), false);
                        player.sendMessage(Text.literal("§a/autosell set handall §r- Switch autosell to /sell handall"), false);
                        player.sendMessage(Text.literal("§a/autosell set custom <cmd> §r- Switch custom command"), false);
                        player.sendMessage(Text.literal("§a/slotnotify §r- Toggle milestone notifications"), false);

                        // Spacer
                        player.sendMessage(Text.literal(""), false);

                        // Footer
                        player.sendMessage(Text.literal("§7IFN+Autosell Author: jcwd"), false);
                        player.sendMessage(Text.literal("§e=================================="), false);

                        // Play soft XP level-up sound
                        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.4f, 1.0f);
                    }
                    return 1;
                })
            )
        );
    }

    // ========== Inventory Logic ==========
    private void checkInventory(ClientPlayerEntity player) {
        IFNConfig config = ConfigManager.getConfig();
        boolean isFull = InventoryUtil.isInventoryWithoutEmptySlots(player);

        // Hard Full Handling
        if (isFull && !wasInventoryFull) {
            player.sendMessage(Text.literal("§e[Inventory] Your inventory is full!"), false);
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 0.8f);

            if (config.autosellEnabled) {
                autosellActive = true; // block milestones
                player.sendMessage(Text.literal("§b[Autosell] Selling items... Mode: " + config.autosellMode), false);
                runAutosellCommand(player);

                // Suppress milestones while autosell is running
                milestonesPrevState = config.milestonesEnabled;  // remember user's setting
                config.milestonesEnabled = false;

                // Delay check (10 ticks later ~0.5s)
                final int[] ticksWaited = {0};
                ClientTickEvents.END_CLIENT_TICK.register(client -> {
                    if (ticksWaited[0] == Integer.MIN_VALUE) return; // already done

                    if (ticksWaited[0]++ >= 10) {
                        int freeSlots = InventoryUtil.countFreeSlots(player);
                        if (freeSlots >= 36) {
                            player.sendMessage(Text.literal("§a[Autosell] Inventory sold! Now Empty! §r(" + freeSlots + " slots free)"), false);
                            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);

                            emptyNotified = true;
                            emptyMessageTicks = -1;

                            // Reset milestones (same as empty inventory)
                            hit50 = false;
                            hit90 = false;
                        } else if (freeSlots > 0) {
                            player.sendMessage(Text.literal("§a[Autosell] Items sold! §r(" + freeSlots + " slots free)"), false);

                            // Reset milestones too, so no milestone fires right after autosell
                            hit50 = false;
                            hit90 = false;
                        } else {
                            player.sendMessage(Text.literal("§c[Autosell] No sellable items. §rInventory still full."), false);
                        }

                        // Restore milestones to what the user had set before autosell
                        config.milestonesEnabled = milestonesPrevState;
                        autosellActive = false;

                        ticksWaited[0] = Integer.MIN_VALUE; // stop ticking
                    }
                });
            }

            wasInventoryFull = true;
        } else if (!isFull) {
            wasInventoryFull = false; // reset when inventory has space
    }

        // === Milestones with bulk-move debounce ===
        if (config.milestonesEnabled) {
            int used = InventoryUtil.countUsedSlots(player);
            int free = 36 - used;
            float percent = (used / 36.0f) * 100f;

            // Track any inventory change (useful if you rely on timing elsewhere)
            if (used != lastUsedSlots) {
                lastInvUpdate = System.currentTimeMillis();
                lastUsedSlots = used;
            }

            boolean suppressMilestones = false;

            // --- Inventory Empty (unchanged behavior) ---
            if (used == 0) {
                if (!emptyNotified) {
                    emptyNotified = true;
                    hit50 = false;
                    hit90 = false;
                    emptyMessageTicks = 0;
                }

                if (emptyMessageTicks >= 15) {
                    player.sendMessage(Text.literal("§a[Notifier] Inventory is empty! §r(" + free + " slots left)"), false);
                    player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);
                    emptyMessageTicks = -1;
                } else if (emptyMessageTicks >= 0) {
                    emptyMessageTicks++;
                }

                // While empty, don't spam milestones
                suppressMilestones = true;
            } else {
                emptyNotified = false;
                emptyMessageTicks = -1;
            }

            // --- Milestones (only if not suppressed by "empty" or autosell) ---
            if (!suppressMilestones && !autosellActive) {
                // Only show milestones if enabled in config
                if (config.milestonesEnabled) {
                    float thresholdPercent = (config.milestoneThreshold / 36.0f) * 100f;
                
                if (percent >= 50 && !hit50) {
                    hit50 = true;
                    player.sendMessage(Text.literal("§e[Notifier] Inventory 50% full! §r(" + free + " slots left)"), false);
                    player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.2f);
                } else if (percent < 50 && hit50) {
                    hit50 = false;
                    player.sendMessage(Text.literal("§a[Notifier] Inventory dropped below 50%! §r(" + free + " slots left)"), false);
                    player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.8f);
                }

                    // Use configurable threshold instead of hardcoded 90%
                    if (percent >= thresholdPercent && !hit90) {
                        hit90 = true;
                        player.sendMessage(Text.literal("§6[Notifier] Inventory " + (int)thresholdPercent + "% full! §r(" + free + " slots left)"), false);
                        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.6f);
                    } else if (percent < thresholdPercent && hit90) {
                        hit90 = false;
                        player.sendMessage(Text.literal("§a[Notifier] Inventory dropped below " + (int)thresholdPercent + "%! §r(" + free + " slots left)"), false);
                        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.5f);
                    }
                }
            }
        }
    }
}
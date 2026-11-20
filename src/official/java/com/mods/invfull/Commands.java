package com.mods.invfull;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** /autosell, /slotnotify and /invfull (help, config). Command feedback is never silenced by the Notifications switch. */
final class Commands {
    private Commands() {}

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        registerAutosell(dispatcher);
        registerSlotNotify(dispatcher);
        registerInvFull(dispatcher);
    }

    private static void say(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal(text));
    }

    // ========== /autosell ==========
    private static void registerAutosell(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommands.literal("autosell")
                .executes(ctx -> {
                    IFNConfig config = ConfigManager.getConfig();
                    config.autosellEnabled = !config.autosellEnabled;
                    ConfigManager.save();
                    say(ctx.getSource(), config.autosellEnabled
                            ? "§e[Autosell] §aEnabled! Mode: " + config.autosellMode
                            : "§e[Autosell] §cDisabled!");
                    return 1;
                })
                .then(ClientCommands.literal("set")
                    .then(ClientCommands.literal("all").executes(ctx -> setMode(ctx.getSource(), "all")))
                    .then(ClientCommands.literal("handall").executes(ctx -> setMode(ctx.getSource(), "handall")))
                    .then(ClientCommands.literal("custom")
                        .then(ClientCommands.argument("command", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String customCmd = StringArgumentType.getString(ctx, "command").trim();
                                if (customCmd.startsWith("/")) {
                                    customCmd = customCmd.substring(1).trim(); // strip leading /
                                }
                                if (customCmd.isEmpty()) {
                                    say(ctx.getSource(), "§c[Autosell] Usage: /autosell set custom <command>");
                                    return 0;
                                }

                                IFNConfig config = ConfigManager.getConfig();
                                config.autosellMode = "custom";
                                config.customCommand = customCmd;
                                config.autosellEnabled = true;
                                ConfigManager.save();

                                say(ctx.getSource(), "§e[Autosell] §aEnabled! Custom command set to: §f/" + customCmd);
                                return 1;
                            })
                        )
                    )
                )
        );
    }

    private static int setMode(FabricClientCommandSource source, String mode) {
        IFNConfig config = ConfigManager.getConfig();
        config.autosellMode = mode;
        config.autosellEnabled = true;
        ConfigManager.save();
        say(source, "§e[Autosell] §aEnabled! Mode: §b" + mode);
        return 1;
    }

    // ========== /slotnotify ==========
    private static void registerSlotNotify(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("slotnotify")
            .executes(ctx -> {
                IFNConfig config = ConfigManager.getConfig();
                config.milestonesEnabled = !config.milestonesEnabled;
                ConfigManager.save();
                say(ctx.getSource(), "§e[Notifier] §rMilestone notifications "
                        + (config.milestonesEnabled ? "§aenabled ✅" : "§cdisabled ❌"));
                return 1;
            })
        );
    }

    // ========== /invfull config | help ==========
    private static void registerInvFull(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("invfull")
            .then(ClientCommands.literal("config")
                .executes(ctx -> {
                    // Open it on the next main-thread pass: the chat screen that ran this command closes itself afterwards.
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> Compat.openScreen(client, new ConfigScreen(null)));
                    return 1;
                })
            )
            .then(ClientCommands.literal("help")
                .executes(ctx -> {
                    showHelp(ctx.getSource());
                    return 1;
                })
            )
        );
    }

    private static void showHelp(FabricClientCommandSource source) {
        String version = FabricLoader.getInstance().getModContainer("inventorynotifier_autosell")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");

        say(source, "§e=========== IFN+AutoSell " + version + " ===========");
        say(source, "§bModrinth: §7https://modrinth.com/mod/inventoryfullnotifier+autosell");
        say(source, "§bSuggestions & Issues: §7https://github.com/jcwd-source/InventoryNotifier-Autosell/issues");
        say(source, "");
        say(source, "§eCommands:");
        say(source, "§a/autosell §r- Enable/Disables autosell");
        say(source, "§a/autosell set all §r- Switch autosell to /sell all");
        say(source, "§a/autosell set handall §r- Switch autosell to /sell handall");
        say(source, "§a/autosell set custom <cmd> §r- Switch custom command");
        say(source, "§a/slotnotify §r- Toggle milestone notifications");
        say(source, "§a/invfull config §r- Open the config screen");
        say(source, "");
        say(source, "§7IFN+Autosell Author: jcwd");
        say(source, "§e==================================");

        // Play soft XP level-up sound
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.PLAYER_LEVELUP, 0.4f, 1.0f);
        }
    }
}

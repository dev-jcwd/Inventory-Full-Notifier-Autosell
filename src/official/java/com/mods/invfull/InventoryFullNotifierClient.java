package com.mods.invfull;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class InventoryFullNotifierClient implements ClientModInitializer {
    private static final InventoryMonitor MONITOR = new InventoryMonitor();

    @Override
    public void onInitializeClient() {
        System.out.println("[InventoryFullNotifier] Loaded. Use /invfull help for commands.");

        ConfigManager.load();
        KeybindManager.registerKeybinds();

        // Welcome message when joining; also forget all state from the previous world.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            MONITOR.reset();
            // client.player is not set yet while JOIN fires, so look it up on the main thread afterwards
            client.execute(() -> {
                if (client.player != null) {
                    String keybindName = KeybindManager.getConfigKeybindDisplayName();
                    client.player.sendSystemMessage(
                        Component.literal("§6§lIFN+Autosell §r§aActive! §rPress §e[" + keybindName + "] §rto see config!"));
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MONITOR.reset());

        // The single tick listener for everything
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                MONITOR.tick(client.player);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Commands.register(dispatcher));
    }
}

package com.mods.invfull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("ifn_config.json");
    private static IFNConfig config;

    public static IFNConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void load() {
        IFNConfig loaded = null;
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                loaded = GSON.fromJson(reader, IFNConfig.class); // null for an empty file
            } catch (IOException | JsonParseException e) {
                // Keep the broken file as .bak so the player's settings are not lost when defaults are written over it.
                System.err.println("[IFN+Autosell] Could not read config, using defaults: " + e);
                try {
                    Files.move(CONFIG_FILE, CONFIG_FILE.resolveSibling("ifn_config.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            }
        }
        boolean existed = loaded != null;
        config = existed ? loaded : new IFNConfig();
        config.sanitize();
        if (!existed) save(); // write defaults
    }

    public static void save() {
        if (config == null) return;
        Path tmp = CONFIG_FILE.resolveSibling("ifn_config.json.tmp");
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(config, writer);
            }
            // write-then-rename so a crash mid-save cannot truncate the real file
            Files.move(tmp, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[IFN+Autosell] Could not save config: " + e);
        }
    }
}

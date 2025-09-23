package com.mods.invfull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/ifn_config.json");
    private static IFNConfig config;

    public static IFNConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                config = new IFNConfig();
                save(); // write defaults
            } else {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, IFNConfig.class);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            config = new IFNConfig(); // fallback
        }
    }

    public static void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

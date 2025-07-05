package com.wh0oo.meteorolocracy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VoteConfig {
    private static final File CONFIG_FILE = new File("config/meteorolocracy.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData config = new ConfigData(); // Defaults

    static {
        load();
    }

    public static long getVoteDuration() {
        return config.voteDurationSeconds;
    }

    public static long getPlayerCooldown() {
        return config.playerCooldownSeconds;
    }

    public static double getVoteThreshold() {
        return config.voteThreshold;
    }

    public static boolean shouldEndEarly() {
        return config.endEarlyIfAllVoted;
    }

    private static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // write defaults
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) config = loaded;
        } catch (IOException e) {
            System.err.println("[Meteorolocracy] Failed to load config: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[Meteorolocracy] Failed to save config: " + e.getMessage());
        }
    }

    private static class ConfigData {
        long voteDurationSeconds = 120;
        long playerCooldownSeconds = 10800;
        double voteThreshold = 0.5;
        boolean endEarlyIfAllVoted = true;
    }
}

package com.asiantiertagger.config;

import com.asiantiertagger.AsianTierTaggerClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

/**
 * ModConfig — persists all user-configurable settings to a JSON file in
 * {@code .minecraft/config/asiantiertagger.json}.
 *
 * <p>Call {@link #load()} on startup and {@link #save()} whenever a setting changes.
 */
public class ModConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("asiantiertagger.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------

    private static ModConfig INSTANCE = new ModConfig();

    public static ModConfig get() { return INSTANCE; }

    // -------------------------------------------------------------------------
    // Config fields (with defaults)
    // -------------------------------------------------------------------------

    /** Whether the mod is currently active. */
    private boolean enabled = true;

    /** Base URL of the tier API (no trailing slash). */
    private String apiUrl = "https://0967657c-9763-44ef-ae07-ceb60fac80b0-00-aqup5s2ew8nf.sisko.replit.dev";

    /** How often to refresh player tiers, in seconds. */
    private int refreshIntervalSeconds = 30;

    /** Show tier tag above player heads in the world. */
    private boolean showAboveHead = true;

    /** Show tier tag in the player tab list. */
    private boolean showInTab = true;

    /** Show tier tag prefixed to chat messages. */
    private boolean showInChat = true;

    /** Hide the local player's own tier tag. */
    private boolean hideOwnTag = false;

    /** Active gamemode filter (empty string = all gamemodes). */
    private String gamemodeFilter = "";

    // -------------------------------------------------------------------------
    // Load / Save
    // -------------------------------------------------------------------------

    /** Loads config from disk, creating the file with defaults if it doesn't exist. */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                AsianTierTaggerClient.LOGGER.info("[AsianTierTagger] Config loaded from {}", CONFIG_PATH);
            } catch (Exception e) {
                AsianTierTaggerClient.LOGGER.error("[AsianTierTagger] Failed to load config: {}", e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
            AsianTierTaggerClient.LOGGER.info("[AsianTierTagger] Default config written to {}", CONFIG_PATH);
        }
    }

    /** Saves the current config to disk. */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            AsianTierTaggerClient.LOGGER.error("[AsianTierTagger] Failed to save config: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public boolean isEnabled()                     { return enabled; }
    public void setEnabled(boolean v)              { enabled = v; }

    public String getApiUrl()                      { return apiUrl; }
    public void setApiUrl(String v)                { apiUrl = v; }

    public int getRefreshIntervalSeconds()         { return refreshIntervalSeconds; }
    public void setRefreshIntervalSeconds(int v)   { refreshIntervalSeconds = Math.max(5, v); }

    public boolean isShowAboveHead()               { return showAboveHead; }
    public void setShowAboveHead(boolean v)        { showAboveHead = v; }

    public boolean isShowInTab()                   { return showInTab; }
    public void setShowInTab(boolean v)            { showInTab = v; }

    public boolean isShowInChat()                  { return showInChat; }
    public void setShowInChat(boolean v)           { showInChat = v; }

    public boolean isHideOwnTag()                  { return hideOwnTag; }
    public void setHideOwnTag(boolean v)           { hideOwnTag = v; }

    public String getGamemodeFilter()              { return gamemodeFilter; }
    public void setGamemodeFilter(String v)        { gamemodeFilter = v == null ? "" : v.trim(); }
}

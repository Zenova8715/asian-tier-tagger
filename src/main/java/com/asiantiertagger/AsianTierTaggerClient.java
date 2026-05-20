package com.asiantiertagger;

import com.asiantiertagger.api.TierApiManager;
import com.asiantiertagger.cache.PlayerCacheManager;
import com.asiantiertagger.commands.TierCommand;
import com.asiantiertagger.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AsianTierTagger — Client entrypoint.
 * Initializes config, cache, API manager, commands, and the auto-refresh tick loop.
 */
public class AsianTierTaggerClient implements ClientModInitializer {

    public static final String MOD_ID = "asiantiertagger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Ticks between auto-refresh attempts (20 ticks = 1 second). */
    private static final int TICKS_PER_SECOND = 20;

    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AsianTierTagger] Initializing...");

        // Load config from disk (creates default if absent)
        ModConfig.load();

        // Register /asiantiers command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                TierCommand.register(dispatcher));

        // Auto-refresh ticker: runs every N seconds based on config
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.get().isEnabled()) return;
            tickCounter++;
            int refreshIntervalTicks = ModConfig.get().getRefreshIntervalSeconds() * TICKS_PER_SECOND;
            if (tickCounter >= refreshIntervalTicks) {
                tickCounter = 0;
                refreshAllVisible(client);
            }
        });

        LOGGER.info("[AsianTierTagger] Ready. API URL: {}", ModConfig.get().getApiUrl());
    }

    /**
     * Queues async API fetches for every player currently visible on the tab list.
     */
    private void refreshAllVisible(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return;

        client.getNetworkHandler().getPlayerList().forEach(entry -> {
            String name = entry.getProfile().getName();
            if (name == null || name.isBlank()) return;

            // Only fetch if not already cached (cache has its own TTL)
            if (!PlayerCacheManager.isCached(name)) {
                TierApiManager.fetchAsync(name);
            }
        });
    }
}

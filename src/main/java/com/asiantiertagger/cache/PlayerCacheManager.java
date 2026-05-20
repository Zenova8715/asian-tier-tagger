package com.asiantiertagger.cache;

import com.asiantiertagger.api.PlayerTierData;
import com.asiantiertagger.api.TierApiManager;
import com.asiantiertagger.config.ModConfig;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerCacheManager — thread-safe in-memory cache for tier data.
 *
 * <p>Each entry has a configurable TTL (default 30 s). After expiry,
 * the next lookup triggers a background re-fetch so rendering is never blocked.
 */
public class PlayerCacheManager {

    /** Cache entry wrapper that tracks when the data was stored. */
    private record CacheEntry(PlayerTierData data, long timestampMs) {
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestampMs > ttlMs;
        }
    }

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns cached tier data for the player, or {@link Optional#empty()} if absent / expired.
     * When the entry is stale, a background refresh is automatically queued.
     *
     * @param username Minecraft username (case-insensitive lookup)
     */
    public static Optional<PlayerTierData> get(String username) {
        if (username == null || username.isBlank()) return Optional.empty();

        CacheEntry entry = CACHE.get(username.toLowerCase());
        if (entry == null) return Optional.empty();

        long ttlMs = (long) ModConfig.get().getRefreshIntervalSeconds() * 1000L;
        if (entry.isExpired(ttlMs)) {
            // Stale — re-fetch in background, return stale data for now (no blank frame)
            TierApiManager.fetchAsync(username);
        }

        return Optional.of(entry.data());
    }

    /**
     * Stores tier data for the given player. Called by {@link TierApiManager} on success.
     */
    public static void cache(String username, PlayerTierData data) {
        if (username == null || username.isBlank()) return;
        CACHE.put(username.toLowerCase(), new CacheEntry(data, System.currentTimeMillis()));
    }

    /**
     * Stores a "no tier" sentinel so we don't hammer the API for unknown players.
     */
    public static void cacheNoTier(String username) {
        cache(username, PlayerTierData.noTier(username));
    }

    /**
     * Returns {@code true} if a non-expired entry exists for this player.
     */
    public static boolean isCached(String username) {
        if (username == null || username.isBlank()) return false;
        CacheEntry entry = CACHE.get(username.toLowerCase());
        if (entry == null) return false;
        long ttlMs = (long) ModConfig.get().getRefreshIntervalSeconds() * 1000L;
        return !entry.isExpired(ttlMs);
    }

    /**
     * Clears the entire cache (useful when switching servers or toggling the mod off/on).
     */
    public static void clearAll() {
        CACHE.clear();
    }

    /**
     * Returns the current number of cached entries.
     */
    public static int size() {
        return CACHE.size();
    }
}

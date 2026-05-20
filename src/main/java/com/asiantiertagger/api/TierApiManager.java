package com.asiantiertagger.api;

import com.asiantiertagger.AsianTierTaggerClient;
import com.asiantiertagger.cache.PlayerCacheManager;
import com.asiantiertagger.config.ModConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TierApiManager — handles all HTTP communication with the tier API.
 *
 * <p>Features:
 * <ul>
 *   <li>Non-blocking async requests (virtual-thread-backed executor)</li>
 *   <li>Retry logic with exponential back-off (up to MAX_RETRIES attempts)</li>
 *   <li>Per-player in-flight deduplication (no duplicate requests)</li>
 *   <li>Graceful fallback when the API is offline</li>
 * </ul>
 */
public class TierApiManager {

    private static final Gson GSON = new Gson();
    private static final int MAX_RETRIES = 3;
    private static final long TIMEOUT_SECONDS = 5;

    /** Tracks usernames that are currently being fetched to avoid duplicate requests. */
    private static final Map<String, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();

    /** Single shared HttpClient for all requests. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    /** Dedicated thread pool so API work never blocks the game thread. */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "AsianTierTagger-API");
        t.setDaemon(true);
        return t;
    });

    /**
     * Starts an async fetch for the given username.
     * Skips if a request is already in-flight for this player.
     *
     * @param username Minecraft username to look up
     */
    public static void fetchAsync(String username) {
        if (username == null || username.isBlank()) return;
        if (IN_FLIGHT.putIfAbsent(username, Boolean.TRUE) != null) return; // already in-flight

        EXECUTOR.submit(() -> {
            try {
                fetchWithRetry(username, 0);
            } finally {
                IN_FLIGHT.remove(username);
            }
        });
    }

    /**
     * Performs the HTTP GET request, retrying on failure with exponential back-off.
     *
     * @param username  player name
     * @param attempt   current attempt number (0-indexed)
     */
    private static void fetchWithRetry(String username, int attempt) {
        String baseUrl = ModConfig.get().getApiUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            AsianTierTaggerClient.LOGGER.warn("[AsianTierTagger] API URL is not set. Skipping fetch for {}", username);
            return;
        }

        String url = baseUrl.replaceAll("/$", "") + "/player/" + username;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .header("User-Agent", "AsianTierTagger/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                parseAndCache(username, response.body());
            } else if (response.statusCode() == 404) {
                // Player not found — cache a "no tier" sentinel so we don't hammer the API
                PlayerCacheManager.cacheNoTier(username);
            } else {
                AsianTierTaggerClient.LOGGER.warn(
                        "[AsianTierTagger] API returned {} for {}. Attempt {}/{}",
                        response.statusCode(), username, attempt + 1, MAX_RETRIES);
                retry(username, attempt);
            }

        } catch (Exception e) {
            AsianTierTaggerClient.LOGGER.warn(
                    "[AsianTierTagger] Request failed for {} (attempt {}/{}): {}",
                    username, attempt + 1, MAX_RETRIES, e.getMessage());
            retry(username, attempt);
        }
    }

    /**
     * Schedules a retry with exponential back-off unless we've exhausted attempts.
     */
    private static void retry(String username, int attempt) {
        if (attempt + 1 >= MAX_RETRIES) {
            AsianTierTaggerClient.LOGGER.warn("[AsianTierTagger] Giving up on {} after {} attempts.", username, MAX_RETRIES);
            PlayerCacheManager.cacheNoTier(username); // fallback sentinel
            return;
        }
        long delayMs = (long) Math.pow(2, attempt) * 1000L; // 1s, 2s, 4s
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        fetchWithRetry(username, attempt + 1);
    }

    /**
     * Parses the JSON response body and stores the result in the player cache.
     *
     * <p>Expected JSON shape:
     * <pre>
     * {
     *   "player":   "VoidFury",
     *   "tier":     "HT2",
     *   "gamemode": "NethPot",
     *   "color":    "#FF5555",
     *   "prefix":   "[HT2]"
     * }
     * </pre>
     */
    private static void parseAndCache(String username, String body) {
        try {
            JsonObject obj = GSON.fromJson(body, JsonObject.class);

            String player   = getOrDefault(obj, "player",   username);
            String tier     = getOrDefault(obj, "tier",     "UNRANKED");
            String gamemode = getOrDefault(obj, "gamemode", "Unknown");
            String color    = getOrDefault(obj, "color",    "#AAAAAA");
            String prefix   = getOrDefault(obj, "prefix",  "[?]");

            PlayerCacheManager.cache(player, new PlayerTierData(player, tier, gamemode, color, prefix));
        } catch (Exception e) {
            AsianTierTaggerClient.LOGGER.error("[AsianTierTagger] Failed to parse response for {}: {}", username, e.getMessage());
            PlayerCacheManager.cacheNoTier(username);
        }
    }

    private static String getOrDefault(JsonObject obj, String key, String def) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : def;
    }
}

package com.asiantiertagger.render;

import com.asiantiertagger.api.PlayerTierData;
import com.asiantiertagger.api.TierApiManager;
import com.asiantiertagger.cache.PlayerCacheManager;
import com.asiantiertagger.config.ModConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TierRenderSystem — central helper used by all mixins to produce tier-prefixed text.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Resolve a username → cached {@link PlayerTierData} (or trigger async fetch)</li>
 *   <li>Build colored {@link Text} objects for above-head / tab / chat contexts</li>
 *   <li>Map tier strings and hex colors to Minecraft {@link Formatting}</li>
 * </ul>
 */
public class TierRenderSystem {

    // ── Tier → Minecraft Formatting map ────────────────────────────────────────

    private static final Map<String, Formatting> TIER_COLORS = new HashMap<>();

    static {
        // Low Tiers
        TIER_COLORS.put("LT5", Formatting.GRAY);
        TIER_COLORS.put("LT4", Formatting.DARK_GRAY);
        TIER_COLORS.put("LT3", Formatting.GREEN);
        TIER_COLORS.put("LT2", Formatting.AQUA);
        TIER_COLORS.put("LT1", Formatting.BLUE);
        // High Tiers
        TIER_COLORS.put("HT5", Formatting.YELLOW);
        TIER_COLORS.put("HT4", Formatting.GOLD);
        TIER_COLORS.put("HT3", Formatting.RED);
        TIER_COLORS.put("HT2", Formatting.DARK_RED);
        TIER_COLORS.put("HT1", Formatting.DARK_PURPLE);
        // Fallback
        TIER_COLORS.put("UNRANKED", Formatting.DARK_GRAY);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns a {@link Text} component with the tier prefix prepended to the username.
     * E.g.: {@code [HT2] VoidFury} with appropriate color.
     *
     * <p>If no cached data exists, an async fetch is triggered and the plain username is returned.
     *
     * @param username       Minecraft username
     * @param isLocalPlayer  true if this is the local player (respects hide-own-tag setting)
     */
    public static Text buildPrefixedName(String username, boolean isLocalPlayer) {
        ModConfig cfg = ModConfig.get();

        if (!cfg.isEnabled()) {
            return Text.literal(username);
        }
        if (isLocalPlayer && cfg.isHideOwnTag()) {
            return Text.literal(username);
        }

        Optional<PlayerTierData> opt = PlayerCacheManager.get(username);
        if (opt.isEmpty()) {
            // Not cached yet — fire off a background fetch and return the plain name for now
            TierApiManager.fetchAsync(username);
            return Text.literal(username);
        }

        PlayerTierData data = opt.get();

        // Gamemode filter
        String filter = cfg.getGamemodeFilter();
        if (!filter.isBlank() && !data.gamemode().equalsIgnoreCase(filter)) {
            return Text.literal(username);
        }

        if (!data.hasPrefix()) {
            return Text.literal(username);
        }

        return buildColoredText(data, username);
    }

    /**
     * Builds {@code [PREFIX] username} as a styled {@link MutableText}.
     */
    private static MutableText buildColoredText(PlayerTierData data, String username) {
        Formatting color = TIER_COLORS.getOrDefault(data.tier().toUpperCase(), Formatting.GRAY);

        // Try to use the hex color from the API if available and valid
        MutableText prefixText = Text.literal(data.prefix() + " ");
        prefixText.setStyle(Style.EMPTY.withColor(color));

        MutableText nameText = Text.literal(username);
        nameText.setStyle(Style.EMPTY.withFormatting(Formatting.WHITE));

        return prefixText.append(nameText);
    }

    /**
     * Returns just the colored prefix text (e.g. "[HT2] "), useful for tab list injection.
     */
    public static Text buildPrefixOnly(String username) {
        Optional<PlayerTierData> opt = PlayerCacheManager.get(username);
        if (opt.isEmpty() || !opt.get().hasPrefix()) {
            return Text.empty();
        }
        PlayerTierData data = opt.get();
        Formatting color = TIER_COLORS.getOrDefault(data.tier().toUpperCase(), Formatting.GRAY);
        MutableText t = Text.literal(data.prefix() + " ");
        t.setStyle(Style.EMPTY.withColor(color));
        return t;
    }

    /**
     * Converts a hex color string (e.g. {@code "#FF5555"}) to the nearest
     * {@link Formatting} value for fallback contexts that don't support true color.
     */
    public static Formatting hexToNearestFormatting(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() < 7) return Formatting.WHITE;
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            // Very rough nearest-color mapping
            if (r > 200 && g < 100 && b < 100) return Formatting.RED;
            if (r > 150 && g > 150 && b < 100) return Formatting.YELLOW;
            if (r < 100 && g > 150 && b < 100) return Formatting.GREEN;
            if (r < 100 && g > 150 && b > 150) return Formatting.AQUA;
            if (r < 100 && g < 100 && b > 150) return Formatting.BLUE;
            if (r > 100 && g < 100 && b > 100) return Formatting.DARK_PURPLE;
            if (r > 150 && g < 100 && b < 50)  return Formatting.DARK_RED;
            if (r > 150 && g > 100 && b < 50)  return Formatting.GOLD;
            if (r > 150 && g > 150 && b > 150) return Formatting.WHITE;
            if (r < 100 && g < 100 && b < 100) return Formatting.DARK_GRAY;
            return Formatting.GRAY;
        } catch (NumberFormatException e) {
            return Formatting.WHITE;
        }
    }
}

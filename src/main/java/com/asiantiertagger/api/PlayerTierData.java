package com.asiantiertagger.api;

/**
 * Immutable data record holding one player's tier information as returned by the API.
 */
public record PlayerTierData(
        String player,
        String tier,
        String gamemode,
        String color,
        String prefix
) {
    /**
     * Sentinel value used when the API returns no tier or is unreachable.
     */
    public static PlayerTierData noTier(String username) {
        return new PlayerTierData(username, "UNRANKED", "Unknown", "#AAAAAA", "");
    }

    /**
     * @return true if this player has a real (non-empty) tier prefix to display.
     */
    public boolean hasPrefix() {
        return prefix != null && !prefix.isBlank();
    }
}

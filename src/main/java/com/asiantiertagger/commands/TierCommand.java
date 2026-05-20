package com.asiantiertagger.commands;

import com.asiantiertagger.api.TierApiManager;
import com.asiantiertagger.cache.PlayerCacheManager;
import com.asiantiertagger.config.ConfigScreen;
import com.asiantiertagger.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * TierCommand — registers all {@code /asiantiers} sub-commands.
 *
 * <pre>
 * /asiantiers toggle          — enable / disable the mod
 * /asiantiers config          — open the in-game GUI
 * /asiantiers refresh         — force-clear cache and re-fetch all visible players
 * /asiantiers lookup <name>   — look up a specific player's tier
 * /asiantiers seturl <url>    — set the API base URL at runtime
 * /asiantiers interval <sec>  — set the auto-refresh interval
 * /asiantiers status          — print current settings to chat
 * </pre>
 */
public class TierCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("asiantiers")

                // /asiantiers toggle
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> {
                            ModConfig cfg = ModConfig.get();
                            cfg.setEnabled(!cfg.isEnabled());
                            ModConfig.save();
                            String state = cfg.isEnabled() ? "§aenabled" : "§cdisabled";
                            ctx.getSource().sendFeedback(Text.literal("[AsianTierTagger] Mod " + state + "§r."));
                            return 1;
                        }))

                // /asiantiers config — open GUI
                .then(ClientCommandManager.literal("config")
                        .executes(ctx -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.execute(() ->
                                    client.setScreen(new ConfigScreen(client.currentScreen)));
                            return 1;
                        }))

                // /asiantiers refresh — clear cache + re-fetch all visible
                .then(ClientCommandManager.literal("refresh")
                        .executes(ctx -> {
                            PlayerCacheManager.clearAll();
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.getNetworkHandler() != null) {
                                client.getNetworkHandler().getPlayerList().forEach(entry -> {
                                    String name = entry.getProfile().getName();
                                    if (name != null && !name.isBlank()) {
                                        TierApiManager.fetchAsync(name);
                                    }
                                });
                            }
                            ctx.getSource().sendFeedback(Text.literal(
                                    "[AsianTierTagger] §aCache cleared. Refreshing all visible players..."));
                            return 1;
                        }))

                // /asiantiers lookup <username>
                .then(ClientCommandManager.literal("lookup")
                        .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "username");
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "[AsianTierTagger] §7Fetching tier for §f" + name + "§7..."));
                                    PlayerCacheManager.clearAll(); // evict stale entry for this player
                                    TierApiManager.fetchAsync(name);
                                    // Result will appear above their head / tab once the response arrives
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "[AsianTierTagger] §7Request sent. Tag will appear shortly."));
                                    return 1;
                                })))

                // /asiantiers seturl <url>
                .then(ClientCommandManager.literal("seturl")
                        .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String url = StringArgumentType.getString(ctx, "url").trim();
                                    ModConfig.get().setApiUrl(url);
                                    ModConfig.save();
                                    PlayerCacheManager.clearAll();
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "[AsianTierTagger] §aAPI URL set to: §f" + url));
                                    return 1;
                                })))

                // /asiantiers interval <seconds>
                .then(ClientCommandManager.literal("interval")
                        .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(5, 3600))
                                .executes(ctx -> {
                                    int secs = IntegerArgumentType.getInteger(ctx, "seconds");
                                    ModConfig.get().setRefreshIntervalSeconds(secs);
                                    ModConfig.save();
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "[AsianTierTagger] §aRefresh interval set to §f" + secs + "s§a."));
                                    return 1;
                                })))

                // /asiantiers status
                .then(ClientCommandManager.literal("status")
                        .executes(ctx -> {
                            ModConfig cfg = ModConfig.get();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§6[AsianTierTagger] §7Status:\n" +
                                    "  Enabled: "          + (cfg.isEnabled() ? "§aYes" : "§cNo") + "\n§7" +
                                    "  API URL: §f"        + cfg.getApiUrl() + "§7\n" +
                                    "  Refresh: §f"        + cfg.getRefreshIntervalSeconds() + "s§7\n" +
                                    "  Above Head: "       + boolStr(cfg.isShowAboveHead()) + "§7\n" +
                                    "  Tab List: "         + boolStr(cfg.isShowInTab()) + "§7\n" +
                                    "  Chat: "             + boolStr(cfg.isShowInChat()) + "§7\n" +
                                    "  Hide Own Tag: "     + boolStr(cfg.isHideOwnTag()) + "§7\n" +
                                    "  Cached players: §f" + PlayerCacheManager.size()));
                            return 1;
                        }))
        );
    }

    private static String boolStr(boolean v) {
        return v ? "§aYes" : "§cNo";
    }
}

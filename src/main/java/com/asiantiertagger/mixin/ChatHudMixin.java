package com.asiantiertagger.mixin;

import com.asiantiertagger.config.ModConfig;
import com.asiantiertagger.render.TierRenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChatHudMixin — intercepts incoming chat messages and prepends the sender's tier tag.
 *
 * <p>Strategy: When a chat message arrives ({@code addMessage}), we extract the sender's
 * name from the message text, look up their tier, and rebuild the message with a colored
 * prefix inserted before the username portion.
 *
 * <p>Supported vanilla chat format: {@code <Username> message text}
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    /**
     * Matches the vanilla chat format {@code <PlayerName> ...}.
     * Group 1 captures the player name.
     */
    private static final Pattern CHAT_PATTERN = Pattern.compile("^<([A-Za-z0-9_]{1,16})>");

    /**
     * Modify the message {@link Text} before it is added to the chat HUD.
     * If the mod is disabled or showInChat is off, the original message passes through unchanged.
     */
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text onAddMessage(Text message) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.isEnabled() || !cfg.isShowInChat()) return message;

        // Get the raw string representation of the message for regex matching
        String raw = message.getString();
        Matcher m = CHAT_PATTERN.matcher(raw);
        if (!m.find()) return message; // not a standard player chat message

        String username = m.group(1);

        // Check if this is our own message + hide-own-tag is on
        MinecraftClient client = MinecraftClient.getInstance();
        if (cfg.isHideOwnTag() && client.player != null &&
                client.player.getGameProfile().getName().equalsIgnoreCase(username)) {
            return message;
        }

        // Get the prefix text (returns empty if no tier data cached)
        Text prefix = TierRenderSystem.buildPrefixOnly(username);
        if (prefix.getString().isBlank()) return message; // no tier yet

        // Rebuild: <prefix><original message>
        MutableText result = prefix.copy().append(message);
        return result;
    }
}

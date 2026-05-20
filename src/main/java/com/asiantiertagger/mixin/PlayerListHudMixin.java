package com.asiantiertagger.mixin;

import com.asiantiertagger.config.ModConfig;
import com.asiantiertagger.render.TierRenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerListHudMixin — injects tier tags into the player tab list (F3+TAB / TAB overlay).
 *
 * <p>Intercepts {@code getPlayerName} so every name in the tab list gets a tier prefix.
 */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    /**
     * Fired when the tab list resolves the display name for a {@link PlayerListEntry}.
     * We prepend the tier prefix before the vanilla name is returned to the renderer.
     */
    @Inject(
            method = "getPlayerName",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetPlayerName(PlayerListEntry entry,
                                 CallbackInfoReturnable<Text> cir) {

        ModConfig cfg = ModConfig.get();
        if (!cfg.isEnabled() || !cfg.isShowInTab()) return;

        String username = entry.getProfile().getName();
        if (username == null || username.isBlank()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        boolean isLocal = client.player != null &&
                client.player.getUuid().equals(entry.getProfile().getId());

        if (isLocal && cfg.isHideOwnTag()) return;

        Text prefix = TierRenderSystem.buildPrefixOnly(username);
        if (prefix.equals(Text.empty())) return; // no tier data yet — keep vanilla name

        // Build: <colored prefix> <original display name>
        MutableText result = prefix.copy().append(cir.getReturnValue());
        cir.setReturnValue(result);
    }
}

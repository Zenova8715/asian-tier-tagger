package com.asiantiertagger.mixin;

import com.asiantiertagger.config.ModConfig;
import com.asiantiertagger.render.TierRenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerEntityRendererMixin — injects tier tags into the name label rendered above player heads.
 *
 * <p>Intercepts {@code renderLabelIfPresent} so we can replace the displayed name with a
 * tier-prefixed version (e.g. "[HT2] VoidFury") without touching any vanilla rendering code.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx,
                                     PlayerEntityModel<AbstractClientPlayerEntity> model,
                                     float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    /**
     * Called just before the nametag label is drawn. We swap in the tier-prefixed name.
     */
    @Inject(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderLabel(AbstractClientPlayerEntity player,
                               Text text,
                               MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers,
                               int light,
                               float tickDelta,
                               CallbackInfo ci) {

        ModConfig cfg = ModConfig.get();
        if (!cfg.isEnabled() || !cfg.isShowAboveHead()) return;

        String username = player.getGameProfile().getName();
        if (username == null || username.isBlank()) return;

        // Determine whether this is the local player
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isLocal = client.player != null &&
                client.player.getUuid().equals(player.getUuid());

        if (isLocal && cfg.isHideOwnTag()) return;

        Text prefixedName = TierRenderSystem.buildPrefixedName(username, isLocal);

        // Cancel the original call and re-invoke with our prefixed text
        ci.cancel();
        super.renderLabelIfPresent(player, prefixedName, matrices, vertexConsumers, light, tickDelta);
    }
}

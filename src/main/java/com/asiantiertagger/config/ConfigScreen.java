package com.asiantiertagger.config;

import com.asiantiertagger.cache.PlayerCacheManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * ConfigScreen — an in-game GUI for editing AsianTierTagger settings.
 *
 * <p>Access via: {@code /asiantiers config}
 *
 * <p>Controls:
 * <ul>
 *   <li>Toggle mod on/off</li>
 *   <li>Toggle above-head display</li>
 *   <li>Toggle tab display</li>
 *   <li>Toggle chat display</li>
 *   <li>Toggle hide-own-tag</li>
 *   <li>Set API URL</li>
 *   <li>Set refresh interval</li>
 *   <li>Force refresh (clear cache)</li>
 * </ul>
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    private TextFieldWidget apiUrlField;
    private TextFieldWidget refreshField;

    public ConfigScreen(Screen parent) {
        super(Text.literal("AsianTierTagger Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY  = 40;
        int spacing = 24;
        int btnW    = 200;
        int btnH    = 20;

        ModConfig cfg = ModConfig.get();

        // ── Enabled toggle ────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        toggleText("Mod Enabled", cfg.isEnabled()),
                        btn -> {
                            cfg.setEnabled(!cfg.isEnabled());
                            btn.setMessage(toggleText("Mod Enabled", cfg.isEnabled()));
                            ModConfig.save();
                        })
                .dimensions(centerX - btnW / 2, startY, btnW, btnH)
                .build());

        // ── Show above head ───────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        toggleText("Show Above Head", cfg.isShowAboveHead()),
                        btn -> {
                            cfg.setShowAboveHead(!cfg.isShowAboveHead());
                            btn.setMessage(toggleText("Show Above Head", cfg.isShowAboveHead()));
                            ModConfig.save();
                        })
                .dimensions(centerX - btnW / 2, startY + spacing, btnW, btnH)
                .build());

        // ── Show in tab ───────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        toggleText("Show In Tab", cfg.isShowInTab()),
                        btn -> {
                            cfg.setShowInTab(!cfg.isShowInTab());
                            btn.setMessage(toggleText("Show In Tab", cfg.isShowInTab()));
                            ModConfig.save();
                        })
                .dimensions(centerX - btnW / 2, startY + spacing * 2, btnW, btnH)
                .build());

        // ── Show in chat ──────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        toggleText("Show In Chat", cfg.isShowInChat()),
                        btn -> {
                            cfg.setShowInChat(!cfg.isShowInChat());
                            btn.setMessage(toggleText("Show In Chat", cfg.isShowInChat()));
                            ModConfig.save();
                        })
                .dimensions(centerX - btnW / 2, startY + spacing * 3, btnW, btnH)
                .build());

        // ── Hide own tag ──────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        toggleText("Hide Own Tag", cfg.isHideOwnTag()),
                        btn -> {
                            cfg.setHideOwnTag(!cfg.isHideOwnTag());
                            btn.setMessage(toggleText("Hide Own Tag", cfg.isHideOwnTag()));
                            ModConfig.save();
                        })
                .dimensions(centerX - btnW / 2, startY + spacing * 4, btnW, btnH)
                .build());

        // ── API URL text field ────────────────────────────────────────────────
        apiUrlField = new TextFieldWidget(
                this.textRenderer,
                centerX - btnW / 2, startY + spacing * 5 + 12,
                btnW, btnH,
                Text.literal("API URL"));
        apiUrlField.setMaxLength(256);
        apiUrlField.setText(cfg.getApiUrl());
        addDrawableChild(apiUrlField);

        // ── Refresh interval text field ────────────────────────────────────────
        refreshField = new TextFieldWidget(
                this.textRenderer,
                centerX - btnW / 2, startY + spacing * 6 + 18,
                60, btnH,
                Text.literal("Refresh (s)"));
        refreshField.setMaxLength(4);
        refreshField.setText(String.valueOf(cfg.getRefreshIntervalSeconds()));
        addDrawableChild(refreshField);

        // ── Save settings ─────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Save Settings"),
                        btn -> saveAndClose())
                .dimensions(centerX - btnW / 2, startY + spacing * 7 + 22, btnW, btnH)
                .build());

        // ── Force refresh (clear cache) ────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Force Refresh Cache"),
                        btn -> {
                            PlayerCacheManager.clearAll();
                            btn.setMessage(Text.literal("Cache Cleared!"));
                        })
                .dimensions(centerX - btnW / 2, startY + spacing * 8 + 22, btnW, btnH)
                .build());

        // ── Close ─────────────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Close"),
                        btn -> close())
                .dimensions(centerX - 50, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title, this.width / 2, 14, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY  = 40;
        int spacing = 24;

        // Labels for text fields
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("API URL:"),
                centerX - 100, startY + spacing * 5 + 2, 0xAAAAAA);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Refresh Interval (seconds):"),
                centerX - 100, startY + spacing * 6 + 8, 0xAAAAAA);
    }

    private void saveAndClose() {
        ModConfig cfg = ModConfig.get();
        cfg.setApiUrl(apiUrlField.getText().trim());
        try {
            cfg.setRefreshIntervalSeconds(Integer.parseInt(refreshField.getText().trim()));
        } catch (NumberFormatException ignored) { /* keep old value */ }
        ModConfig.save();
        close();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Text toggleText(String label, boolean state) {
        String stateStr = state ? "§aON" : "§cOFF";
        return Text.literal(label + ": " + stateStr);
    }
}

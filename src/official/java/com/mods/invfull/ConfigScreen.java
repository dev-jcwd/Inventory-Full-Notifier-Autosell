package com.mods.invfull;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    private boolean compact = false;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Inventory Full Notifier Config"));
        this.parent = parent;
    }

    private static Component onOff(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    /** Slider over 1..36 slots. Only changes the config in memory; it is written once when the screen closes. */
    private abstract static class SlotSlider extends AbstractSliderButton {
        private final String label;

        SlotSlider(int x, int y, int width, int height, String label, int slots) {
            super(x, y, width, height, Component.literal(label + ": " + slots + " slots"), slots / (double) IFNConfig.SLOTS);
            this.label = label;
        }

        protected int slots() {
            return Math.max(1, (int) Math.ceil(this.value * IFNConfig.SLOTS));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(label + ": " + slots() + " slots"));
        }
    }

    @Override
    protected void init() {
        super.init();

        IFNConfig config = ConfigManager.getConfig();
        int centerX = this.width / 2;
        int buttonWidth = 200;
        // Short windows (or a big GUI scale) cannot fit one tall column: put the keybind info in a second column.
        compact = this.height < 330;
        int left = compact ? centerX - buttonWidth - 5 : centerX - buttonWidth / 2;
        int right = centerX + 5;
        int top = compact ? 42 : 50;
        int step = compact ? 22 : 25;

        // Auto Sell Toggle
        this.addRenderableWidget(Button.builder(
            onOff("Auto Sell", config.autosellEnabled),
            button -> {
                config.autosellEnabled = !config.autosellEnabled;
                button.setMessage(onOff("Auto Sell", config.autosellEnabled));
            })
            .bounds(left, top, buttonWidth, 20)
            .build());

        // Auto Sell Mode: all -> handall -> custom -> all
        this.addRenderableWidget(Button.builder(
            Component.literal("Auto Sell Mode: " + config.autosellMode),
            button -> {
                switch (config.autosellMode) {
                    case "all": config.autosellMode = "handall"; break;
                    case "handall": config.autosellMode = "custom"; break;
                    default: config.autosellMode = "all"; break;
                }
                button.setMessage(Component.literal("Auto Sell Mode: " + config.autosellMode));
            })
            .bounds(left, top + step, buttonWidth, 20)
            .build());

        // Custom Command Field
        EditBox customCommandField = new EditBox(this.font, left, top + 2 * step, buttonWidth, 20, Component.literal("Custom command"));
        customCommandField.setMaxLength(100);
        customCommandField.setValue(config.customCommand);
        customCommandField.setResponder(text -> config.customCommand = text);
        this.addRenderableWidget(customCommandField);

        // Auto Sell Threshold Slider
        this.addRenderableWidget(new SlotSlider(left, top + 3 * step, buttonWidth, 20, "Threshold", config.autosellThreshold) {
            @Override
            protected void applyValue() {
                config.autosellThreshold = slots();
            }
        });

        // Notifications Toggle
        this.addRenderableWidget(Button.builder(
            onOff("Notifications", config.notificationsEnabled),
            button -> {
                config.notificationsEnabled = !config.notificationsEnabled;
                button.setMessage(onOff("Notifications", config.notificationsEnabled));
            })
            .bounds(left, top + 4 * step, buttonWidth, 20)
            .build());

        // Milestones Toggle
        this.addRenderableWidget(Button.builder(
            onOff("Milestones", config.milestonesEnabled),
            button -> {
                config.milestonesEnabled = !config.milestonesEnabled;
                button.setMessage(onOff("Milestones", config.milestonesEnabled));
            })
            .bounds(left, top + 5 * step, buttonWidth, 20)
            .build());

        // Milestone Threshold Slider
        this.addRenderableWidget(new SlotSlider(left, top + 6 * step, buttonWidth, 20, "Milestone Alert", config.milestoneThreshold) {
            @Override
            protected void applyValue() {
                config.milestoneThreshold = slots();
            }
        });

        // Keybind information (display only)
        this.addRenderableWidget(Button.builder(
            Component.literal("Config Keybind: " + KeybindManager.getConfigKeybindDisplayName()),
            button -> {})
            .bounds(compact ? right : left, compact ? 66 : 236, buttonWidth, 20)
            .build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Mode Switch Keybind: " + KeybindManager.getAutosellSwitchKeybindDisplayName()),
            button -> {})
            .bounds(compact ? right : left, compact ? 90 : 261, buttonWidth, 20)
            .build());

        // Done Button
        this.addRenderableWidget(Button.builder(
            Component.literal("Done"),
            button -> this.onClose())
            .bounds(centerX - buttonWidth / 2, this.height - (compact ? 26 : 34), buttonWidth, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Own background instead of the blurred default, so the blur is not applied several times
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Buttons and the other widgets
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        Component titleText = Component.literal("IFN + AUTOSELL")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        graphics.centeredText(this.font, titleText, this.width / 2, 20, 0xFFFFFFFF);

        // Separator line below the title
        graphics.fill(this.width / 2 - 80, 35, this.width / 2 + 80, 36, 0xFFFFD700);

        // Keybind section header
        graphics.centeredText(this.font,
                Component.literal("Keybinds (Change in Controls menu)").withStyle(ChatFormatting.YELLOW),
                compact ? this.width / 2 + 5 + 100 : this.width / 2, compact ? 50 : 223, 0xFFFFFF55);

        // Footer text (no room for it on a short screen)
        if (!compact) graphics.centeredText(this.font,
                Component.literal("Client-side mod - Works on all servers")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                this.width / 2, this.height - 12, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        Compat.openScreen(this.minecraft, this.parent);
    }

    /** Runs however the screen is left (Done, Escape, another screen): write the settings once. */
    @Override
    public void removed() {
        ConfigManager.getConfig().sanitize();
        ConfigManager.save();
    }
}

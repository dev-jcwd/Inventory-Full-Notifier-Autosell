package com.mods.invfull;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget customCommandField;
    private SliderWidget thresholdSlider;
    private ButtonWidget autosellToggle;
    private ButtonWidget notificationsToggle;
    
    public ConfigScreen(Screen parent) {
        super(Text.literal("Inventory Full Notifier Config"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        IFNConfig config = ConfigManager.getConfig();
        int centerX = this.width / 2;
        int buttonWidth = 200;
        
        // Auto Sell Toggle
        this.autosellToggle = this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Auto Sell: " + (config.autosellEnabled ? "ON" : "OFF")),
            button -> {
                config.autosellEnabled = !config.autosellEnabled;
                button.setMessage(Text.literal("Auto Sell: " + (config.autosellEnabled ? "ON" : "OFF")));
                ConfigManager.save();
            })
            .dimensions(centerX - buttonWidth/2, 50, buttonWidth, 20)
            .build());
        
        // Auto Sell Mode
        this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Auto Sell Mode: " + config.autosellMode),
            button -> {
                if (config.autosellMode.equals("all")) {
                    config.autosellMode = "handall";
                } else if (config.autosellMode.equals("handall")) {
                    config.autosellMode = "custom";
                } else if (config.autosellMode.equals("custom")) {
                    config.autosellMode = "all";
                } else {
                    // Default fallback
                    config.autosellMode = "all";
                }
                button.setMessage(Text.literal("Auto Sell Mode: " + config.autosellMode));
                ConfigManager.save();
            })
            .dimensions(centerX - buttonWidth/2, 75, buttonWidth, 20)
            .build());
        
        // Custom Command Field
        this.customCommandField = new TextFieldWidget(this.textRenderer, centerX - buttonWidth/2, 100, buttonWidth, 20, Text.literal(""));
        this.customCommandField.setMaxLength(100);
        this.customCommandField.setText(config.customCommand);
        this.customCommandField.setChangedListener(text -> {
            config.customCommand = text;
            ConfigManager.save();
        });
        this.addDrawableChild(this.customCommandField);
        
        // Auto Sell Threshold Slider
        this.thresholdSlider = this.addDrawableChild(new SliderWidget(
            centerX - buttonWidth/2, 125, buttonWidth, 20, 
            Text.literal("Threshold: " + config.autosellThreshold + " slots"), 
            config.autosellThreshold / 36.0) {
            
            @Override
            protected void updateMessage() {
                int value = (int) Math.ceil(this.value * 36);
                if (value < 1) value = 1;
                this.setMessage(Text.literal("Threshold: " + value + " slots"));
            }
            
            @Override
            protected void applyValue() {
                int value = (int) Math.ceil(this.value * 36);
                if (value < 1) value = 1;
                config.autosellThreshold = value;
                ConfigManager.save();
            }
        });
        
        // Notifications Toggle
        this.notificationsToggle = this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Notifications: " + (config.notificationsEnabled ? "ON" : "OFF")),
            button -> {
                config.notificationsEnabled = !config.notificationsEnabled;
                button.setMessage(Text.literal("Notifications: " + (config.notificationsEnabled ? "ON" : "OFF")));
                ConfigManager.save();
            })
            .dimensions(centerX - buttonWidth/2, 150, buttonWidth, 20)
            .build());
        
        // Milestones Toggle
        this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Milestones: " + (config.milestonesEnabled ? "ON" : "OFF")),
            button -> {
                config.milestonesEnabled = !config.milestonesEnabled;
                button.setMessage(Text.literal("Milestones: " + (config.milestonesEnabled ? "ON" : "OFF")));
                ConfigManager.save();
            })
            .dimensions(centerX - buttonWidth/2, 175, buttonWidth, 20)
            .build());
            
        // Milestone Threshold Slider
        this.addDrawableChild(new SliderWidget(
            centerX - buttonWidth/2, 200, buttonWidth, 20, 
            Text.literal("Milestone Alert: " + config.milestoneThreshold + " slots"), 
            config.milestoneThreshold / 36.0) {
            
            @Override
            protected void updateMessage() {
                int value = (int) Math.ceil(this.value * 36);
                if (value < 1) value = 1;
                this.setMessage(Text.literal("Milestone Alert: " + value + " slots"));
            }
            
            @Override
            protected void applyValue() {
                int value = (int) Math.ceil(this.value * 36);
                if (value < 1) value = 1;
                config.milestoneThreshold = value;
                ConfigManager.save();
            }
        });
        
        // Keybind Information Section
        this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Config Keybind: " + KeybindManager.getConfigKeybindDisplayName()),
            button -> {
                // This button is just for display, no action needed
            })
            .dimensions(centerX - buttonWidth/2, 230, buttonWidth, 20)
            .build());
            
        this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Mode Switch Keybind: " + KeybindManager.getAutosellSwitchKeybindDisplayName()),
            button -> {
                // This button is just for display, no action needed
            })
            .dimensions(centerX - buttonWidth/2, 255, buttonWidth, 20)
            .build());
        
        // Done Button
        this.addDrawableChild(new ButtonWidget.Builder(
            Text.literal("Done"),
            button -> this.close())
            .dimensions(centerX - buttonWidth/2, this.height - 30, buttonWidth, 20)
            .build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use fillGradient instead of renderBackground to avoid multiple blur effects
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        // Render buttons and default elements first
        super.render(context, mouseX, mouseY, delta);

        // Now draw your custom title on top
        Text titleText = Text.literal("IFN + AUTOSELL")
                .formatted(net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, titleText, this.width / 2, 20, 0xFFFFFF);

        // Separator line below title
        context.fill(this.width / 2 - 80, 35, this.width / 2 + 80, 36, 0xFFD700);

        // Keybind section header
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Keybinds (Change in Controls menu)")
                        .formatted(net.minecraft.util.Formatting.YELLOW),
                this.width / 2, 215, 0xFFFF55);

        // Footer text
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Client-side mod - Works on all servers")
                        .formatted(net.minecraft.util.Formatting.GRAY, net.minecraft.util.Formatting.ITALIC),
                this.width / 2, this.height - 12, 0xAAAAAA);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }
}
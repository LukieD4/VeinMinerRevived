package com.lukiedoggo.veinminerrevived.client;

import com.lukiedoggo.veinminerrevived.config.ModConfig;
import com.lukiedoggo.veinminerrevived.util.CompatUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VeinMinerConfigScreen extends Screen {
    private final Screen parent;
    private EditBox maxBlocksBox;
    private EditBox blockDelayBox;
    private EditBox whitelistBox;
    private EditBox soundBox;

    public Button oresToggleButton;
    public Button sandToggleButton;
    public Button clayToggleButton;
    public Button logsToggleButton;
    public Button gravelToggleButton;
    public Button warningToggleButton;
    public Button verboseToggleButton;
    public Button tooltipToggleButton;
    public Button particlesToggleButton;
    public Button toggleSoundButton;
    public Button toggleWhitelistButton;
    public Button toggleShiftButton;

    // Local state variables
    public boolean allowOres;
    public boolean allowSand;
    public boolean allowClay;
    public boolean allowLogs;
    public boolean allowGravel;
    public boolean showFirstCrouchWarning;
    public boolean verboseDebug;
    public boolean showRequirementTooltips;
    public boolean enableParticles;
    public boolean enableSound;
    public boolean enableWhitelist;
    public boolean enableShiftActivation;

    private boolean isHost;

    public VeinMinerConfigScreen(Screen parent) {
        super(new TextComponent("VeinMiner Config"));
        this.parent = parent;
        CompatUtils.logInfo("Opening VeinMiner config screen");
        this.isHost = Minecraft.getInstance().hasSingleplayerServer();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.labels.clear();

        // CLIENT settings (local)
        allowOres = ModConfig.CLIENT.allowOres.get();
        allowSand = ModConfig.CLIENT.allowSand.get();
        allowClay = ModConfig.CLIENT.allowClay.get();
        allowLogs = ModConfig.CLIENT.allowLogs.get();
        allowGravel = ModConfig.CLIENT.allowGravel.get();
        showFirstCrouchWarning = ModConfig.CLIENT.showFirstCrouchWarning.get();
        verboseDebug = ModConfig.CLIENT.verboseDebug.get();
        showRequirementTooltips = ModConfig.CLIENT.showRequirementTooltips.get();
        enableWhitelist = ModConfig.CLIENT.enableWhitelist.get();

        // SERVER settings (host-controlled)
        enableParticles = ModConfig.SERVER.enableParticles.get();
        enableSound = ModConfig.SERVER.enableSound.get();
        enableShiftActivation = ModConfig.SERVER.enableShiftActivation.get();

        if (!isHost) {
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "[VeinMiner] The server/host can only control the settings for now, sorry</3");

            // SERVER settings with values
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "[VeinMiner] Server-controlled settings:");
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  enableParticles = " + ModConfig.SERVER.enableParticles.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  enableSound = " + ModConfig.SERVER.enableSound.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  enableShiftActivation = " + ModConfig.SERVER.enableShiftActivation.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  maxBlocks = " + ModConfig.SERVER.maxBlocks.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  blockDelay = " + ModConfig.SERVER.blockDelay.get());

            // CLIENT settings with values
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "[VeinMiner] Your client-controlled settings:");
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  allowOres = " + ModConfig.CLIENT.allowOres.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  allowSand = " + ModConfig.CLIENT.allowSand.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  allowClay = " + ModConfig.CLIENT.allowClay.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  allowLogs = " + ModConfig.CLIENT.allowLogs.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  allowGravel = " + ModConfig.CLIENT.allowGravel.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  showFirstCrouchWarning = " + ModConfig.CLIENT.showFirstCrouchWarning.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  verboseDebug = " + ModConfig.CLIENT.verboseDebug.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  showRequirementTooltips = " + ModConfig.CLIENT.showRequirementTooltips.get());
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                "  enableWhitelist = " + ModConfig.CLIENT.enableWhitelist.get());

            return;
        }



        int elementHeight = 22;
        int gap = 12;
        int labelX = this.width / 2 - 120;
        int fieldX = this.width / 2 + 20;
        int y = this.height / 6;

        addLabel("General Settings", this.width / 2, y - 20);

        // === Max Blocks ===
        addLabel("Max Blocks:", labelX, y + 6);
        maxBlocksBox = new EditBox(this.font, fieldX, y, 80, 20, new TextComponent("Max Blocks"));
        maxBlocksBox.setValue(String.valueOf(ModConfig.CLIENT.clientMaxBlocks.get()));
        maxBlocksBox.setMaxLength(3);
        addRenderableWidget(maxBlocksBox);
        y += elementHeight + gap;

        // === Block Delay (SERVER only) ===
        addLabel("Block Delay:", labelX, y + 6);
        blockDelayBox = new EditBox(this.font, fieldX, y, 80, 20, new TextComponent("Block Delay"));
        blockDelayBox.setValue(String.valueOf(ModConfig.SERVER.blockDelay.get()));
        blockDelayBox.setMaxLength(5);
        blockDelayBox.setEditable(isHost);
        addRenderableWidget(blockDelayBox);
        y += elementHeight + gap;

        // === Activation (SERVER only) ===
        toggleShiftButton = makeToggle(fieldX, y, "Shift Required", enableShiftActivation,
            v -> enableShiftActivation = v);
        toggleShiftButton.active = isHost;
        addLabel("Activation", labelX, y + 6);
        y += elementHeight + gap * 2;

        // === Mining Categories (CLIENT only) ===
        addLabel("Mining Categories", this.width / 2, y - 10);
        oresToggleButton   = makeToggle(fieldX, y, "Ores", allowOres, v -> allowOres = v);
        addLabel("Ores", labelX, y+6); y += elementHeight + gap;
        sandToggleButton   = makeToggle(fieldX, y, "Sand", allowSand, v -> allowSand = v);
        addLabel("Sand", labelX, y+6); y += elementHeight + gap;
        clayToggleButton   = makeToggle(fieldX, y, "Clay", allowClay, v -> allowClay = v);
        addLabel("Clay", labelX, y+6); y += elementHeight + gap;
        logsToggleButton   = makeToggle(fieldX, y, "Logs", allowLogs, v -> allowLogs = v);
        addLabel("Logs", labelX, y+6); y += elementHeight + gap;
        gravelToggleButton = makeToggle(fieldX, y, "Gravel", allowGravel, v -> allowGravel = v);
        addLabel("Gravel", labelX, y+6); y += elementHeight + gap * 2;

        // === Behavior (CLIENT only) ===
        addLabel("General Behavior", this.width / 2, y - 10);
        warningToggleButton = makeToggle(fieldX, y, "Warning", showFirstCrouchWarning, v -> showFirstCrouchWarning = v);
        addLabel("First-time Warning", labelX, y+6); y += elementHeight + gap;
        verboseToggleButton = makeToggle(fieldX, y, "Verbose", verboseDebug, v -> verboseDebug = v);
        addLabel("Verbose Debug", labelX, y+6); y += elementHeight + gap;
        tooltipToggleButton = makeToggle(fieldX, y, "Tooltips", showRequirementTooltips, v -> showRequirementTooltips = v);
        addLabel("Tooltips", labelX, y+6); y += elementHeight + gap * 2;

        // === Visuals & Sound (SERVER only) ===
        addLabel("Visuals & Sound", this.width / 2, y - 10);
        particlesToggleButton = makeToggle(fieldX, y, "Particles", enableParticles, v -> enableParticles = v);
        particlesToggleButton.active = isHost;
        addLabel("Enable Particles", labelX, y+6); y += elementHeight + gap;

        addLabel("Custom Sound:", labelX, y+6);
        soundBox = new EditBox(this.font, fieldX, y, 120, 20, new TextComponent("Custom Sound"));
        soundBox.setValue(ModConfig.SERVER.customSound.get());
        soundBox.setMaxLength(128);
        soundBox.setEditable(isHost);
        addRenderableWidget(soundBox);
        y += elementHeight + gap;

        toggleSoundButton = makeToggle(fieldX, y, "Sound", enableSound, v -> enableSound = v);
        toggleSoundButton.active = isHost;
        addLabel("Enable Sound", labelX, y+6); y += elementHeight + gap * 2;

        // === Whitelist (CLIENT only) ===
        addLabel("Whitelist", this.width / 2, y - 10);
        toggleWhitelistButton = makeToggle(fieldX, y, "Whitelist", enableWhitelist, v -> enableWhitelist = v);
        addLabel("Enable Whitelist", labelX, y+6); y += elementHeight + gap;

        addLabel("Blocks:", labelX, y+6);
        whitelistBox = new EditBox(this.font, fieldX, y, 200, 20, new TextComponent("Whitelist"));
        String whitelistText = ModConfig.CLIENT.whitelist.get().stream().collect(Collectors.joining(", "));
        whitelistBox.setValue(whitelistText);
        whitelistBox.setMaxLength(2048);
        addRenderableWidget(whitelistBox);
        y += elementHeight + gap * 2;

        // Save / Cancel
        addRenderableWidget(new Button(this.width / 2 - 120, y, 100, 20,
            new TextComponent("Save"), b -> onSave()));
        addRenderableWidget(new Button(this.width / 2 + 20, y, 100, 20,
            new TextComponent("Cancel"), b -> onClose()));
    }

    // === Helper methods ===
    private void addLabel(String text, int x, int y) {
        this.labels.add(new Label(text, x, y));
    }

    private static class Label {
        final String text; final int x, y;
        Label(String text, int x, int y) { this.text = text; this.x = x; this.y = y; }
    }
    
    private final List<Label> labels = new ArrayList<>();
    private Button makeToggle(int centerX, int y, String label, boolean initial,
                             java.util.function.Consumer<Boolean> setter) {
        final boolean[] state = { initial };
        Button btn = new Button(centerX - 50, y, 100, 20,
            new TextComponent(toggleLabel(label, state[0])),
            b -> {
                state[0] = !state[0];
                setter.accept(state[0]);
                b.setMessage(new TextComponent(toggleLabel(label, state[0])));
            });
        this.addRenderableWidget(btn);
        return btn;
    }
    private String toggleLabel(String name, boolean on) {
        return (on ? "[✓] " : "[ ] ") + name;
    }



    // === Save / Load ===
    private void onSave() {
        CompatUtils.logInfo("Saving VeinMiner configuration...");
        try {
            // SERVER authoritative values (host only)
            if (isHost) {
                double serverDelay = CompatUtils.safeParseDouble(blockDelayBox.getValue(),
                        ModConfig.SERVER.blockDelay.get(), 0.0D, 0.5D);
                ModConfig.SERVER.blockDelay.set(serverDelay);

                ModConfig.SERVER.enableShiftActivation.set(enableShiftActivation);
                ModConfig.SERVER.enableParticles.set(enableParticles);
                ModConfig.SERVER.enableSound.set(enableSound);

                String serverSound = soundBox.getValue().trim();
                if (serverSound.isEmpty()) serverSound = "minecraft:entity.item.pickup";
                ModConfig.SERVER.customSound.set(serverSound);

                // Save SERVER config
                ModConfig.SERVER_SPEC.save();
            }

            // CLIENT local values
            int enteredMax = CompatUtils.safeParseInt(maxBlocksBox.getValue(),
                    ModConfig.CLIENT.clientMaxBlocks.get(), 1, 500);

            // Apply server cap if host
            int cappedMax = isHost ? Math.min(enteredMax, ModConfig.SERVER.maxBlocks.get()) : enteredMax;
            ModConfig.CLIENT.clientMaxBlocks.set(cappedMax);

            ModConfig.CLIENT.allowOres.set(allowOres);
            ModConfig.CLIENT.allowSand.set(allowSand);
            ModConfig.CLIENT.allowClay.set(allowClay);
            ModConfig.CLIENT.allowLogs.set(allowLogs);
            ModConfig.CLIENT.allowGravel.set(allowGravel);

            ModConfig.CLIENT.showFirstCrouchWarning.set(showFirstCrouchWarning);
            ModConfig.CLIENT.verboseDebug.set(verboseDebug);
            ModConfig.CLIENT.showRequirementTooltips.set(showRequirementTooltips);
            ModConfig.CLIENT.enableWhitelist.set(enableWhitelist);

            // Parse whitelist
            String rawWhitelist = whitelistBox.getValue().trim();
            List<String> parsedWhitelist = new ArrayList<>();
            if (!rawWhitelist.isEmpty()) {
                parsedWhitelist = Arrays.stream(rawWhitelist.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .filter(CompatUtils::isValidRegistryName)
                        .collect(Collectors.toList());
            }
            ModConfig.CLIENT.whitelist.set(parsedWhitelist);

            // Save CLIENT config
            ModConfig.CLIENT_SPEC.save();

            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                    "VeinMiner configuration saved successfully!");
            onClose();
        } catch (Exception e) {
            CompatUtils.logError("Error saving configuration", e);
            CompatUtils.sendPlayerMessage(Minecraft.getInstance().player,
                    "Error saving configuration: " + e.getMessage());
        }
    }


    // === GUI overrides ===
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        for (Label label : labels) {
            if (label.x == this.width / 2) {
                int textWidth = this.font.width(label.text);
                this.font.draw(poseStack, label.text, label.x - textWidth / 2, label.y, 0xFFFF55);
            } else {
                this.font.draw(poseStack, label.text, label.x, label.y, 0xFFFFFF);
            }
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
        super.onClose();
    }
}
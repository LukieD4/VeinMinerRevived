package com.lukiedoggo.veinminerrevived.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import com.lukiedoggo.veinminerrevived.util.CompatUtils;

import java.util.Collections;
import java.util.List;

public class ModConfig {
    // ========== CONFIG SPEC DEFINITIONS ==========
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        CompatUtils.logInfo("Initializing VeinMiner configuration...");

        // CLIENT
        final Pair<Client, ForgeConfigSpec> clientPair =
                new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();

        // SERVER (authoritative, synced to clients)
        final Pair<Server, ForgeConfigSpec> serverPair =
                new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = serverPair.getRight();
        SERVER = serverPair.getLeft();

        CompatUtils.logInfo("Configuration initialized successfully");
    }

    // ============================================================
    // SERVER CONFIGURATION (authoritative, synced to clients)
    // ============================================================
    public static class Server {
        public final ForgeConfigSpec.IntValue maxBlocks;
        public final ForgeConfigSpec.DoubleValue blockDelay;
        public final ForgeConfigSpec.BooleanValue enableShiftActivation;
        public final ForgeConfigSpec.BooleanValue enableParticles;
        public final ForgeConfigSpec.BooleanValue enableSound;
        public final ForgeConfigSpec.ConfigValue<String> customSound;

        public Server(ForgeConfigSpec.Builder builder) {
            CompatUtils.logDebug("Building server configuration...");

            builder.push("veinminer_server_settings");

            maxBlocks = builder
                    .comment("Maximum number of blocks veinminer can break in one operation (Server authoritative)")
                    .defineInRange("maxBlocks", 64, 1, 500);

            blockDelay = builder
                    .comment("Delay between breaking blocks (in seconds) (Server authoritative)")
                    .defineInRange("blockDelay", 0.1D, 0.0D, 0.5D);

            enableShiftActivation = builder
                    .comment("If true, players must hold Shift. If false, clients may toggle freely.")
                    .define("enableShiftActivation", true);

            enableParticles = builder
                    .comment("If false, disables particles globally. If true, clients may disable locally.")
                    .define("enableParticles", false);

            enableSound = builder
                    .comment("Play veinmining sounds (Server authoritative)")
                    .define("enableSound", true);

            customSound = builder
                    .comment("Custom sound to play for veinmining (Server authoritative)")
                    .define("customSound", "minecraft:entity.item.pickup");

            builder.pop();
        }
    }

    // ============================================================
    // CLIENT CONFIGURATION (player-controlled, limited by server)
    // ============================================================
    public static class Client {
        public final ForgeConfigSpec.IntValue clientMaxBlocks;
        public final ForgeConfigSpec.BooleanValue allowOres;
        public final ForgeConfigSpec.BooleanValue allowSand;
        public final ForgeConfigSpec.BooleanValue allowClay;
        public final ForgeConfigSpec.BooleanValue allowLogs;
        public final ForgeConfigSpec.BooleanValue allowGravel;
        public final ForgeConfigSpec.BooleanValue enableWhitelist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;

        public final ForgeConfigSpec.BooleanValue showFirstCrouchWarning;
        public final ForgeConfigSpec.BooleanValue verboseDebug;
        public final ForgeConfigSpec.BooleanValue showRequirementTooltips;
        public final ForgeConfigSpec.BooleanValue enableParticles;

        public Client(ForgeConfigSpec.Builder builder) {
            CompatUtils.logDebug("Building client configuration...");

            builder.push("veinminer_client_settings");

            clientMaxBlocks = builder
                    .comment("Client-side block cap (capped by server maxBlocks)")
                    .defineInRange("clientMaxBlocks", 64, 1, 500);

            allowOres = builder.comment("Allow veinmining of ores").define("allowOres", true);
            allowSand = builder.comment("Allow veinmining of sand").define("allowSand", false);
            allowClay = builder.comment("Allow veinmining of clay").define("allowClay", false);
            allowLogs = builder.comment("Allow veinmining of logs").define("allowLogs", false);
            allowGravel = builder.comment("Allow veinmining of gravel").define("allowGravel", false);

            enableWhitelist = builder.comment("Enable client whitelist mode").define("enableWhitelist", false);
            whitelist = builder.comment("Whitelist entries (Client)")
                    .defineList("whitelist", Collections.emptyList(),
                            obj -> obj instanceof String && CompatUtils.isValidRegistryName((String) obj));

            showFirstCrouchWarning = builder.comment("Show crouch hint").define("showFirstCrouchWarning", true);
            verboseDebug = builder.comment("Enable verbose debug in chat").define("verboseDebug", false);
            showRequirementTooltips = builder.comment("Show tooltips if requirements not met").define("showRequirementTooltips", true);

            enableParticles = builder.comment("Show particles (only if server allows)").define("enableParticles", true);

            builder.pop();
        }
    }

    // ============================================================
    // HELPER METHODS (apply rules between server/client)
    // ============================================================
    public static int getEffectiveMaxBlocks() {
        int client = CLIENT.clientMaxBlocks.get();
        int server = SERVER.maxBlocks.get();
        return Math.min(client, server); // client capped by server
    }

    public static double getEffectiveBlockDelay() {
        return SERVER.blockDelay.get(); // server only
    }

    public static boolean getEffectiveShiftActivation() {
        return SERVER.enableShiftActivation.get(); // server always authoritative
    }

    public static boolean getEffectiveParticles() {
        if (!SERVER.enableParticles.get()) {
            return false; // server disables globally
        }
        return CLIENT.enableParticles.get(); // otherwise client choice
    }

    public static boolean getEffectiveSound() {
        return SERVER.enableSound.get();
    }

    public static String getEffectiveCustomSound() {
        return SERVER.customSound.get();
    }
}

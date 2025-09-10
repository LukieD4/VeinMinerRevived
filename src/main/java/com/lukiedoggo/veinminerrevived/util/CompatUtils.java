package com.lukiedoggo.veinminerrevived.util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
/**
 * CompatUtils - Compatibility utility class for transitioning between Minecraft versions.
 * Currently focused on Forge 1.18.2-40.3.11-mdk
 * 
 * This class wraps version-specific functionality to make future updates easier.
 */
public class CompatUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Resolves a ResourceLocation from a string, using the safest method available.
     * For 1.18.2, uses constructor. For 1.20.6+, switch to ResourceLocation.parse().
     */
    public static ResourceLocation resolveResourceLocation(String resourceString) {
        try {
            // Safe for 1.18.2
            return new ResourceLocation(resourceString);
            // Future version: return ResourceLocation.parse(resourceString);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse ResourceLocation: {}", resourceString, e);
            return new ResourceLocation("minecraft", "air"); // Safe fallback
        }
    }
    /**
     * Gets the registry name of a block as a string.
     * Handles potential null registry names gracefully.
     */
    public static String getBlockName(Block block) {
        try {
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
            return key != null ? key.toString() : "unknown_block";
        } catch (Exception e) {
            LOGGER.warn("Failed to get block name for block: {}", block.getClass().getSimpleName(), e);
            return "unknown_block";
        }
    }
    /**
     * Sends a chat message to a player.
     * Wraps the version-specific message sending logic.
     */
    public static void sendPlayerMessage(Player player, String message) {
        try {
            // 1.18.2 compatible method
            Component textComponent = new TextComponent(message);
            player.sendMessage(textComponent, player.getUUID());
        } catch (Exception e) {
            LOGGER.error("Failed to send message to player {}: {}", player.getName().getString(), message, e);
        }
    }
    /**
     * Sends an action bar message to a player.
     * For 1.18.2, falls back to regular chat if action bar fails.
     */
    public static void sendActionBarMessage(Player player, String message) {
        try {
            // Try to send as action bar message
            Component textComponent = new TextComponent(message);
            player.displayClientMessage(textComponent, true); // true = action bar
        } catch (Exception e) {
            LOGGER.debug("Action bar failed, falling back to chat message for: {}", message);
            // Fallback to regular message
            sendPlayerMessage(player, message);
        }
    }
    /**
     * Plays a sound at a specific location.
     * Handles sound registration lookup and error cases.
     */
    public static void playSound(Level level, BlockPos pos, String soundName, Player player) {
        try {
            if (soundName == null || soundName.trim().isEmpty()) {
                soundName = "minecraft:entity.item.pickup"; // Default fallback
            }
            ResourceLocation soundLocation = resolveResourceLocation(soundName.trim());
            SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundLocation);
            
            if (soundEvent != null) {
                // 1.18.2 method signature
                level.playSound(
                    null, // player (null for server-side)
                    pos,
                    soundEvent,
                    SoundSource.PLAYERS,
                    1.0F, // volume
                    1.0F  // pitch
                );
                LOGGER.debug("Played sound {} at {}", soundName, pos);
            } else {
                LOGGER.warn("Sound not found in registry: {}", soundName);
                // Try default sound as fallback
                if (!soundName.equals("minecraft:entity.item.pickup")) {
                    playSound(level, pos, "minecraft:entity.item.pickup", player);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to play sound {} at {}", soundName, pos, e);
        }
    }
    /**
     * Checks if an ItemStack is broken (durability depleted).
     * Handles edge cases and null checks.
     */
    public static boolean isToolBroken(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) {
                return true;
            }
            
            if (!stack.isDamageableItem()) {
                return false; // Items without durability can't break
            }
            
            return stack.getDamageValue() >= stack.getMaxDamage();
        } catch (Exception e) {
            LOGGER.error("Error checking tool durability for {}", stack, e);
            return true; // Assume broken on error for safety
        }
    }
    /**
     * Gets the maximum durability of an ItemStack.
     * Returns 0 for non-damageable items.
     */
    public static int getMaxDurability(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
                return 0;
            }
            return stack.getMaxDamage();
        } catch (Exception e) {
            LOGGER.error("Error getting max durability for {}", stack, e);
            return 0;
        }
    }
    /**
     * Gets the current damage of an ItemStack.
     * Returns 0 for non-damageable items.
     */
    public static int getCurrentDamage(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
                return 0;
            }
            return stack.getDamageValue();
        } catch (Exception e) {
            LOGGER.error("Error getting current damage for {}", stack, e);
            return 0;
        }
    }
    /**
     * Creates a text component from a string.
     * Wraps version-specific component creation.
     */
    public static Component createTextComponent(String text) {
        try {
            // 1.18.2 method
            return new TextComponent(text);
            // Future versions might use Component.literal(text)
        } catch (Exception e) {
            LOGGER.error("Failed to create text component for: {}", text, e);
            return new TextComponent("Error"); // Safe fallback
        }
    }
    /**
     * Safely converts a string to an integer with bounds checking.
     * Returns default value if parsing fails.
     */
    public static int safeParseInt(String value, int defaultValue, int min, int max) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            LOGGER.debug("Failed to parse integer '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }
    /**
     * Safely converts a string to a double with bounds checking.
     * Returns default value if parsing fails.
     */
    public static double safeParseDouble(String value, double defaultValue, double min, double max) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            
            double parsed = Double.parseDouble(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            LOGGER.debug("Failed to parse double '{}', using default: {}", value, defaultValue);
            return defaultValue;
        }
    }
    /**
     * Validates a block registry name format.
     * Returns true if the name appears to be a valid registry name.
     */
    public static boolean isValidRegistryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = name.trim();
        
        // Basic format check: namespace:path
        if (!trimmed.contains(":")) {
            return false;
        }
        
        String[] parts = trimmed.split(":", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return false;
        }
        
        // Check for invalid characters (basic check)
        return trimmed.matches("[a-z0-9_.-]+:[a-z0-9/_.-]+");
    }
    /**
     * Safely gets a player's hunger level.
     * Returns 0 if there's an error accessing the food data.
     */
    public static int getPlayerHunger(Player player) {
        try {
            return player.getFoodData().getFoodLevel();
        } catch (Exception e) {
            LOGGER.error("Error getting hunger level for player {}", player.getName().getString(), e);
            return 0;
        }
    }
    /**
     * Safely sets a player's hunger level.
     * Bounds checking included.
     */
    public static void setPlayerHunger(Player player, int level) {
        try {
            int boundedLevel = Math.max(0, Math.min(20, level));
            player.getFoodData().setFoodLevel(boundedLevel);
        } catch (Exception e) {
            LOGGER.error("Error setting hunger level for player {} to {}", 
                player.getName().getString(), level, e);
        }
    }
    /**
     * Logs a debug message with VeinMiner prefix.
     * Centralized logging for easier debugging.
     */
    public static void logDebug(String message, Object... args) {
        LOGGER.debug("[VeinMiner] " + message, args);
    }
    /**
     * Logs an info message with VeinMiner prefix.
     */
    public static void logInfo(String message, Object... args) {
        LOGGER.info("[VeinMiner] " + message, args);
    }
    /**
     * Logs an error message with VeinMiner prefix.
     */
    public static void logError(String message, Throwable t) {
        LOGGER.error("[VeinMiner] " + message, t);
    }
    /**
     * Logs a warning message with VeinMiner prefix.
     */
    public static void logWarning(String message, Object... args) {
        LOGGER.warn("[VeinMiner] " + message, args);
    }
}
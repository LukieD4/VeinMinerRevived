package com.lukiedoggo.veinminerrevived.commands;
// package com.example.veinminerrevived.commands;

// import com.example.veinminerrevived.config.ModConfig;
// import com.example.veinminerrevived.events.VeinMinerEvents;
// import com.example.veinminerrevived.util.CompatUtils;
// import com.mojang.brigadier.CommandDispatcher;
// import com.mojang.brigadier.context.CommandContext;
// import net.minecraft.commands.CommandSourceStack;
// import net.minecraft.commands.Commands;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.level.block.Block;
// import net.minecraft.world.level.block.state.BlockState;
// import net.minecraft.core.BlockPos;
// import net.minecraftforge.event.RegisterCommandsEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;

// /**
//  * Debug command for VeinMiner troubleshooting.
//  * Usage: /veinminer debug
//  */
// @Mod.EventBusSubscriber
// public class VeinMinerDebugCommand {

//     @SubscribeEvent
//     public static void registerCommands(RegisterCommandsEvent event) {
//         CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
//         dispatcher.register(
//             Commands.literal("veinminer")
//                 .then(Commands.literal("debug")
//                     .requires(source -> source.hasPermission(2)) // Require OP
//                     .executes(VeinMinerDebugCommand::executeDebug))
//                 .then(Commands.literal("reload")
//                     .requires(source -> source.hasPermission(2)) // Require OP
//                     .executes(VeinMinerDebugCommand::executeReload))
//                 .then(Commands.literal("test")
//                     .requires(source -> source.hasPermission(2)) // Require OP
//                     .executes(VeinMinerDebugCommand::executeTest))
//         );
        
//         CompatUtils.logInfo("VeinMiner debug commands registered");
//     }

//     private static int executeDebug(CommandContext<CommandSourceStack> context) {
//         CommandSourceStack source = context.getSource();
        
//         try {
//             Player player = source.getPlayerOrException();
            
//             CompatUtils.sendPlayerMessage(player, "=== VeinMiner Debug Information ===");
            
//             // System status
//             CompatUtils.sendPlayerMessage(player, "§aSystem Status: ACTIVE");
            
//             // Configuration status
//             boolean shiftRequired = ModConfig.COMMON.enableShiftActivation.get();
//             int maxBlocks = ModConfig.COMMON.maxBlocks.get();
//             double blockDelay = ModConfig.COMMON.blockDelay.get();
//             boolean verboseDebug = ModConfig.COMMON.verboseDebug.get();
//             boolean oresAllowed = ModConfig.COMMON.allowOres.get();
//             boolean whitelistEnabled = ModConfig.COMMON.enableWhitelist.get();
            
//             CompatUtils.sendPlayerMessage(player, String.format("§bShift Required: %s", shiftRequired ? "YES" : "NO"));
//             CompatUtils.sendPlayerMessage(player, String.format("§bMax Blocks: %d", maxBlocks));
//             CompatUtils.sendPlayerMessage(player, String.format("§bBlock Delay: %.3f seconds", blockDelay));
//             CompatUtils.sendPlayerMessage(player, String.format("§bVerbose Debug: %s", verboseDebug ? "ON" : "OFF"));
//             CompatUtils.sendPlayerMessage(player, String.format("§bOres Allowed: %s", oresAllowed ? "YES" : "NO"));
//             CompatUtils.sendPlayerMessage(player, String.format("§bWhitelist Mode: %s", whitelistEnabled ? "ON" : "OFF"));
            
//             if (whitelistEnabled) {
//                 int whitelistSize = ModConfig.COMMON.whitelist.get().size();
//                 CompatUtils.sendPlayerMessage(player, String.format("§bWhitelist Entries: %d", whitelistSize));
//             }
            
//             // Player status
//             boolean isShiftPressed = player.isCrouching();
//             int hunger = CompatUtils.getPlayerHunger(player);
//             ItemStack heldItem = player.getMainHandItem();
            
//             CompatUtils.sendPlayerMessage(player, String.format("§ePlayer Shift: %s", isShiftPressed ? "PRESSED" : "NOT PRESSED"));
//             CompatUtils.sendPlayerMessage(player, String.format("§ePlayer Hunger: %d/20", hunger));
//             CompatUtils.sendPlayerMessage(player, String.format("§eHeld Item: %s", 
//                 heldItem.isEmpty() ? "NONE" : heldItem.getDisplayName().getString()));
            
//             // Block under player
//             BlockPos pos = player.blockPosition().below();
//             BlockState state = player.level.getBlockState(pos);
//             Block block = state.getBlock();
//             String blockName = CompatUtils.getBlockName(block);
//             boolean isAllowed = VeinMinerEvents.isAllowedBlock(block);
//             boolean canHarvest = heldItem.isCorrectToolForDrops(state);
            
//             CompatUtils.sendPlayerMessage(player, String.format("§6Block Below: %s", blockName));
//             CompatUtils.sendPlayerMessage(player, String.format("§6Block Allowed: %s", isAllowed ? "YES" : "NO"));
//             CompatUtils.sendPlayerMessage(player, String.format("§6Can Harvest: %s", canHarvest ? "YES" : "NO"));
            
//             // Activation check
//             boolean wouldActivate = (!shiftRequired || isShiftPressed) && 
//                                   hunger >= 4 && 
//                                   !heldItem.isEmpty() && 
//                                   canHarvest && 
//                                   isAllowed;
            
//             CompatUtils.sendPlayerMessage(player, String.format("§c§lWould VeinMiner Activate: %s", wouldActivate ? "YES" : "NO"));
            
//             if (!wouldActivate) {
//                 CompatUtils.sendPlayerMessage(player, "§cReasons for not activating:");
//                 if (shiftRequired && !isShiftPressed) {
//                     CompatUtils.sendPlayerMessage(player, "§c- Shift not pressed (required)");
//                 }
//                 if (hunger < 4) {
//                     CompatUtils.sendPlayerMessage(player, "§c- Insufficient hunger (need 4+)");
//                 }
//                 if (heldItem.isEmpty()) {
//                     CompatUtils.sendPlayerMessage(player, "§c- No tool equipped");
//                 }
//                 if (!heldItem.isEmpty() && !canHarvest) {
//                     CompatUtils.sendPlayerMessage(player, "§c- Wrong tool for block");
//                 }
//                 if (!isAllowed) {
//                     CompatUtils.sendPlayerMessage(player, "§c- Block not allowed by config");
//                 }
//             }
            
//             return 1;
            
//         } catch (Exception e) {
//             CompatUtils.logError("Error executing debug command", e);
//             return 0;
//         }
//     }

//     private static int executeReload(CommandContext<CommandSourceStack> context) {
//         CommandSourceStack source = context.getSource();
        
//         try {
//             Player player = source.getPlayerOrException();
            
//             CompatUtils.sendPlayerMessage(player, "§eReloading VeinMiner configuration...");
//             ModConfig.reload();
//             CompatUtils.sendPlayerMessage(player, "§aConfiguration reloaded successfully!");
            
//             return 1;
            
//         } catch (Exception e) {
//             CompatUtils.logError("Error executing reload command", e);
//             try {
//                 Player player = source.getPlayerOrException();
//                 CompatUtils.sendPlayerMessage(player, "§cError reloading configuration: " + e.getMessage());
//             } catch (Exception ignored) {}
//             return 0;
//         }
//     }

//     private static int executeTest(CommandContext<CommandSourceStack> context) {
//         CommandSourceStack source = context.getSource();
        
//         try {
//             Player player = source.getPlayerOrException();
            
//             CompatUtils.sendPlayerMessage(player, "§eRunning VeinMiner system tests...");
            
//             // Test 1: Configuration access
//             try {
//                 int maxBlocks = ModConfig.COMMON.maxBlocks.get();
//                 CompatUtils.sendPlayerMessage(player, "§a✓ Configuration access: OK");
//             } catch (Exception e) {
//                 CompatUtils.sendPlayerMessage(player, "§c✗ Configuration access: FAILED");
//                 CompatUtils.logError("Config test failed", e);
//             }
            
//             // Test 2: Block registry access
//             try {
//                 String ironOreName = CompatUtils.getBlockName(net.minecraft.world.level.block.Blocks.IRON_ORE);
//                 boolean isIronAllowed = VeinMinerEvents.isAllowedBlock(net.minecraft.world.level.block.Blocks.IRON_ORE);
//                 CompatUtils.sendPlayerMessage(player, "§a✓ Block registry access: OK");
//                 CompatUtils.sendPlayerMessage(player, String.format("  Iron ore: %s (allowed: %s)", ironOreName, isIronAllowed));
//             } catch (Exception e) {
//                 CompatUtils.sendPlayerMessage(player, "§c✗ Block registry access: FAILED");
//                 CompatUtils.logError("Block registry test failed", e);
//             }
            
//             // Test 3: Sound system
//             try {
//                 String soundName = ModConfig.COMMON.customSound.get();
//                 boolean isValidSound = CompatUtils.isValidRegistryName(soundName);
//                 CompatUtils.sendPlayerMessage(player, String.format("§a✓ Sound system: OK (using %s, valid: %s)", soundName, isValidSound));
//             } catch (Exception e) {
//                 CompatUtils.sendPlayerMessage(player, "§c✗ Sound system: FAILED");
//                 CompatUtils.logError("Sound test failed", e);
//             }
            
//             // Test 4: Event system
//             try {
//                 // This is a simple test - just check if we can access the event class
//                 VeinMinerEvents.class.getName();
//                 CompatUtils.sendPlayerMessage(player, "§a✓ Event system: OK");
//             } catch (Exception e) {
//                 CompatUtils.sendPlayerMessage(player, "§c✗ Event system: FAILED");
//                 CompatUtils.logError("Event system test failed", e);
//             }
            
//             CompatUtils.sendPlayerMessage(player, "§eSystem tests completed!");
            
//             return 1;
            
//         } catch (Exception e) {
//             CompatUtils.logError("Error executing test command", e);
//             return 0;
//         }
//     }
// }
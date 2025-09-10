package com.lukiedoggo.veinminerrevived.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.lukiedoggo.veinminerrevived.config.ModConfig;
import com.lukiedoggo.veinminerrevived.util.CompatUtils;

/**
 * VeinMinerEvents — central event manager for veinmining.
 * Enforces Client/Server authority rules as specified.
 */
public class VeinMinerEvents {

    /* -------------------------
       Job Queues
       ------------------------- */
    private static final Queue<VeinJob> ACTIVE_JOBS = new ConcurrentLinkedQueue<>();
    private static final Queue<VeinJob> PENDING_JOBS = new ConcurrentLinkedQueue<>();

    /* -------------------------
       Tuning & Safety Limits
       ------------------------- */
    private static final int MAX_GLOBAL_JOBS = 6;
    private static final int MAX_BLOCKS_PER_TICK = 200;
    private static final int MAX_CONSECUTIVE_SKIPS = 6;
    private static final int MIN_PER_JOB_BUDGET = 2;
    private static final long MIN_DELAY_MS = 5L;

    /* -------------------------
       Warning Safety
       ------------------------- */
    private static final long WARNING_BLOCK_MS = 2000; // 2 seconds
    private static final String TAG_WARNED = "veinminerWarned";
    private static final String TAG_LAST_WARNING = "veinminerLastWarning";

    private long firstTimeCrouchEpoch = System.currentTimeMillis();
    private Level lastClientLevel = null;

    public VeinMinerEvents() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Check if player is still in the cooldown window
    private boolean isInWarningCooldown(Player player) {
        long elapsed = System.currentTimeMillis() - firstTimeCrouchEpoch;
        boolean inCooldown = elapsed < WARNING_BLOCK_MS;
        CompatUtils.logDebug("Player {} warning cooldown: {} ({} ms elapsed)",
                player.getName().getString(), inCooldown, elapsed);
        return inCooldown;
    }

    /* -------------------------
       Block Break Trigger
       ------------------------- */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = event.getWorld() instanceof Level ? (Level) event.getWorld() : null;
        if (level == null || level.isClientSide()) return;

        // === Shift activation rule ===
        // Server forces crouch if enabled; if server disabled, client may act without crouch (toggle not present yet).
        boolean serverRequiresShift = ModConfig.SERVER.enableShiftActivation.get();
        if (serverRequiresShift && !player.isCrouching()) {
            if (ModConfig.CLIENT.showFirstCrouchWarning.get() && !player.getPersistentData().getBoolean(TAG_WARNED)) {
                CompatUtils.sendPlayerMessage(player, "Tip: Hold shift to activate VeinMiner!");
                player.getPersistentData().putBoolean(TAG_WARNED, true);
                player.getPersistentData().putLong(TAG_LAST_WARNING, System.currentTimeMillis());
            }
            return;
        }

        // Prevent VeinMining during the warning window
        if (isInWarningCooldown(player)) return;

        // Require hunger
        if (CompatUtils.getPlayerHunger(player) < 4) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        // Require proper tool
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isCorrectToolForDrops(state)) return;

        // Must be allowed by CLIENT config (categories/whitelist are client-authority)
        if (!isAllowedBlock(block)) return;

        // Create job with min(clientMax, serverMax)
        int clientMax = ModConfig.CLIENT.clientMaxBlocks.get();
        int serverMax = ModConfig.SERVER.maxBlocks.get();
        int actualMax = Math.min(clientMax, serverMax);

        VeinJob job = new VeinJob((ServerLevel) level, player.getUUID(), pos, block, actualMax);
        if (ACTIVE_JOBS.size() < MAX_GLOBAL_JOBS) ACTIVE_JOBS.add(job);
        else PENDING_JOBS.add(job);
    }

    /* -------------------------
       Client Tick: Join / Warning Handling
       ------------------------- */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Level currentLevel = net.minecraft.client.Minecraft.getInstance().level;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !ModConfig.CLIENT.showFirstCrouchWarning.get()) return;

        if (lastClientLevel != currentLevel && player.isCrouching() && !player.getPersistentData().getBoolean(TAG_WARNED)) {
            lastClientLevel = currentLevel;
            firstTimeCrouchEpoch = System.currentTimeMillis();
            player.getPersistentData().putBoolean(TAG_WARNED, true);
            String warn = "[VeinMiner] VeinMining is enabled with a destruction block count of §6" +
                    ModConfig.CLIENT.clientMaxBlocks.get() + "§r, be careful!";
            CompatUtils.sendPlayerMessage(player, warn);
        }
    }

    /* -------------------------
       Server Tick: Job Processing
       ------------------------- */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        while (ACTIVE_JOBS.size() < MAX_GLOBAL_JOBS && !PENDING_JOBS.isEmpty()) {
            ACTIVE_JOBS.add(PENDING_JOBS.poll());
        }

        int remaining = MAX_BLOCKS_PER_TICK;

        // === Block delay rule (server authoritative) ===
        double delay = ModConfig.SERVER.blockDelay.get();
        if (delay > 0.2) remaining = Math.max(20, remaining / 4);
        else if (delay > 0.05) remaining = Math.max(50, remaining / 2);

        List<VeinJob> jobsSnapshot = new ArrayList<>(ACTIVE_JOBS);
        if (jobsSnapshot.isEmpty()) return;

        int share = Math.max(MIN_PER_JOB_BUDGET, remaining / jobsSnapshot.size());

        Iterator<VeinJob> it = ACTIVE_JOBS.iterator();
        while (it.hasNext() && remaining > 0) {
            VeinJob job = it.next();
            int processed = job.processNow(Math.min(share, remaining));
            remaining -= processed;
            if (job.isFinished()) it.remove();
        }

        // Leftover budget pass
        it = ACTIVE_JOBS.iterator();
        while (it.hasNext() && remaining > 0) {
            VeinJob job = it.next();
            int processed = job.processNow(Math.min(remaining, MIN_PER_JOB_BUDGET * 4));
            remaining -= processed;
            if (job.isFinished()) it.remove();
        }
    }

    /* -------------------------
       Config Checks (CLIENT authority)
       ------------------------- */
    public static boolean isAllowedBlock(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        if (key == null) return false;

        // If client enabled "whitelist only", then only client whitelist applies.
        if (ModConfig.CLIENT.enableWhitelist.get()) {
            List<? extends String> wl = ModConfig.CLIENT.whitelist.get();
            return wl.contains(key.toString());
        }

        // Otherwise, client categories decide (server does NOT override these).
        String path = key.getPath();

        if (isOre(path) && ModConfig.CLIENT.allowOres.get()) return true;
        if ((block == Blocks.SAND || block == Blocks.RED_SAND) && ModConfig.CLIENT.allowSand.get()) return true;
        if (block == Blocks.CLAY && ModConfig.CLIENT.allowClay.get()) return true;
        if ((path.contains("log") || path.contains("stem")) && ModConfig.CLIENT.allowLogs.get()) return true;
        if (block == Blocks.GRAVEL && ModConfig.CLIENT.allowGravel.get()) return true;

        return false;
    }

    private static boolean isOre(String path) {
        return (path.endsWith("_ore") || path.endsWith("deepslate_ore")) && !path.endsWith("_ores");
    }

    private static boolean areSameOreFamily(Block a, Block b) {
        ResourceLocation ka = ForgeRegistries.BLOCKS.getKey(a);
        ResourceLocation kb = ForgeRegistries.BLOCKS.getKey(b);
        if (ka == null || kb == null) return false;
        String sa = ka.getPath().replace("deepslate_", "");
        String sb = kb.getPath().replace("deepslate_", "");
        return sa.equals(sb);
    }

    /* -------------------------
       VeinJob Inner Class
       ------------------------- */
    private static class VeinJob {
        private final ServerLevel level;
        private final UUID playerId;
        private final Block origin;
        private final int maxBlocks;

        private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        private final HashSet<Long> visited = new HashSet<>();

        private int mined = 0;
        private int skips = 0;
        private long nextProcessTime = 0L;
        private boolean finished = false;

        VeinJob(ServerLevel level, UUID playerId, BlockPos start, Block origin, int maxBlocks) {
            this.level = level;
            this.playerId = playerId;
            this.origin = origin;
            this.maxBlocks = Math.max(1, maxBlocks);
            enqueue(start);
            enqueueNeighbors(start);
            this.nextProcessTime = System.currentTimeMillis();
        }

        int processNow(int budget) {
            if (finished || budget <= 0) return 0;

            long now = System.currentTimeMillis();
            long delayMs = Math.max(MIN_DELAY_MS, (long) (ModConfig.SERVER.blockDelay.get() * 1000));
            if (now < nextProcessTime) return 0;

            Player player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) return markFinished();

            ItemStack tool = player.getMainHandItem();
            int attempts = 0;

            while (attempts < budget && mined < maxBlocks && !queue.isEmpty()) {
                now = System.currentTimeMillis();
                if (now < nextProcessTime) break;

                BlockPos pos = queue.poll();
                if (pos == null) continue;
                attempts++;

                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();

                if (areSameOreFamily(block, origin)) {
                    if (tool.isEmpty() || !tool.isCorrectToolForDrops(state)) {
                        skips++;
                    } else if (level.destroyBlock(pos, true, player)) {
                        mined++;
                        skips = 0;

                        // Tool damage + break
                        tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                        if (CompatUtils.isToolBroken(tool)) {
                            CompatUtils.sendActionBarMessage(player, "[VeinMiner] Tool broke — ending jobs.");
                            terminateJobsFor(player.getUUID());
                            return markFinished();
                        }

                        // Hunger drain (unchanged)
                        if (level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && !player.isCreative()) {
                            if (mined % 4 == 0) {
                                int newHunger = Math.max(CompatUtils.getPlayerHunger(player) - 1, 0);
                                CompatUtils.setPlayerHunger(player, newHunger);
                            }
                        }

                        // === Particles rule ===
                        // Server can disable globally. If server enabled, you can wire a client toggle later.
                        if (ModConfig.SERVER.enableParticles.get()) {
                            level.sendParticles(
                                    ParticleTypes.CRIT,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    5, 0.2, 0.2, 0.2, 0.01
                            );
                        }

                        // === Sound rule (server authoritative) ===
                        if (ModConfig.SERVER.enableSound.get()) {
                            String soundName = ModConfig.SERVER.customSound.get();
                            CompatUtils.playSound(level, pos, soundName, player);
                        }

                        if (ModConfig.CLIENT.verboseDebug.get()) {
                            player.displayClientMessage(
                                    new TextComponent("[VeinMiner Debug] +" + mined + " / " + maxBlocks + " at " + pos),
                                    true
                            );
                        }

                        enqueueNeighbors(pos);
                    } else {
                        skips++;
                    }
                } else {
                    skips += enqueueNeighbors(pos) == 0 ? 1 : 0;
                }

                nextProcessTime = System.currentTimeMillis() + delayMs;

                if (mined >= maxBlocks || skips >= MAX_CONSECUTIVE_SKIPS) return markFinished();
            }

            if (queue.isEmpty() || mined >= maxBlocks) return markFinished();
            return attempts;
        }

        private boolean isFinished() { return finished; }

        private int enqueueNeighbors(BlockPos center) {
            int added = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = center.offset(dx, dy, dz);
                        if (visited.add(neighbor.asLong())) {
                            Block nBlock = level.getBlockState(neighbor).getBlock();
                            if (areSameOreFamily(nBlock, origin)) {
                                queue.add(neighbor);
                                added++;
                            }
                        }
                    }
                }
            }
            return added;
        }

        private void enqueue(BlockPos pos) {
            if (visited.add(pos.asLong())) queue.add(pos);
        }

        private int markFinished() {
            this.finished = true;
            return 0;
        }

        private void terminateJobsFor(UUID id) {
            ACTIVE_JOBS.removeIf(j -> j.playerId.equals(id));
            PENDING_JOBS.removeIf(j -> j.playerId.equals(id));
        }
    }
}

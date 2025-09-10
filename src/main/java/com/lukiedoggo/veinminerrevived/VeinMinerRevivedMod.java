package com.lukiedoggo.veinminerrevived;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import java.util.stream.Collectors;
// The value here should match an entry in the META-INF/mods.toml file
@Mod("veinminerrevived")
public class VeinMinerRevivedMod
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public VeinMinerRevivedMod()
    {
        // Register lifecycle listeners
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register the server config (per-world, synced to clients)
        ModLoadingContext.get().registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            com.lukiedoggo.veinminerrevived.config.ModConfig.SERVER_SPEC
        );

        // Register the client config (local only)
        ModLoadingContext.get().registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
            com.lukiedoggo.veinminerrevived.config.ModConfig.CLIENT_SPEC
        );

        // // Register the common config (shared, non-synced)
        // ModLoadingContext.get().registerConfig(
        //     net.minecraftforge.fml.config.ModConfig.Type.COMMON,
        //     com.example.veinminerrevived.config.ModConfig.COMMON_SPEC
        // );

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new com.lukiedoggo.veinminerrevived.events.VeinMinerEvents());
    }
    private void setup(final FMLCommonSetupEvent event)
    {
        // LOGGER.info("HELLO FROM PREINIT");
        // LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }
    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        InterModComms.sendTo("veinminerrevived", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }
    private void processIMC(final InterModProcessEvent event)
    {
        LOGGER.info("Got IMC {}", event.getIMCStream().map(m->m.messageSupplier().get()).collect(Collectors.toList()));
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent)
        {
            LOGGER.info("HELLO from Register Block");
        }
    }
}
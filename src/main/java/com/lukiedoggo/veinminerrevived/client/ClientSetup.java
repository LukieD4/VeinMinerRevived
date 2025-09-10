package com.lukiedoggo.veinminerrevived.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextComponent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event) {
        // Initialize keybindings
        KeyBindings.init();

        // Register client tick listener
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
    }

    private static void onClientTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Make sure Minecraft is ready
        if (mc.player == null) return;
        LocalPlayer player = mc.player;

        // Trigger the key only once per press
        if (KeyBindings.OPEN_CONFIG.consumeClick()) {

            // Only allow editing if singleplayer or hosting server
            boolean canEdit = mc.hasSingleplayerServer() || player.hasPermissions(2);

            if (canEdit) {
                mc.setScreen(new VeinMinerConfigScreen(mc.screen)); // open the config UI
            } else {
                // If on a dedicated server, show a message instead
                player.sendMessage(
                        new TextComponent("VeinMiner config can only be edited in singleplayer or if you are the host."),
                        player.getUUID()
                );
            }
        }
    }
}

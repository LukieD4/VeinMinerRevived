package com.lukiedoggo.veinminerrevived.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.client.ClientRegistry;

public class KeyBindings {
    public static KeyMapping OPEN_CONFIG;

    public static void init() {
        OPEN_CONFIG = new KeyMapping(
            "Open Configuration Menu",   // translation key (optional)
            GLFW.GLFW_KEY_RIGHT_SHIFT,               // default: V
            "key.categories.VeinMinerRevived"          // key category
        );

        ClientRegistry.registerKeyBinding(OPEN_CONFIG);
    }
}

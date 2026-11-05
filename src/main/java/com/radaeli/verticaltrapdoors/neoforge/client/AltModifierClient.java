package com.radaeli.verticaltrapdoors.neoforge.client;

import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import com.radaeli.verticaltrapdoors.neoforge.network.AltModifierPayload;
import com.radaeli.verticaltrapdoors.neoforge.network.AltModifierState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Sends Alt key state before the matching block-use packet. */
@EventBusSubscriber(modid = VerticalTrapdoors.MOD_ID, value = Dist.CLIENT)
public final class AltModifierClient {
    private static boolean lastSentDown;

    private AltModifierClient() {
    }

    @SubscribeEvent
    public static void onAltKey(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_LEFT_ALT || event.getKey() == GLFW.GLFW_KEY_RIGHT_ALT) {
            syncState();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        syncState();
    }

    private static void syncState() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;

        if (!connected) {
            lastSentDown = false;
            return;
        }

        long window = minecraft.getWindow().getWindow();
        boolean down = minecraft.screen == null
                && (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS);
        AltModifierState.setDown(minecraft.player, down);

        if (down != lastSentDown) {
            PacketDistributor.sendToServer(new AltModifierPayload(down));
            lastSentDown = down;
        }
    }
}

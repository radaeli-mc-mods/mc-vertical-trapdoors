package com.radaeli.verticaltrapdoors.forge.client;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.forge.VerticalTrapdoorsForge;
import com.radaeli.verticaltrapdoors.forge.network.AltModifierMessage;
import com.radaeli.verticaltrapdoors.forge.network.ForgeAltNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Keeps local prediction and the server's per-player Alt state in sync. */
@Mod.EventBusSubscriber(modid = VerticalTrapdoorsForge.MOD_ID, value = Dist.CLIENT)
public final class AltModifierClient {
    private static boolean lastSentDown;

    private AltModifierClient() {}

    @SubscribeEvent
    public static void onAltKey(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_LEFT_ALT || event.getKey() == GLFW.GLFW_KEY_RIGHT_ALT) {
            syncState();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) syncState();
    }

    public static void syncState() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
        if (!connected) {
            AltModifierState.clearLocalState();
            lastSentDown = false;
            return;
        }

        long window = minecraft.getWindow().getWindow();
        boolean focused = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        boolean down = focused && minecraft.screen == null
                && (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS);
        AltModifierState.setLocalDown(minecraft.player, down);
        if (down != lastSentDown) {
            ForgeAltNetworking.CHANNEL.send(PacketDistributor.SERVER.noArg(), new AltModifierMessage(down));
            lastSentDown = down;
        }
    }
}

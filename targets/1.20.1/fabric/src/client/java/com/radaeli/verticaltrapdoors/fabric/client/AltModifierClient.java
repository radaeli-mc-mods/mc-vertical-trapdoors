package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.fabric.network.FabricAltNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;

/** Predicts Alt locally and synchronizes only changes with the connected server. */
public final class AltModifierClient {
    private static boolean lastSentDown;

    private AltModifierClient() {}

    public static void tick(Minecraft minecraft) {
        syncState(minecraft);
    }

    public static void syncState(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null
                || !ClientPlayNetworking.canSend(FabricAltNetworking.ALT_MODIFIER_ID)) {
            clear();
            return;
        }

        long window = minecraft.getWindow().getWindow();
        boolean focused = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        boolean down = focused && minecraft.screen == null
                && (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS);
        AltModifierState.setLocalDown(minecraft.player, down);

        if (down != lastSentDown) {
            FriendlyByteBuf buffer = PacketByteBufs.create();
            buffer.writeBoolean(down);
            ClientPlayNetworking.send(FabricAltNetworking.ALT_MODIFIER_ID, buffer);
            lastSentDown = down;
        }
    }

    public static void clear() {
        AltModifierState.clearLocalState();
        lastSentDown = false;
    }
}

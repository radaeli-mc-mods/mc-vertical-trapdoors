package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.client.SdlAltKeys;
import com.radaeli.verticaltrapdoors.fabric.network.FabricAltNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Predicts Alt locally and synchronizes only changes with the connected server. */
public final class AltModifierClient {
    private static boolean lastSentDown;

    private AltModifierClient() {}

    public static void tick(Minecraft minecraft) {
        syncState(minecraft);
    }

    public static void syncState(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null) {
            AltModifierState.clearLocalState();
            lastSentDown = false;
            return;
        }

        boolean focused = minecraft.getWindow().isFocused();
        boolean down = focused && minecraft.gui.screen() == null
                && SdlAltKeys.isAltDown();
        AltModifierState.setLocalDown(minecraft.player, down);

        if (down != lastSentDown && ClientPlayNetworking.canSend(FabricAltNetworking.ALT_MODIFIER_ID)) {
            ClientPlayNetworking.send(new FabricAltNetworking.AltModifierPayload(down));
            lastSentDown = down;
        }
    }

    public static void clear() {
        AltModifierState.clearLocalState();
        lastSentDown = false;
    }
}

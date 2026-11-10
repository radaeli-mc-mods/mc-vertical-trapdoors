package com.radaeli.verticaltrapdoors.neoforge.client;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.client.SdlAltKeys;
import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import com.radaeli.verticaltrapdoors.neoforge.network.AltModifierPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/** Sends Alt key state before the matching block-use packet. */
@EventBusSubscriber(modid = VerticalTrapdoors.MOD_ID, value = Dist.CLIENT)
public final class AltInputSync {
    private static boolean lastSentDown;

    private AltInputSync() {}

    @SubscribeEvent
    public static void onAltKey(InputEvent.Key event) {
        if (SdlAltKeys.isAlt(event.getKey())) syncState();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        syncState();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    public static void syncState() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
        if (!connected) {
            reset();
            return;
        }

        boolean focused = minecraft.getWindow().isFocused();
        boolean down = focused && !GameplayScreen.isOpen(minecraft) && minecraft.hasAltDown();
        AltModifierState.setLocalDown(minecraft.player, down);

        if (down != lastSentDown) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new AltModifierPayload(down));
            lastSentDown = down;
        }
    }

    private static void reset() {
        AltModifierState.clearLocalState();
        lastSentDown = false;
    }
}

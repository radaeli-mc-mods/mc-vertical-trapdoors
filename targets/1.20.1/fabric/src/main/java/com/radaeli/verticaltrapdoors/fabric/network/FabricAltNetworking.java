package com.radaeli.verticaltrapdoors.fabric.network;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.fabric.VerticalTrapdoorsFabric;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public final class FabricAltNetworking {
    public static final ResourceLocation ALT_MODIFIER_ID =
            new ResourceLocation(VerticalTrapdoorsFabric.MOD_ID, "alt_modifier");

    private FabricAltNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ALT_MODIFIER_ID, (server, player, handler, buffer, sender) -> {
            if (buffer.readableBytes() != 1) {
                return;
            }
            // Fabric owns the buffer. Capture the value before leaving the network thread.
            boolean down = buffer.readBoolean();
            server.execute(() -> {
                if (handler.isAcceptingMessages()) {
                    AltModifierState.setServerDown(player, down);
                }
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                AltModifierState.clearServer(handler.getPlayer()));
    }
}

package com.radaeli.verticaltrapdoors.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.network.chat.Component;
import com.radaeli.verticaltrapdoors.fabric.client.mixin.ClientCommonPacketListenerAccessor;

public final class FabricCompatibilityHandshakeClient {
    private static boolean receivedServerProtocol;

    private FabricCompatibilityHandshakeClient() {}

    public static void register() {
        ClientConfigurationConnectionEvents.INIT.register((handler, client) -> receivedServerProtocol = false);
        ClientConfigurationNetworking.registerGlobalReceiver(FabricCompatibilityHandshake.TYPE,
                (payload, context) -> {
                    if (payload.protocol() != FabricCompatibilityHandshake.PROTOCOL) {
                        connection(context.networkHandler()).disconnect(Component.literal(
                                "Vertical Trapdoors protocol mismatch. Update the client and server mods."));
                        return;
                    }
                    receivedServerProtocol = true;
                });
        ClientConfigurationConnectionEvents.COMPLETE.register((handler, client) -> {
            if (!receivedServerProtocol) {
                connection(handler).disconnect(Component.literal(
                        "Vertical Trapdoors must be installed on both client and server."));
            }
            receivedServerProtocol = false;
        });
        ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> receivedServerProtocol = false);
    }

    private static net.minecraft.network.Connection connection(
            net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl handler) {
        return ((ClientCommonPacketListenerAccessor) handler).verticalTrapdoors$getConnection();
    }
}

package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.fabric.network.FabricCompatibilityHandshake;
import com.radaeli.verticaltrapdoors.fabric.network.FabricCompatibilityHandshakeClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class VerticalTrapdoorsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricCompatibilityHandshakeClient.register();
        ClientTickEvents.END_CLIENT_TICK.register(AltModifierClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AltModifierClient.clear());
    }
}

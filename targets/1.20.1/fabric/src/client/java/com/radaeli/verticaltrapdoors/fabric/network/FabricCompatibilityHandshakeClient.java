package com.radaeli.verticaltrapdoors.fabric.network;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.minecraft.network.chat.Component;

public final class FabricCompatibilityHandshakeClient {
    private FabricCompatibilityHandshakeClient() {}

    public static void register() {
        ClientLoginNetworking.registerGlobalReceiver(FabricCompatibilityHandshake.CHANNEL,
                (client, handler, buffer, listeners) -> {
                    LoginConfirmation login = (LoginConfirmation) handler;
                    if (!FabricCompatibilityHandshake.matchesProtocol(buffer)) {
                        login.verticalTrapdoors$reject(FabricCompatibilityHandshake.INCOMPATIBLE_PROTOCOL);
                        return CompletableFuture.completedFuture(null);
                    }
                    login.verticalTrapdoors$confirmServer();
                    return CompletableFuture.completedFuture(FabricCompatibilityHandshake.protocolBuffer());
                });
    }

    /** The confirmation belongs to one login listener, including when connections overlap. */
    public interface LoginConfirmation {
        void verticalTrapdoors$confirmServer();
        void verticalTrapdoors$reject(Component reason);
    }
}

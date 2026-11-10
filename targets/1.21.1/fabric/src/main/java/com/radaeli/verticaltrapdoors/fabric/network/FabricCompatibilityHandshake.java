package com.radaeli.verticaltrapdoors.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.radaeli.verticaltrapdoors.AltModifierState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import io.netty.buffer.ByteBuf;

/** Requires a modded peer with the matching shared protocol before entering play. */
public final class FabricCompatibilityHandshake {
    public static final int PROTOCOL = 1;
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(
            "verticaltrapdoors", "compatibility");
    public static final CustomPacketPayload.Type<CompatibilityPayload> TYPE =
            new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<ByteBuf, CompatibilityPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(CompatibilityPayload::new, CompatibilityPayload::protocol);

    private FabricCompatibilityHandshake() {}

    public static void registerServer() {
        PayloadTypeRegistry.configurationS2C().register(TYPE, CODEC);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, TYPE)) {
                handler.disconnect(Component.literal("Vertical Trapdoors must be installed on both client and server."));
                return;
            }
            ServerConfigurationNetworking.send(handler, new CompatibilityPayload(PROTOCOL));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                AltModifierState.clearServer(handler.getPlayer()));
    }

    public record CompatibilityPayload(int protocol) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

package com.radaeli.verticaltrapdoors.fabric.network;

import com.radaeli.verticaltrapdoors.fabric.VerticalTrapdoorsFabric;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

/** Login must succeed before either peer starts exchanging block state IDs. */
public final class FabricCompatibilityHandshake {
    public static final int PROTOCOL = 1;
    public static final ResourceLocation CHANNEL =
            new ResourceLocation(VerticalTrapdoorsFabric.MOD_ID, "compatibility");
    public static final Component MISSING_MOD =
            Component.literal("Vertical Trapdoors must be installed on both client and server.");
    public static final Component INCOMPATIBLE_PROTOCOL =
            Component.literal("Vertical Trapdoors protocol mismatch. Update the client and server mods.");
    private static final Map<ServerLoginPacketListenerImpl, CompletableFuture<Component>> PENDING =
            new ConcurrentHashMap<>();

    private FabricCompatibilityHandshake() {}

    public static void registerServer() {
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buffer, sync, sender) -> {
            CompletableFuture<Component> response = PENDING.remove(handler);
            if (response != null) {
                response.complete(!understood ? MISSING_MOD : matchesProtocol(buffer) ? null : INCOMPATIBLE_PROTOCOL);
            }
        });
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) -> {
            var response = new CompletableFuture<Component>();
            PENDING.put(handler, response);
            // Fabric removes the outstanding query before invoking the receiver.
            // Start waiting before sending so a server tick cannot accept early.
            sync.waitFor(response.thenAcceptAsync(reason -> {
                if (reason != null) {
                    handler.disconnect(reason);
                }
            }, server));
            sender.sendPacket(CHANNEL, protocolBuffer());
        });
        ServerLoginConnectionEvents.DISCONNECT.register((handler, server) -> PENDING.remove(handler));
    }

    public static FriendlyByteBuf protocolBuffer() {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(PROTOCOL);
        return buffer;
    }

    public static boolean matchesProtocol(FriendlyByteBuf buffer) {
        return buffer.readableBytes() == 1 && buffer.readUnsignedByte() == PROTOCOL;
    }
}

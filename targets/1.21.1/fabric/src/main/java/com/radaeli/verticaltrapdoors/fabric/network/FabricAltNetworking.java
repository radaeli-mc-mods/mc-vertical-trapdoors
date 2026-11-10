package com.radaeli.verticaltrapdoors.fabric.network;

import com.radaeli.verticaltrapdoors.AltModifierState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class FabricAltNetworking {
    public static final ResourceLocation ALT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "verticaltrapdoors", "alt_modifier");
    public static final CustomPacketPayload.Type<AltModifierPayload> ALT_MODIFIER_TYPE =
            new CustomPacketPayload.Type<>(ALT_MODIFIER_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, AltModifierPayload> ALT_MODIFIER_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, AltModifierPayload::down, AltModifierPayload::new);

    private FabricAltNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ALT_MODIFIER_TYPE, ALT_MODIFIER_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ALT_MODIFIER_TYPE,
                (payload, context) -> context.server().execute(
                        () -> AltModifierState.setServerDown(context.player(), payload.down())));
    }

    public record AltModifierPayload(boolean down) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ALT_MODIFIER_TYPE;
        }
    }
}

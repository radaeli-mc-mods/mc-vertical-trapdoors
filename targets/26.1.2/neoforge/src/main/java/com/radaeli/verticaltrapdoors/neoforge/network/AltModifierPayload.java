package com.radaeli.verticaltrapdoors.neoforge.network;

import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import com.radaeli.verticaltrapdoors.AltModifierState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Synchronizes the client's momentary Alt key state before block placement. */
public record AltModifierPayload(boolean down) implements CustomPacketPayload {
    public static final Type<AltModifierPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(VerticalTrapdoors.MOD_ID, "alt_modifier")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AltModifierPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, AltModifierPayload::down, AltModifierPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AltModifierPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AltModifierState.setServerDown(context.player(), payload.down()));
    }
}

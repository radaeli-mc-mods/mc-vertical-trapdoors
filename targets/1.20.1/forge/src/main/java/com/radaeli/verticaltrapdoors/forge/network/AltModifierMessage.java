package com.radaeli.verticaltrapdoors.forge.network;

import com.radaeli.verticaltrapdoors.AltModifierState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** One boolean C2S update for the currently held Alt key. */
public record AltModifierMessage(boolean down) {
    public static void encode(AltModifierMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.down());
    }

    public static AltModifierMessage decode(FriendlyByteBuf buffer) {
        return new AltModifierMessage(buffer.readBoolean());
    }

    public static void handle(AltModifierMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                AltModifierState.setServerDown(context.getSender(), message.down());
            }
        });
        context.setPacketHandled(true);
    }
}

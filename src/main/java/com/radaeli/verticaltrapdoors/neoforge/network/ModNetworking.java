package com.radaeli.verticaltrapdoors.neoforge.network;

import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                AltModifierPayload.TYPE,
                AltModifierPayload.STREAM_CODEC,
                AltModifierPayload::handle
        );
    }
}

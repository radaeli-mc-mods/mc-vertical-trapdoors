package com.radaeli.verticaltrapdoors.neoforge.network;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = VerticalTrapdoors.MOD_ID)
public final class AltModifierLogoutHandler {
    private AltModifierLogoutHandler() {}

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AltModifierState.clearServer(event.getEntity());
    }
}

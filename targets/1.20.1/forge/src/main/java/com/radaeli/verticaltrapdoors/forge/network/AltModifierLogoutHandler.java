package com.radaeli.verticaltrapdoors.forge.network;

import com.radaeli.verticaltrapdoors.AltModifierState;
import com.radaeli.verticaltrapdoors.forge.VerticalTrapdoorsForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VerticalTrapdoorsForge.MOD_ID)
public final class AltModifierLogoutHandler {
    private AltModifierLogoutHandler() {}

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AltModifierState.clearServer(event.getEntity());
    }
}

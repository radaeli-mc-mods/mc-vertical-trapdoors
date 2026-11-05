package com.radaeli.verticaltrapdoors.neoforge.network;

import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks a player's momentary Alt key state on the client and server. */
@EventBusSubscriber(modid = VerticalTrapdoors.MOD_ID)
public final class AltModifierState {
    private static final Set<UUID> ALT_DOWN = ConcurrentHashMap.newKeySet();

    private AltModifierState() {
    }

    public static void setDown(Player player, boolean down) {
        if (down) {
            ALT_DOWN.add(player.getUUID());
        } else {
            ALT_DOWN.remove(player.getUUID());
        }
    }

    public static boolean isDown(Player player) {
        return ALT_DOWN.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ALT_DOWN.remove(event.getEntity().getUUID());
    }
}

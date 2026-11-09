package com.radaeli.verticaltrapdoors;

import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the predicting client's Alt state separately from each server player's state. */
public final class AltModifierState {
    private static final Set<UUID> LOCAL_DOWN = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SERVER_DOWN = ConcurrentHashMap.newKeySet();

    private AltModifierState() {}

    public static void setLocalDown(Player player, boolean down) { update(LOCAL_DOWN, player.getUUID(), down); }
    public static void setServerDown(Player player, boolean down) { update(SERVER_DOWN, player.getUUID(), down); }

    public static boolean isDown(Player player) {
        return (player.level().isClientSide ? LOCAL_DOWN : SERVER_DOWN).contains(player.getUUID());
    }

    public static void clearLocal(Player player) { LOCAL_DOWN.remove(player.getUUID()); }
    public static void clearServer(Player player) { SERVER_DOWN.remove(player.getUUID()); }
    public static void clearLocalState() { LOCAL_DOWN.clear(); }

    private static void update(Set<UUID> state, UUID player, boolean down) {
        if (down) state.add(player); else state.remove(player);
    }
}

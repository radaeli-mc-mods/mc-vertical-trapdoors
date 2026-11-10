package com.radaeli.verticaltrapdoors.neoforge.client;

import net.minecraft.client.Minecraft;

final class GameplayScreen {
    private GameplayScreen() {}

    static boolean isOpen(Minecraft minecraft) {
        return minecraft.screen != null;
    }
}

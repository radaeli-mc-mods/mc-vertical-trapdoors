package com.radaeli.verticaltrapdoors.fabric;

import com.radaeli.verticaltrapdoors.fabric.network.FabricAltNetworking;
import com.radaeli.verticaltrapdoors.fabric.network.FabricCompatibilityHandshake;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VerticalTrapdoorsFabric implements ModInitializer {
    public static final String MOD_ID = "verticaltrapdoors";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FabricAltNetworking.register();
        FabricCompatibilityHandshake.registerServer();
        LOGGER.info("{} initialized on Fabric", MOD_ID);
    }
}

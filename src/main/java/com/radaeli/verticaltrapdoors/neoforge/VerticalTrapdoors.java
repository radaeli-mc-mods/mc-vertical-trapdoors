package com.radaeli.verticaltrapdoors.neoforge;

import com.radaeli.verticaltrapdoors.neoforge.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NeoForge entry point for Vertical Trapdoors. */
@Mod(VerticalTrapdoors.MOD_ID)
public final class VerticalTrapdoors {
    public static final String MOD_ID = "verticaltrapdoors";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VerticalTrapdoors(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::register);
        LOGGER.info("{} initialized on NeoForge", MOD_ID);
    }
}

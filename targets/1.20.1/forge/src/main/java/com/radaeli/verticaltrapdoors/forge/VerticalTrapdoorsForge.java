package com.radaeli.verticaltrapdoors.forge;

import com.radaeli.verticaltrapdoors.forge.block.TrapDoorRegistrySnapshot;
import com.radaeli.verticaltrapdoors.forge.network.ForgeAltNetworking;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge entry point for Vertical Trapdoors. */
@Mod(VerticalTrapdoorsForge.MOD_ID)
public final class VerticalTrapdoorsForge {
    public static final String MOD_ID = "verticaltrapdoors";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VerticalTrapdoorsForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(TrapDoorRegistrySnapshot::collect);
        ForgeAltNetworking.register();
        LOGGER.info("{} initialized on Forge", MOD_ID);
    }
}

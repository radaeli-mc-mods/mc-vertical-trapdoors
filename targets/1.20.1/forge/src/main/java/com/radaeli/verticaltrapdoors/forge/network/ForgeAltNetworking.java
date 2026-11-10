package com.radaeli.verticaltrapdoors.forge.network;

import com.radaeli.verticaltrapdoors.forge.VerticalTrapdoorsForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Required protocol channel used to synchronize a client's Alt modifier. */
public final class ForgeAltNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VerticalTrapdoorsForge.MOD_ID, "alt_modifier"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static boolean registered;

    private ForgeAltNetworking() {}

    public static synchronized void register() {
        if (registered) return;
        CHANNEL.registerMessage(0, AltModifierMessage.class,
                AltModifierMessage::encode, AltModifierMessage::decode, AltModifierMessage::handle);
        registered = true;
    }
}

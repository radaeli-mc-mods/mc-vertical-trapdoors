package com.radaeli.verticaltrapdoors.neoforge.block;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Captures registered trapdoors before the client's first model bake. */
public final class TrapDoorRegistrySnapshot {
    private static volatile Map<ResourceLocation, TrapDoorBlock> trapdoors = Map.of();

    private TrapDoorRegistrySnapshot() {}

    public static void collect(FMLCommonSetupEvent event) {
        trapdoors = BuiltInRegistries.BLOCK.stream()
                .filter(TrapDoorBlock.class::isInstance)
                .map(TrapDoorBlock.class::cast)
                .collect(Collectors.toUnmodifiableMap(BuiltInRegistries.BLOCK::getKey, Function.identity()));
    }

    public static Map<ResourceLocation, TrapDoorBlock> getTrapdoors() {
        return trapdoors;
    }
}

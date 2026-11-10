package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Replaces only the vertical state models, keeping a snapshot of baked source models. */
public final class TrapDoorModelInstaller {
    private TrapDoorModelInstaller() {}

    public static void install(Map<BlockState, BakedModel> models) {
        Map<BlockState, BakedModel> originals = Map.copyOf(models);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof TrapDoorBlock trapdoor)) {
                continue;
            }
            BlockState defaultState = trapdoor.defaultBlockState();
            if (!defaultState.hasProperty(TrapDoorProperties.VERTICAL)
                    || !defaultState.hasProperty(TrapDoorProperties.REVERSED)) {
                continue;
            }
            installTrapdoor(models, originals, BuiltInRegistries.BLOCK.getKey(block), trapdoor);
        }
    }

    private static void installTrapdoor(Map<BlockState, BakedModel> models,
            Map<BlockState, BakedModel> originals, ResourceLocation id, TrapDoorBlock trapdoor) {
        for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
            if (!state.getValue(TrapDoorProperties.VERTICAL)) {
                continue;
            }
            BlockState sourceState = TrapDoorOrientation.modelState(state);
            BakedModel source = originals.get(sourceState);
            if (source == null) {
                throw new IllegalStateException("Missing original trapdoor model: " + id + " " + sourceState);
            }
            models.put(state, new RotatedFabricBakedModel(source, sourceState, state));
        }
    }
}

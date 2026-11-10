package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Replaces vertical state models after all Fabric model modifiers have run. */
public final class TrapDoorModelInstaller {
    private TrapDoorModelInstaller() {}

    public static void install(Map<BlockState, BlockStateModel> models) {
        Map<BlockState, BlockStateModel> originals = Map.copyOf(models);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof TrapDoorBlock trapdoor)) continue;
            BlockState defaultState = trapdoor.defaultBlockState();
            if (!defaultState.hasProperty(TrapDoorProperties.VERTICAL)
                    || !defaultState.hasProperty(TrapDoorProperties.REVERSED)) continue;

            for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
                if (!state.getValue(TrapDoorProperties.VERTICAL)) continue;
                BlockState sourceState = TrapDoorOrientation.modelState(state);
                BlockStateModel source = originals.get(sourceState);
                if (source == null) {
                    throw new IllegalStateException("Missing original trapdoor model for " + sourceState);
                }
                models.put(state, new RotatedFabricBlockStateModel(source, sourceState, state));
            }
        }
    }
}

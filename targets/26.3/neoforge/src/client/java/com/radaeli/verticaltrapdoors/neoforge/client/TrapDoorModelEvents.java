package com.radaeli.verticaltrapdoors.neoforge.client;

import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import com.radaeli.verticaltrapdoors.neoforge.block.TrapDoorRegistrySnapshot;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Replaces only vertical block-state models, retaining each original model as the source. */
@EventBusSubscriber(modid = VerticalTrapdoors.MOD_ID, value = Dist.CLIENT)
public final class TrapDoorModelEvents {
    private TrapDoorModelEvents() {}

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        applyModels(event.getBakingResult(), TrapDoorRegistrySnapshot.getTrapdoors());
    }

    static void applyModels(ModelBakery.BakingResult result, Map<Identifier, TrapDoorBlock> trapdoors) {
        applyModels(result.blockStateModels(), trapdoors);
    }

    static void applyModels(Map<BlockState, BlockStateModel> models, Map<Identifier, TrapDoorBlock> trapdoors) {
        Map<BlockState, BlockStateModel> originals = Map.copyOf(models);
        trapdoors.values().forEach(trapdoor -> {
            BlockState defaultState = trapdoor.defaultBlockState();
            if (!defaultState.hasProperty(TrapDoorProperties.VERTICAL)
                    || !defaultState.hasProperty(TrapDoorProperties.REVERSED)) return;
            for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
                if (!state.getValue(TrapDoorProperties.VERTICAL)) continue;
                BlockState sourceState = TrapDoorOrientation.modelState(state);
                BlockStateModel source = originals.get(sourceState);
                if (source == null) throw new IllegalStateException("Missing original trapdoor model: " + sourceState);
                models.put(state, new RotatedTrapDoorBlockStateModel(source, sourceState, state));
            }
        });
    }
}

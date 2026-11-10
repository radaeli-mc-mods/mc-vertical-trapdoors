package com.radaeli.verticaltrapdoors.forge.client;

import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.forge.VerticalTrapdoorsForge;
import com.radaeli.verticaltrapdoors.forge.block.TrapDoorRegistrySnapshot;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds upright model variants based on an immutable snapshot of original bake results. */
@Mod.EventBusSubscriber(modid = VerticalTrapdoorsForge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TrapDoorModelEvents {
    private TrapDoorModelEvents() {}

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        applyModels(event.getModels(), TrapDoorRegistrySnapshot.getTrapdoors());
    }

    static void applyModels(Map<ResourceLocation, BakedModel> models,
            Map<ResourceLocation, TrapDoorBlock> trapdoors) {
        Map<ResourceLocation, BakedModel> originals = Map.copyOf(models);
        trapdoors.forEach((id, trapdoor) -> {
            BlockState defaultState = trapdoor.defaultBlockState();
            if (!defaultState.hasProperty(TrapDoorProperties.VERTICAL)
                    || !defaultState.hasProperty(TrapDoorProperties.REVERSED)) return;
            for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
                if (!state.getValue(TrapDoorProperties.VERTICAL)) continue;
                BlockState sourceState = TrapDoorOrientation.modelState(state);
                ResourceLocation modelId = BlockModelShaper.stateToModelLocation(id, sourceState);
                BakedModel source = originals.get(modelId);
                if (source == null) throw new IllegalStateException("Missing original trapdoor model: " + modelId);
                models.put(BlockModelShaper.stateToModelLocation(id, state),
                        new RotatedForgeBakedModel(source, sourceState, state));
            }
        });
    }
}

package com.radaeli.verticaltrapdoors.neoforge.client;

import com.radaeli.verticaltrapdoors.neoforge.VerticalTrapdoors;
import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.neoforge.block.TrapDoorRegistrySnapshot;
import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Builds extra model variants from the original per-state baking results. */
@EventBusSubscriber(modid = VerticalTrapdoors.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TrapDoorModelEvents {
    private TrapDoorModelEvents() {}

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        applyModels(event.getModels(), TrapDoorRegistrySnapshot.getTrapdoors());
    }

    static void applyModels(Map<ModelResourceLocation, BakedModel> models,
            Map<ResourceLocation, TrapDoorBlock> trapdoors) {
        // Map.Entry snapshots retain live values when the original map is overwritten.
        // Keep the original models themselves, including across opposite-facing lookups.
        Map<ModelResourceLocation, BakedModel> originals = Map.copyOf(models);
        trapdoors.forEach((id, trapdoor) -> {
            if (!trapdoor.defaultBlockState().hasProperty(TrapDoorProperties.VERTICAL)
                    || !trapdoor.defaultBlockState().hasProperty(TrapDoorProperties.REVERSED)) {
                return;
            }
            for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
                if (!state.getValue(TrapDoorProperties.VERTICAL)) {
                    continue;
                }
                BlockState sourceState = TrapDoorOrientation.modelState(state);
                BakedModel source = originals.get(BlockModelShaper.stateToModelLocation(id, sourceState));
                if (source == null) {
                    throw new IllegalStateException("Missing original trapdoor model: "
                            + BlockModelShaper.stateToModelLocation(id, sourceState));
                }
                models.put(BlockModelShaper.stateToModelLocation(id, state),
                        new RotatedTrapDoorBakedModel(source, sourceState, state));
            }
        });
    }
}

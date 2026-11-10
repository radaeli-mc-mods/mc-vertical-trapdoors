package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Replaces only the vertical state models, keeping a snapshot of baked source models. */
public final class TrapDoorModelInstaller {
    private TrapDoorModelInstaller() {}

    public static void install(Map<ResourceLocation, BakedModel> models) {
        Map<ResourceLocation, BakedModel> originals = Map.copyOf(models);
        BakedModel missing = originals.get(ModelBakery.MISSING_MODEL_LOCATION);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof TrapDoorBlock trapdoor)) {
                continue;
            }
            BlockState defaultState = trapdoor.defaultBlockState();
            if (!defaultState.hasProperty(TrapDoorProperties.VERTICAL)
                    || !defaultState.hasProperty(TrapDoorProperties.REVERSED)) {
                continue;
            }
            installTrapdoor(models, originals, missing, BuiltInRegistries.BLOCK.getKey(block), trapdoor);
        }
    }

    private static void installTrapdoor(Map<ResourceLocation, BakedModel> models,
            Map<ResourceLocation, BakedModel> originals, BakedModel missing, ResourceLocation id, TrapDoorBlock trapdoor) {
        for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
            if (!state.getValue(TrapDoorProperties.VERTICAL)) {
                continue;
            }
            BlockState sourceState = TrapDoorOrientation.modelState(state);
            ModelResourceLocation sourceKey = BlockModelShaper.stateToModelLocation(id, sourceState);
            BakedModel source = originals.get(sourceKey);
            // Keep vanilla's missing-model behavior when a pack omits the source.
            if (source != null && source != missing) {
                ModelResourceLocation placedKey = BlockModelShaper.stateToModelLocation(id, state);
                models.put(placedKey, new RotatedFabricBakedModel(source, sourceState, state));
            }
        }
    }
}

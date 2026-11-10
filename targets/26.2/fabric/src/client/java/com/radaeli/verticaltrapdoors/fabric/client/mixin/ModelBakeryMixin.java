package com.radaeli.verticaltrapdoors.fabric.client.mixin;

import com.radaeli.verticaltrapdoors.fabric.client.TrapDoorModelInstaller;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after Fabric's per-model bake modifiers and before vanilla snapshots the dispatch cache. */
@Mixin(ModelManager.class)
public abstract class ModelBakeryMixin {
    @Inject(method = "createBlockStateToModelDispatch", at = @At("HEAD"))
    private static void verticalTrapdoors$installVerticalModels(Map<BlockState, BlockStateModel> models,
            BlockStateModel missingModel, CallbackInfoReturnable<Map<BlockState, BlockStateModel>> callback) {
        TrapDoorModelInstaller.install(models);
    }
}

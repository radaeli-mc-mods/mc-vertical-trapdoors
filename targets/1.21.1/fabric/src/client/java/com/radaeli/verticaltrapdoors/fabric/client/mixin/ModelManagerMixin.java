package com.radaeli.verticaltrapdoors.fabric.client.mixin;

import com.radaeli.verticaltrapdoors.fabric.client.TrapDoorModelInstaller;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
    @Inject(method = "loadModels", at = @At("RETURN"))
    private void verticalTrapdoors$installVerticalModels(CallbackInfoReturnable<?> callback) {
        ModelManagerReloadStateAccessor reloadState =
                (ModelManagerReloadStateAccessor) callback.getReturnValue();
        Map<BlockState, BakedModel> wrapped = new HashMap<>(reloadState.verticalTrapdoors$getModelCache());
        TrapDoorModelInstaller.install(wrapped);
        reloadState.verticalTrapdoors$setModelCache(wrapped);
    }
}

package com.radaeli.verticaltrapdoors.fabric.client.mixin;

import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.fabric.client.TrapDoorModelInstaller;
import java.util.Map;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
    @Inject(method = "loadModels", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;"))
    private void verticalTrapdoors$installVerticalModels(ProfilerFiller profiler, Map<?, ?> atlases,
            ModelBakery bakery, CallbackInfoReturnable<?> callback) {
        // Fabric has finished baking and modifying the models. Both the state cache
        // and the model registry are still to be published from this map.
        TrapDoorModelInstaller.install(bakery.getBakedTopLevelModels());
    }

    @Inject(method = "requiresRender", at = @At("HEAD"), cancellable = true)
    private void verticalTrapdoors$refreshVerticalPanel(BlockState previous, BlockState current,
            CallbackInfoReturnable<Boolean> callback) {
        // Vanilla's model groups do not account for the wrapped geometry.
        if (previous != current && (verticalTrapdoors$isVertical(previous) || verticalTrapdoors$isVertical(current))) {
            callback.setReturnValue(true);
        }
    }

    @Unique
    private static boolean verticalTrapdoors$isVertical(BlockState state) {
        return state.hasProperty(TrapDoorProperties.VERTICAL) && state.getValue(TrapDoorProperties.VERTICAL);
    }
}

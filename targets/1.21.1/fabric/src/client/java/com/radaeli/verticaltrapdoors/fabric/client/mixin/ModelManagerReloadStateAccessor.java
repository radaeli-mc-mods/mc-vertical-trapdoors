package com.radaeli.verticaltrapdoors.fabric.client.mixin;

import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.resources.model.ModelManager$ReloadState")
public interface ModelManagerReloadStateAccessor {
    @Accessor("modelCache")
    Map<BlockState, BakedModel> verticalTrapdoors$getModelCache();

    @Mutable
    @Accessor("modelCache")
    void verticalTrapdoors$setModelCache(Map<BlockState, BakedModel> modelCache);
}

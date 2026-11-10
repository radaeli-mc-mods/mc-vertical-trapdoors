package com.radaeli.verticaltrapdoors.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Block.class)
public interface BlockDefaultStateInvoker {
    @Invoker("registerDefaultState")
    void verticalTrapdoors$registerDefaultState(BlockState state);
}

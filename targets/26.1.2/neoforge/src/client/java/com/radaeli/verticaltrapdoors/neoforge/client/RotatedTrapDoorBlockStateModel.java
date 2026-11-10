package com.radaeli.verticaltrapdoors.neoforge.client;

import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.joml.Matrix4f;

/** Delegates state-dependent model selection to the matching vanilla trapdoor state. */
final class RotatedTrapDoorBlockStateModel implements DynamicBlockStateModel {
    private final BlockStateModel original;
    private final BlockState sourceState;
    private final Matrix4f transform;

    RotatedTrapDoorBlockStateModel(BlockStateModel original, BlockState sourceState, BlockState placedState) {
        this.original = original;
        this.sourceState = sourceState;
        this.transform = PanelTransform.matrix(sourceState, placedState);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            List<BlockStateModelPart> output) {
        List<BlockStateModelPart> sourceParts = new java.util.ArrayList<>();
        original.collectParts(level, pos, sourceState, random, sourceParts);
        sourceParts.stream().map(part -> new RotatedBlockStateModelPart(part, transform)).forEach(output::add);
    }

    @Override public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return original.particleMaterial();
    }

    @Override public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial(
            BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return original.particleMaterial(level, pos, sourceState);
    }

    @Override public int materialFlags() { return original.materialFlags(); }

    @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return original.materialFlags(level, pos, sourceState);
    }

    @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(sourceState, original.createGeometryKey(level, pos, sourceState, random));
    }

    private record GeometryKey(BlockState sourceState, Object originalKey) {}
}

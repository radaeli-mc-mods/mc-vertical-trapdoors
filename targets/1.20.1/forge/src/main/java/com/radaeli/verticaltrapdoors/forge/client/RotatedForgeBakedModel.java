package com.radaeli.verticaltrapdoors.forge.client;

import com.mojang.math.Transformation;
import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** Rigidly moves a trapdoor panel while retaining its source model data. */
final class RotatedForgeBakedModel extends BakedModelWrapper<BakedModel> {
    private final BlockState sourceState;
    private final Matrix4f matrix;
    private final IQuadTransformer transformer;

    RotatedForgeBakedModel(BakedModel original, BlockState sourceState, BlockState placedState) {
        super(original);
        this.sourceState = sourceState;
        matrix = PanelTransform.matrix(sourceState, placedState);
        transformer = QuadTransformers.applying(new Transformation(matrix));
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        return originalModel.getModelData(level, pos, sourceState, data);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return originalModel.getRenderTypes(sourceState, random, data);
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state) {
        return originalModel.useAmbientOcclusion(sourceState);
    }

    @Override
    public boolean useAmbientOcclusion(BlockState state, RenderType renderType) {
        return originalModel.useAmbientOcclusion(sourceState, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
            ModelData data, @Nullable RenderType renderType) {
        if (side != null) return List.of();
        List<BakedQuad> result = new ArrayList<>();
        long seed = random.nextLong();
        random.setSeed(seed);
        append(result, originalModel.getQuads(sourceState, null, random, data, renderType));
        for (Direction sourceSide : Direction.values()) {
            random.setSeed(seed);
            append(result, originalModel.getQuads(sourceState, sourceSide, random, data, renderType));
        }
        return List.copyOf(result);
    }

    private void append(List<BakedQuad> result, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            BakedQuad transformed = transformer.process(quad);
            result.add(new BakedQuad(transformed.getVertices(), quad.getTintIndex(),
                    Direction.rotate(matrix, quad.getDirection()), quad.getSprite(), quad.isShade(),
                    quad.hasAmbientOcclusion()));
        }
    }
}

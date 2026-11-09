package com.radaeli.verticaltrapdoors.neoforge.client;

import com.mojang.math.Transformation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/** Moves a single textured panel rigidly, preserving UVs and both physical faces. */
final class RotatedTrapDoorBakedModel extends BakedModelWrapper<BakedModel> {
    private final BlockState sourceState;
    private final Matrix4f matrix;
    private final IQuadTransformer transformer;

    RotatedTrapDoorBakedModel(BakedModel originalModel, BlockState sourceState, BlockState placedState) {
        super(originalModel);
        this.sourceState = sourceState;
        matrix = PanelTransform.matrix(sourceState, placedState);
        transformer = QuadTransformers.applying(new Transformation(matrix));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
            RandomSource random, ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        // Rotation/translation can move originally culled faces into the block interior.
        // Return all transformed faces as unculled so neither side disappears incorrectly.
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
                    Direction.rotate(matrix, quad.getDirection()), quad.getSprite(),
                    quad.isShade(), quad.hasAmbientOcclusion()));
        }
    }
}

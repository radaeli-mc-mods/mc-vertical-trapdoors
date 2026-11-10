package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/** Moves the source panel while retaining its original quad attributes and model behavior. */
public final class RotatedFabricBakedModel implements BakedModel, FabricBakedModel {
    private final BakedModel original;
    private final BlockState sourceState;
    private final Matrix4f matrix;

    RotatedFabricBakedModel(BakedModel original, BlockState sourceState, BlockState placedState) {
        this.original = original;
        this.sourceState = sourceState;
        this.matrix = PanelTransform.matrix(sourceState, placedState);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        if (side != null) {
            return List.of();
        }
        List<BakedQuad> result = new ArrayList<>();
        long seed = random.nextLong();
        random.setSeed(seed);
        append(result, original.getQuads(sourceState, null, random));
        for (Direction sourceSide : Direction.values()) {
            random.setSeed(seed);
            append(result, original.getQuads(sourceState, sourceSide, random));
        }
        return List.copyOf(result);
    }

    private void append(List<BakedQuad> result, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices().clone();
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * 8;
                Vector3f position = new Vector3f(Float.intBitsToFloat(vertices[offset]),
                        Float.intBitsToFloat(vertices[offset + 1]), Float.intBitsToFloat(vertices[offset + 2]));
                matrix.transformPosition(position);
                vertices[offset] = Float.floatToRawIntBits(position.x);
                vertices[offset + 1] = Float.floatToRawIntBits(position.y);
                vertices[offset + 2] = Float.floatToRawIntBits(position.z);
                int packedNormal = vertices[offset + 7];
                Vector3f normal = new Vector3f((byte) packedNormal / 127.0f,
                        (byte) (packedNormal >> 8) / 127.0f, (byte) (packedNormal >> 16) / 127.0f);
                matrix.transformDirection(normal);
                vertices[offset + 7] = (packedNormal & 0xff000000) | (Math.round(normal.x * 127.0f) & 0xff)
                        | ((Math.round(normal.y * 127.0f) & 0xff) << 8)
                        | ((Math.round(normal.z * 127.0f) & 0xff) << 16);
            }
            result.add(new BakedQuad(vertices, quad.getTintIndex(), Direction.rotate(matrix, quad.getDirection()),
                    quad.getSprite(), quad.isShade()));
        }
    }

    @Override
    @SuppressWarnings("removal") // Fabric 1.20.1's consumer accepts an explicit source state.
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos,
            Supplier<RandomSource> randomSupplier, RenderContext context) {
        FabricBakedModel fabricModel = (FabricBakedModel) original;
        context.pushTransform(this::transformQuad);
        try {
            if (fabricModel.isVanillaAdapter()) {
                // Use the renderer's vanilla encoder so shade, tint, AO and seeds
                // are handled exactly as on the source model.
                context.bakedModelConsumer().accept(original, sourceState);
            } else {
                fabricModel.emitBlockQuads(view, sourceState, pos, randomSupplier, context);
            }
        } finally {
            context.popTransform();
        }
    }

    private boolean transformQuad(MutableQuadView quad) {
        Direction nominalFace = quad.nominalFace();
        Vector3f vector = new Vector3f();
        for (int vertex = 0; vertex < 4; vertex++) {
            vector.set(quad.x(vertex), quad.y(vertex), quad.z(vertex));
            matrix.transformPosition(vector);
            quad.pos(vertex, vector.x, vector.y, vector.z);
            if (quad.hasNormal(vertex)) {
                Vector3f normal = vector.set(quad.normalX(vertex), quad.normalY(vertex), quad.normalZ(vertex));
                matrix.transformDirection(normal);
                quad.normal(vertex, normal.x, normal.y, normal.z);
            }
        }
        // The source cull face may now be inside the block.
        quad.cullFace(null).nominalFace(nominalFace == null ? null : Direction.rotate(matrix, nominalFace));
        return true;
    }

    @Override public boolean isVanillaAdapter() { return false; }
    @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return original.isGui3d(); }
    @Override public boolean usesBlockLight() { return original.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return original.isCustomRenderer(); }
    @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return original.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return original.getOverrides(); }
}

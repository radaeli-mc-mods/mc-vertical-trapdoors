package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
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
                matrix.transformDirection(normal).normalize();
                vertices[offset + 7] = ((int) (normal.x * 127.0f) & 0xff)
                        | (((int) (normal.y * 127.0f) & 0xff) << 8)
                        | (((int) (normal.z * 127.0f) & 0xff) << 16);
            }
            result.add(new BakedQuad(vertices, quad.getTintIndex(), Direction.rotate(matrix, quad.getDirection()),
                    quad.getSprite(), quad.isShade()));
        }
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter view, BlockState state, BlockPos pos,
            Supplier<RandomSource> randomSupplier, RenderContext context) {
        FabricBakedModel fabricModel = (FabricBakedModel) original;
        if (!fabricModel.isVanillaAdapter()) {
            pushPanelTransform(context);
            try {
                fabricModel.emitBlockQuads(view, sourceState, pos, randomSupplier, context);
            } finally {
                context.popTransform();
            }
            return;
        }

        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) {
            return;
        }
        pushPanelTransform(context);
        QuadEmitter emitter = context.getEmitter();
        RandomSource random = randomSupplier.get();
        long seed = random.nextLong();
        try {
            emitVanillaQuads(emitter, random, seed, null, renderer);
            for (Direction face : Direction.values()) {
                emitVanillaQuads(emitter, random, seed, face, renderer);
            }
        } finally {
            context.popTransform();
        }
    }

    private void pushPanelTransform(RenderContext context) {
        context.pushTransform(quad -> {
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3f position = new Vector3f(quad.x(vertex), quad.y(vertex), quad.z(vertex));
                matrix.transformPosition(position);
                quad.pos(vertex, position.x, position.y, position.z);
                Vector3f normal = new Vector3f(quad.normalX(vertex), quad.normalY(vertex), quad.normalZ(vertex));
                matrix.transformDirection(normal).normalize();
                quad.normal(vertex, normal.x, normal.y, normal.z);
            }
            quad.cullFace(null);
            return true;
        });
    }

    private void emitVanillaQuads(QuadEmitter emitter, RandomSource random, long seed,
            @Nullable Direction face, Renderer renderer) {
        random.setSeed(seed);
        for (BakedQuad quad : original.getQuads(sourceState, face, random)) {
            int tint = quad.getTintIndex();
            emitter.fromVanilla(quad, renderer.materialFinder().find(), null);
            emitter.colorIndex(tint).emit();
        }
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

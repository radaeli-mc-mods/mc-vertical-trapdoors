package com.radaeli.verticaltrapdoors.fabric.client;

import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModelPart;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Rotates block model output while preserving source-state selection and quad attributes. */
final class RotatedFabricBlockStateModel implements BlockStateModel {
    private final BlockStateModel original;
    private final BlockState sourceState;
    private final Matrix4f transform;
    private final Matrix3f normalTransform;

    RotatedFabricBlockStateModel(BlockStateModel original, BlockState sourceState, BlockState placedState) {
        this.original = original;
        this.sourceState = sourceState;
        this.transform = PanelTransform.matrix(sourceState, placedState);
        this.normalTransform = new Matrix3f(transform);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        List<BlockStateModelPart> sourceParts = new ArrayList<>();
        original.collectParts(random, sourceParts);
        sourceParts.stream().map(part -> new RotatedPart(part, transform, normalTransform)).forEach(output::add);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, Predicate<@Nullable Direction> cullTest) {
        emitter.pushTransform(this::transformQuad);
        try {
            // Moved faces no longer line up with their source cull faces, so retain all quads.
            original.emitQuads(emitter, level, pos, sourceState, random, ignored -> false);
        } finally {
            emitter.popTransform();
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return new GeometryKey(sourceState, original.createGeometryKey(level, pos, sourceState, random));
    }

    @Override public Material.Baked particleMaterial() { return original.particleMaterial(); }
    @Override public int materialFlags() { return original.materialFlags(); }
    @Override public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return original.particleMaterial(level, pos, sourceState);
    }
    @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return original.materialFlags(level, pos, sourceState, random);
    }

    private boolean transformQuad(net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView quad) {
        for (int vertex = 0; vertex < 4; vertex++) {
            Vector3f position = new Vector3f(quad.x(vertex), quad.y(vertex), quad.z(vertex));
            transform.transformPosition(position);
            quad.pos(vertex, position.x, position.y, position.z);
            if (quad.hasNormal(vertex)) {
                Vector3f normal = new Vector3f(quad.normalX(vertex), quad.normalY(vertex), quad.normalZ(vertex));
                normalTransform.transform(normal).normalize();
                quad.normal(vertex, normal.x, normal.y, normal.z);
            }
        }
        quad.cullFace(null);
        Direction nominalFace = quad.nominalFace();
        if (nominalFace != null) quad.nominalFace(rotate(nominalFace));
        return true;
    }

    private Direction rotate(Direction direction) {
        Vector3f normal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        transform.transformDirection(normal).normalize();
        return Direction.getNearest(Math.round(normal.x), Math.round(normal.y), Math.round(normal.z), Direction.UP);
    }

    private record GeometryKey(BlockState source, Object originalKey) {}

    private static final class RotatedPart implements BlockStateModelPart, FabricBlockStateModelPart {
        private final BlockStateModelPart original;
        private final Matrix4f transform;
        private final Matrix3f normalTransform;

        private RotatedPart(BlockStateModelPart original, Matrix4f transform, Matrix3f normalTransform) {
            this.original = original;
            this.transform = new Matrix4f(transform);
            this.normalTransform = new Matrix3f(normalTransform);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction != null) return List.of();
            List<BakedQuad> result = new ArrayList<>();
            append(result, original.getQuads(null));
            for (Direction face : Direction.values()) append(result, original.getQuads(face));
            return List.copyOf(result);
        }

        @Override
        public void emitQuads(QuadEmitter emitter, Predicate<@Nullable Direction> cullTest) {
            emitter.pushTransform(quad -> {
                for (int vertex = 0; vertex < 4; vertex++) {
                    Vector3f position = new Vector3f(quad.x(vertex), quad.y(vertex), quad.z(vertex));
                    transform.transformPosition(position);
                    quad.pos(vertex, position.x, position.y, position.z);
                    if (quad.hasNormal(vertex)) {
                        Vector3f normal = new Vector3f(quad.normalX(vertex), quad.normalY(vertex), quad.normalZ(vertex));
                        normalTransform.transform(normal).normalize();
                        quad.normal(vertex, normal.x, normal.y, normal.z);
                    }
                }
                quad.cullFace(null);
                return true;
            });
            try {
                ((FabricBlockStateModelPart) original).emitQuads(emitter, ignored -> false);
            } finally {
                emitter.popTransform();
            }
        }

        private void append(List<BakedQuad> output, List<BakedQuad> quads) {
            for (BakedQuad quad : quads) output.add(transform(quad));
        }

        private BakedQuad transform(BakedQuad quad) {
            return new BakedQuad(position(quad.position0()), position(quad.position1()), position(quad.position2()),
                    position(quad.position3()), quad.packedUV0(), quad.packedUV1(), quad.packedUV2(),
                    quad.packedUV3(), rotate(quad.direction()), quad.materialInfo());
        }

        private Vector3f position(org.joml.Vector3fc position) {
            return transform.transformPosition(new Vector3f(position));
        }

        private Direction rotate(Direction direction) {
            Vector3f normal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            transform.transformDirection(normal).normalize();
            return Direction.getNearest(Math.round(normal.x), Math.round(normal.y), Math.round(normal.z), Direction.UP);
        }

        @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
        @Override public Material.Baked particleMaterial() { return original.particleMaterial(); }
        @Override public int materialFlags() { return original.materialFlags(); }
    }
}

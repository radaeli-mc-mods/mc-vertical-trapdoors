package com.radaeli.verticaltrapdoors.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrapDoorModelRegressionTest {
    private static final Identifier VANILLA_ID = Identifier.withDefaultNamespace("acacia_trapdoor");
    private static final Identifier THIRD_PARTY_ID = Identifier.fromNamespaceAndPath("third_party", "custom_trapdoor");
    private static final BakedQuad.MaterialInfo MATERIAL = new BakedQuad.MaterialInfo(null,
            ChunkSectionLayer.CUTOUT, null, null, null, 3, null, 5, true);
    private static final BakedQuad.MaterialInfo RELOADED_MATERIAL = new BakedQuad.MaterialInfo(null,
            ChunkSectionLayer.TRANSLUCENT, null, null, null, 0, null, 0, false);
    private static final Material.Baked RELOADED_PARTICLE = new Material.Baked(null, true);
    private static TrapDoorBlock trapdoor;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        trapdoor = (TrapDoorBlock) Blocks.ACACIA_TRAPDOOR;
        assertTrue(trapdoor.defaultBlockState().hasProperty(TrapDoorProperties.VERTICAL),
                "The regression test must load the production trapdoor mixin");
    }

    @Test
    void transformedModelMatchesTheVerticalCollisionGeometry() {
        Map<BlockState, BlockStateModel> models = modelsFor(trapdoor, false);
        TrapDoorModelEvents.applyModels(models, Map.of(VANILLA_ID, trapdoor));

        for (BlockState state : verticalStates(trapdoor)) {
            List<BlockStateModelPart> parts = collect(models.get(state), state);
            List<BakedQuad> quads = parts.getFirst().getQuads(null);
            assertBox(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds(), bounds(quads));
        }
    }

    @Test
    void dynamicSourceModelsReceiveTheVanillaTrapdoorState() {
        Map<BlockState, BlockStateModel> models = modelsFor(trapdoor, true);
        TrapDoorModelEvents.applyModels(models, Map.of(VANILLA_ID, trapdoor));

        for (BlockState state : verticalStates(trapdoor)) {
            BlockState expectedSource = TrapDoorOrientation.modelState(state);
            SourceModel source = (SourceModel) models.get(expectedSource);
            List<BlockStateModelPart> parts = collect(models.get(state), state);
            assertSame(expectedSource, source.lastState);
            assertEquals(1, parts.size());
        }
    }

    @Test
    void transformedQuadsRetainBakedAndParticleMaterial() {
        Map<BlockState, BlockStateModel> models = modelsFor(trapdoor, false);
        TrapDoorModelEvents.applyModels(models, Map.of(VANILLA_ID, trapdoor));

        BlockState state = verticalStates(trapdoor).getFirst();
        BlockStateModelPart part = collect(models.get(state), state).getFirst();
        BakedQuad quad = part.getQuads(null).getFirst();
        assertSame(MATERIAL, quad.materialInfo());
        assertEquals(3, part.materialFlags());
        assertNull(part.particleMaterial());
        assertEquals(ChunkSectionLayer.CUTOUT, quad.materialInfo().layer());
    }

    @Test
    void rebakeUsesTheReplacementMaterialsAndKeepsAllSixFacesVisible() {
        BlockState state = verticalStates(trapdoor).getFirst();
        BlockState sourceState = TrapDoorOrientation.modelState(state);
        Map<BlockState, BlockStateModel> firstBake = modelsFor(trapdoor, true);
        TrapDoorModelEvents.applyModels(firstBake, Map.of(VANILLA_ID, trapdoor));

        Map<BlockState, BlockStateModel> secondBake = modelsFor(trapdoor, true);
        secondBake.put(sourceState, new SourceModel(sourceState, true, RELOADED_MATERIAL, RELOADED_PARTICLE));
        TrapDoorModelEvents.applyModels(secondBake, Map.of(VANILLA_ID, trapdoor));

        BlockStateModel reloaded = secondBake.get(state);
        BlockStateModelPart part = collect(reloaded, state).getFirst();
        List<BakedQuad> quads = part.getQuads(null);
        assertEquals(6, quads.size());
        quads.forEach(quad -> assertSame(RELOADED_MATERIAL, quad.materialInfo()));
        for (Direction side : Direction.values()) assertTrue(part.getQuads(side).isEmpty());
        assertSame(RELOADED_PARTICLE, part.particleMaterial());
        assertSame(RELOADED_PARTICLE, reloaded.particleMaterial());
        assertSame(RELOADED_PARTICLE, reloaded.particleMaterial(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state));
        assertSame(MATERIAL, collect(firstBake.get(state), state).getFirst().getQuads(null).getFirst().materialInfo());
    }

    @Test
    void geometryKeyDistinguishesOpenAndClosedAndRespectsUncacheableSources() {
        BlockState closed = verticalStates(trapdoor).getFirst().setValue(TrapDoorBlock.OPEN, false);
        BlockState open = closed.setValue(TrapDoorBlock.OPEN, true);
        BlockState sourceState = TrapDoorOrientation.modelState(closed);
        assertSame(sourceState, TrapDoorOrientation.modelState(open));
        Map<BlockState, BlockStateModel> models = modelsFor(trapdoor, true);
        TrapDoorModelEvents.applyModels(models, Map.of(VANILLA_ID, trapdoor));

        assertNull(geometryKey(models.get(closed), closed));
        models.put(sourceState, new SourceModel(sourceState, true) {
            @Override public Object createGeometryKey(
                    BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
                assertSame(sourceState, state);
                return sourceState;
            }
        });
        TrapDoorModelEvents.applyModels(models, Map.of(VANILLA_ID, trapdoor));
        Object closedKey = geometryKey(models.get(closed), closed);
        assertEquals(closedKey, geometryKey(models.get(closed), closed));
        assertNotEquals(closedKey, geometryKey(models.get(open), open));
    }

    private static Object geometryKey(BlockStateModel model, BlockState state) {
        return model.createGeometryKey(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(42));
    }

    @Test
    void onlyRegisteredTrapdoorModelsAreWrapped() {
        Map<BlockState, BlockStateModel> models = modelsFor(trapdoor, false);
        BlockState vanillaState = trapdoor.defaultBlockState();
        BlockStateModel original = models.get(vanillaState);
        TrapDoorModelEvents.applyModels(models, Map.of(VANILLA_ID, trapdoor, THIRD_PARTY_ID, trapdoor));
        assertSame(original, models.get(vanillaState));
    }

    private static Map<BlockState, BlockStateModel> modelsFor(TrapDoorBlock block, boolean dynamic) {
        Map<BlockState, BlockStateModel> models = new HashMap<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            models.put(state, new SourceModel(state, dynamic));
        }
        return models;
    }

    private static List<BlockState> verticalStates(TrapDoorBlock block) {
        return block.getStateDefinition().getPossibleStates().stream()
                .filter(state -> state.getValue(TrapDoorProperties.VERTICAL)).toList();
    }

    private static List<BlockStateModelPart> collect(BlockStateModel model, BlockState state) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(42), parts);
        return parts;
    }

    private static AABB bounds(List<BakedQuad> quads) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (BakedQuad quad : quads) for (int vertex = 0; vertex < 4; vertex++) {
            var p = quad.position(vertex);
            minX = Math.min(minX, p.x()); minY = Math.min(minY, p.y()); minZ = Math.min(minZ, p.z());
            maxX = Math.max(maxX, p.x()); maxY = Math.max(maxY, p.y()); maxZ = Math.max(maxZ, p.z());
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void assertBox(AABB expected, AABB actual) {
        assertEquals(expected.minX, actual.minX, 1e-6);
        assertEquals(expected.minY, actual.minY, 1e-6);
        assertEquals(expected.minZ, actual.minZ, 1e-6);
        assertEquals(expected.maxX, actual.maxX, 1e-6);
        assertEquals(expected.maxY, actual.maxY, 1e-6);
        assertEquals(expected.maxZ, actual.maxZ, 1e-6);
    }

    private static class SourceModel implements DynamicBlockStateModel {
        private final BlockState bakedState;
        private final boolean dynamic;
        private final BakedQuad.MaterialInfo material;
        private final Material.Baked particle;
        private BlockState lastState;

        private SourceModel(BlockState bakedState, boolean dynamic) {
            this(bakedState, dynamic, MATERIAL, null);
        }

        private SourceModel(BlockState bakedState, boolean dynamic,
                BakedQuad.MaterialInfo material, Material.Baked particle) {
            this.bakedState = bakedState;
            this.dynamic = dynamic;
            this.material = material;
            this.particle = particle;
        }

        @Override public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                RandomSource random, List<BlockStateModelPart> output) {
            this.lastState = state;
            BlockState source = dynamic ? state : bakedState;
            output.add(new SourcePart(source.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds(), material, particle));
        }

        @Override public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
            collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, bakedState, random, output);
        }

        @Override public Material.Baked particleMaterial() { return particle; }
        @Override public int materialFlags() { return 3; }
    }

    private static final class SourcePart implements BlockStateModelPart {
        private final List<BakedQuad> quads;
        private final Material.Baked particle;

        private SourcePart(AABB box, BakedQuad.MaterialInfo material, Material.Baked particle) {
            this.particle = particle;
            var min = new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ);
            var max = new Vector3f((float) box.maxX, (float) box.maxY, (float) box.maxZ);
            quads = List.of(
                    quad(min.x, min.y, min.z, max.x, max.y, min.z, material),
                    quad(min.x, min.y, max.z, max.x, max.y, max.z, material),
                    quad(min.x, min.y, min.z, min.x, max.y, max.z, material),
                    quad(max.x, min.y, min.z, max.x, max.y, max.z, material),
                    quad(min.x, min.y, min.z, max.x, min.y, max.z, material),
                    quad(min.x, max.y, min.z, max.x, max.y, max.z, material));
        }

        private static BakedQuad quad(float x0, float y0, float z0, float x1, float y1, float z1,
                BakedQuad.MaterialInfo material) {
            return new BakedQuad(new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0),
                    new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1), 0L, 0L, 0L, 0L,
                    Direction.UP, material, BakedNormals.UNSPECIFIED, BakedColors.DEFAULT);
        }

        @Override public List<BakedQuad> getQuads(Direction side) {
            return side == null ? quads.subList(0, 5) : side == Direction.UP ? quads.subList(5, 6) : List.of();
        }
        @Override public boolean useAmbientOcclusion() { return true; }
        @Override public Material.Baked particleMaterial() { return particle; }
        @Override public int materialFlags() { return 3; }
    }
}

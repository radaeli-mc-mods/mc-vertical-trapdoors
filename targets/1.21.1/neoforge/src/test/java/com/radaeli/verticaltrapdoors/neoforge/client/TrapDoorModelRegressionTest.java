package com.radaeli.verticaltrapdoors.neoforge.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.blaze3d.platform.NativeImage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import com.radaeli.verticaltrapdoors.TrapDoorProperties;
import com.radaeli.verticaltrapdoors.TrapDoorOrientation;
import com.radaeli.verticaltrapdoors.neoforge.block.TrapDoorRegistrySnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrapDoorModelRegressionTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("third_party", "custom_trapdoor");
    private static TrapDoorBlock trapdoor;
    private static TextureAtlasSprite sprite;

    @BeforeAll
    static void bootstrap() throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // The isolated NeoForge test JVM has already frozen its registries.
        MappedRegistry<Block> registry = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;
        registry.unfreeze();
        try {
            trapdoor = Registry.register(registry, ID, new CustomTrapdoor());
        } finally {
            registry.freeze();
        }
        sprite = new TestSprite();
        assertTrue(trapdoor.defaultBlockState().hasProperty(TrapDoorProperties.VERTICAL), "Tests must load the production mixin");
    }

    @Test
    void bakedModelsMatchCollisionForEveryPlacementOrientation() {
        checkModels(false);
    }

    @Test
    void stateDependentModelsReceiveTheOriginalOpenState() {
        checkModels(true);
    }

    @Test
    void discoversAndAlignsEveryRegisteredTrapdoorIncludingOtherNamespaces() {
        TrapDoorRegistrySnapshot.collect(null);
        Map<ResourceLocation, TrapDoorBlock> registered = TrapDoorRegistrySnapshot.getTrapdoors();
        assertSame(trapdoor, registered.get(ID));
        assertTrue(registered.containsKey(ResourceLocation.withDefaultNamespace("acacia_trapdoor")));
        Map<ModelResourceLocation, BakedModel> models = new HashMap<>();
        registered.forEach((id, block) -> block.getStateDefinition().getPossibleStates().forEach(state ->
                models.put(BlockModelShaper.stateToModelLocation(id, state), new PanelModel(state, false))));
        TrapDoorModelEvents.applyModels(models, registered);
        registered.forEach((id, block) -> block.getStateDefinition().getPossibleStates().forEach(state -> {
            if (state.getValue(TrapDoorProperties.VERTICAL)) {
                BakedModel model = models.get(BlockModelShaper.stateToModelLocation(id, state));
                assertBox(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds(),
                        bounds(model.getQuads(state, null, RandomSource.create(1))), id + " " + state);
            }
        }));
    }

    private void checkModels(boolean dynamic) {
        Map<ModelResourceLocation, BakedModel> models = originalModels(dynamic);
        Map<ModelResourceLocation, BakedModel> originals = Map.copyOf(models);
        TrapDoorModelEvents.applyModels(models, Map.of(ID, trapdoor));
        for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = key(state);
            if (!state.getValue(TrapDoorProperties.VERTICAL)) {
                assertSame(originals.get(key), models.get(key), "Vanilla models must remain untouched: " + state);
                continue;
            }
            List<BakedQuad> quads = models.get(key).getQuads(state, null, RandomSource.create(1), ModelData.EMPTY, null);
            AABB mesh = bounds(quads);
            AABB collision = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            AABB outline = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            assertBox(collision, mesh, state.toString());
            assertBox(outline, mesh, state.toString());
            assertEquals(1.0, mesh.getYsize(), 1e-6, "Panel must stay upright: " + state);
            assertSame(sprite, quads.getFirst().getSprite());
            assertEquals(0, quads.getFirst().getVertices()[4], "UVs must stay attached to vertices");
            assertEquals(0, quads.getFirst().getVertices()[5], "UVs must stay attached to vertices");
        }
        // Rebuilding resources from fresh originals must produce the same result.
        Map<ModelResourceLocation, BakedModel> reload = new HashMap<>(originals);
        TrapDoorModelEvents.applyModels(reload, Map.of(ID, trapdoor));
        for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
            assertBox(bounds(models.get(key(state)).getQuads(state, null, RandomSource.create(1))),
                    bounds(reload.get(key(state)).getQuads(state, null, RandomSource.create(1))), state.toString());
        }
    }

    @Test
    void oppositeModifiersCloseOnTheSameWallAndOpenToOppositeSides() {
        for (Direction wall : Direction.Plane.HORIZONTAL) {
            BlockState vanilla = trapdoor.defaultBlockState().setValue(TrapDoorBlock.FACING, wall);
            BlockState shift = TrapDoorOrientation.placementState(vanilla, wall, true, false);
            BlockState alt = TrapDoorOrientation.placementState(vanilla, wall, false, true);
            Map<ModelResourceLocation, BakedModel> models = originalModels(false);
            TrapDoorModelEvents.applyModels(models, Map.of(ID, trapdoor));
            for (BlockState state : List.of(shift, alt)) {
                assertBox(openPanel(wall), bounds(models.get(key(state)).getQuads(state, null, RandomSource.create(1))), state.toString());
            }
            assertBox(openPanel(wall.getClockWise()), bounds(models.get(key(shift.setValue(TrapDoorBlock.OPEN, true)))
                    .getQuads(shift, null, RandomSource.create(1))), "Shift open " + wall);
            assertBox(openPanel(wall.getCounterClockWise()), bounds(models.get(key(alt.setValue(TrapDoorBlock.OPEN, true)))
                    .getQuads(alt, null, RandomSource.create(1))), "Alt open " + wall);
        }
    }

    @Test
    void placementPreservesVanillaAndShiftTakesPriority() {
        for (Direction facing : Direction.Plane.HORIZONTAL) for (Half half : Half.values()) {
            BlockState vanilla = trapdoor.defaultBlockState().setValue(TrapDoorBlock.FACING, facing)
                    .setValue(TrapDoorBlock.HALF, half).setValue(TrapDoorBlock.WATERLOGGED, true);
            assertSame(vanilla, TrapDoorOrientation.placementState(vanilla, facing, false, false));
            for (Direction face : List.of(Direction.UP, Direction.DOWN)) {
                for (boolean shift : List.of(false, true)) for (boolean alt : List.of(false, true)) {
                    assertSame(vanilla, TrapDoorOrientation.placementState(vanilla, face, shift, alt));
                }
            }
            BlockState shift = TrapDoorOrientation.placementState(vanilla, facing, true, false);
            assertSame(shift, TrapDoorOrientation.placementState(vanilla, facing, true, true));
            assertEquals(half, shift.getValue(TrapDoorBlock.HALF));
            assertTrue(shift.getValue(TrapDoorBlock.WATERLOGGED));
        }
    }

    private static Map<ModelResourceLocation, BakedModel> originalModels(boolean dynamic) {
        Map<ModelResourceLocation, BakedModel> models = new HashMap<>();
        for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
            models.put(key(state), new PanelModel(state, dynamic));
        }
        return models;
    }

    private static ModelResourceLocation key(BlockState state) {
        return BlockModelShaper.stateToModelLocation(ID, state);
    }

    @Test
    void realAcaciaFacesAndTextureMoveRigidlyAcrossOpenAndClosedStates() throws IOException {
        Map<ModelResourceLocation, BakedModel> models = realAcaciaModels();
        checkRigidAcaciaMotion(models);
    }

    @Test
    void verticalTextureOrientationMatchesUpperPlacementRegardlessOfClickHeight() throws IOException {
        Map<ModelResourceLocation, BakedModel> models = realAcaciaModels();
        for (Direction wall : Direction.Plane.HORIZONTAL) for (boolean alt : List.of(false, true))
                for (boolean opened : List.of(false, true)) {
            BlockState vanilla = trapdoor.defaultBlockState().setValue(TrapDoorBlock.FACING, wall);
            BlockState upper = TrapDoorOrientation.placementState(vanilla.setValue(TrapDoorBlock.HALF, Half.TOP), wall, !alt, alt)
                    .setValue(TrapDoorBlock.OPEN, opened);
            BlockState lower = TrapDoorOrientation.placementState(vanilla.setValue(TrapDoorBlock.HALF, Half.BOTTOM), wall, !alt, alt)
                    .setValue(TrapDoorBlock.OPEN, opened);
            List<BakedQuad> expected = models.get(key(upper)).getQuads(upper, null, RandomSource.create(42));
            List<BakedQuad> actual = models.get(key(lower)).getQuads(lower, null, RandomSource.create(42));
            assertEquals(expected.size(), actual.size());
            for (int q = 0; q < expected.size(); q++) {
                assertArrayEquals(expected.get(q).getVertices(), actual.get(q).getVertices(),
                        "Geometry, normals and UVs must match upper placement: " + lower + " face " + q);
                assertEquals(expected.get(q).getDirection(), actual.get(q).getDirection());
                assertSame(expected.get(q).getSprite(), actual.get(q).getSprite());
            }
        }
    }

    private static Map<ModelResourceLocation, BakedModel> realAcaciaModels() throws IOException {
        // Bake the game's actual asymmetric acacia model, including opposite faces,
        // edge UVs, per-half rotations, and faces stored in culling buckets.
        BlockModel template = BlockModel.fromString(resource("assets/minecraft/models/block/template_orientable_trapdoor_open.json"));
        JsonObject variants = JsonParser.parseString(resource("assets/minecraft/blockstates/acacia_trapdoor.json"))
                .getAsJsonObject().getAsJsonObject("variants");
        Map<ModelResourceLocation, BakedModel> models = new HashMap<>();
        for (BlockState state : trapdoor.getStateDefinition().getPossibleStates()) {
            String selector = "facing=" + state.getValue(TrapDoorBlock.FACING).getName()
                    + ",half=" + state.getValue(TrapDoorBlock.HALF).getSerializedName() + ",open=true";
            JsonObject variant = variants.getAsJsonObject(selector);
            BlockModelRotation rotation = BlockModelRotation.by(variant.has("x") ? variant.get("x").getAsInt() : 0,
                    variant.has("y") ? variant.get("y").getAsInt() : 0);
            Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);
            List<BakedQuad> unculled = new ArrayList<>();
            for (var element : template.getElements()) for (var face : element.faces.entrySet()) {
                BakedQuad quad = BlockModel.bakeFace(element, face.getValue(), sprite, face.getKey(), rotation);
                Direction cull = face.getValue().cullForDirection();
                if (cull == null) unculled.add(quad);
                else culled.computeIfAbsent(Direction.rotate(rotation.getRotation().getMatrix(), cull), d -> new ArrayList<>()).add(quad);
            }
            models.put(key(state), new BucketModel(unculled, culled));
        }
        TrapDoorModelEvents.applyModels(models, Map.of(ID, trapdoor));
        return models;
    }

    private static void checkRigidAcaciaMotion(Map<ModelResourceLocation, BakedModel> models) {
        for (Direction wall : Direction.Plane.HORIZONTAL) for (Half half : Half.values()) for (boolean alt : List.of(false, true)) {
            BlockState vanilla = trapdoor.defaultBlockState().setValue(TrapDoorBlock.FACING, wall).setValue(TrapDoorBlock.HALF, half);
            BlockState closed = TrapDoorOrientation.placementState(vanilla, wall, !alt, alt);
            BlockState open = closed.setValue(TrapDoorBlock.OPEN, true);
            List<BakedQuad> before = models.get(key(closed)).getQuads(closed, null, RandomSource.create(42));
            List<BakedQuad> after = models.get(key(open)).getQuads(open, null, RandomSource.create(42));
            assertEquals(6, before.size(), "Both panel faces and all four edges must survive culling");
            assertEquals(before.size(), after.size());
            AABB closedBox = closed.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            AABB openBox = open.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            assertBox(closedBox, bounds(before), closed.toString());
            assertBox(openBox, bounds(after), open.toString());
            // Independent pivot: centre of the overlapping hinge edge of both slabs.
            double px = (Math.max(closedBox.minX, openBox.minX) + Math.min(closedBox.maxX, openBox.maxX)) / 2;
            double pz = (Math.max(closedBox.minZ, openBox.minZ) + Math.min(closedBox.maxZ, openBox.maxZ)) / 2;
            int sign = alt ? -1 : 1;
            for (int q = 0; q < before.size(); q++) {
                BakedQuad a = before.get(q), b = after.get(q);
                assertSame(a.getSprite(), b.getSprite());
                assertEquals(a.getTintIndex(), b.getTintIndex());
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * 8;
                    double x = Float.intBitsToFloat(a.getVertices()[offset]);
                    double y = Float.intBitsToFloat(a.getVertices()[offset + 1]);
                    double z = Float.intBitsToFloat(a.getVertices()[offset + 2]);
                    assertEquals(px + sign * (z - pz), Float.intBitsToFloat(b.getVertices()[offset]), 1e-6);
                    assertEquals(y, Float.intBitsToFloat(b.getVertices()[offset + 1]), 1e-6);
                    assertEquals(pz - sign * (x - px), Float.intBitsToFloat(b.getVertices()[offset + 2]), 1e-6);
                    assertEquals(a.getVertices()[offset + 4], b.getVertices()[offset + 4], "U must not jump on either face");
                    assertEquals(a.getVertices()[offset + 5], b.getVertices()[offset + 5], "V must not jump on either face");
                }
                Direction normal = a.getDirection();
                assertEquals(sign * normal.getStepZ(), b.getDirection().getStepX());
                assertEquals(normal.getStepY(), b.getDirection().getStepY());
                assertEquals(-sign * normal.getStepX(), b.getDirection().getStepZ());
            }
            for (Direction side : Direction.values()) {
                assertTrue(models.get(key(open)).getQuads(open, side, RandomSource.create(42)).isEmpty());
            }
        }
    }

    private static String resource(String path) throws IOException {
        try (var stream = TrapDoorModelRegressionTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class BucketModel extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> unculled;
        private final Map<Direction, List<BakedQuad>> culled;
        BucketModel(List<BakedQuad> unculled, Map<Direction, List<BakedQuad>> culled) {
            super(null); this.unculled = unculled; this.culled = culled;
        }
        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return side == null ? unculled : culled.getOrDefault(side, List.of());
        }
        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            return getQuads(state, side, random);
        }
    }

    // Independent geometry fixture: vanilla open panels occupy a 3/16 slab at the block boundary.
    private static AABB openPanel(Direction facing) {
        return switch (facing) {
            case NORTH -> new AABB(0, 0, 13.0/16, 1, 1, 1);
            case SOUTH -> new AABB(0, 0, 0, 1, 1, 3.0/16);
            case EAST -> new AABB(0, 0, 0, 3.0/16, 1, 1);
            case WEST -> new AABB(13.0/16, 0, 0, 1, 1, 1);
            default -> throw new AssertionError(facing);
        };
    }

    private static AABB bounds(List<BakedQuad> quads) {
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (BakedQuad quad : quads) for (int i = 0; i < 4; i++) for (int axis = 0; axis < 3; axis++) {
            float value = Float.intBitsToFloat(quad.getVertices()[i * 8 + axis]);
            min[axis] = Math.min(min[axis], value);
            max[axis] = Math.max(max[axis], value);
        }
        return new AABB(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    private static void assertBox(AABB expected, AABB actual, String context) {
        assertArrayEquals(new double[]{expected.minX, expected.minY, expected.minZ, expected.maxX, expected.maxY, expected.maxZ},
                new double[]{actual.minX, actual.minY, actual.minZ, actual.maxX, actual.maxY, actual.maxZ}, 1e-6, context);
    }

    private static final class CustomTrapdoor extends TrapDoorBlock {
        CustomTrapdoor() { super(BlockSetType.OAK, BlockBehaviour.Properties.of()); }
    }

    private static final class TestSprite extends TextureAtlasSprite {
        TestSprite() throws IOException {
            super(ID, new SpriteContents(ID, new FrameSize(16, 16), acaciaImage(), ResourceMetadata.EMPTY), 16, 16, 0, 0);
        }
    }

    private static NativeImage acaciaImage() throws IOException {
        try (var stream = TrapDoorModelRegressionTest.class.getClassLoader().getResourceAsStream("assets/minecraft/textures/block/acacia_trapdoor.png")) {
            assertNotNull(stream);
            return NativeImage.read(stream);
        }
    }

    private static final class PanelModel extends BakedModelWrapper<BakedModel> {
        private final BlockState bakedState;
        private final boolean dynamic;
        PanelModel(BlockState state, boolean dynamic) { super(null); this.bakedState = state; this.dynamic = dynamic; }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            if (side != null) return List.of();
            BlockState source = dynamic ? state : bakedState;
            AABB box = source.getValue(TrapDoorBlock.OPEN) ? openPanel(source.getValue(TrapDoorBlock.FACING))
                    : source.getValue(TrapDoorBlock.HALF) == Half.TOP ? new AABB(0, 13.0/16, 0, 1, 1, 1) : new AABB(0, 0, 0, 1, 3.0/16, 1);
            int[] vertices = new int[32];
            for (int i = 0; i < 4; i++) {
                vertices[i*8] = Float.floatToIntBits((float)(i < 2 ? box.minX : box.maxX));
                vertices[i*8+1] = Float.floatToIntBits((float)(i % 2 == 0 ? box.minY : box.maxY));
                vertices[i*8+2] = Float.floatToIntBits((float)(i < 2 ? box.minZ : box.maxZ));
            }
            return List.of(new BakedQuad(vertices, -1, Direction.NORTH, sprite, true));
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            return getQuads(state, side, random);
        }
    }
}

package com.radaeli.verticaltrapdoors.neoforge.client;

import com.radaeli.verticaltrapdoors.client.PanelTransform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Transforms baked geometry while retaining the original quad and part materials. */
final class RotatedBlockStateModelPart implements BlockStateModelPart {
    private final BlockStateModelPart original;
    private final Matrix4f transform;
    private final Matrix3f normalTransform;

    RotatedBlockStateModelPart(BlockStateModelPart original, Matrix4f transform) {
        this.original = original;
        this.transform = new Matrix4f(transform);
        this.normalTransform = new Matrix3f(transform);
    }

    @Override public List<BakedQuad> getQuads(Direction side) {
        if (side != null) return List.of();
        List<BakedQuad> transformed = new ArrayList<>();
        append(transformed, original.getQuads(null));
        for (Direction sourceSide : Direction.values()) append(transformed, original.getQuads(sourceSide));
        return List.copyOf(transformed);
    }

    private void append(List<BakedQuad> output, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) output.add(transform(quad));
    }

    private BakedQuad transform(BakedQuad quad) {
        Vector3f p0 = transformedPosition(quad.position0());
        Vector3f p1 = transformedPosition(quad.position1());
        Vector3f p2 = transformedPosition(quad.position2());
        Vector3f p3 = transformedPosition(quad.position3());
        var info = quad.materialInfo();
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(),
                info.tintIndex(), info.shade(), info.lightEmission(), info.ambientOcclusion());
        return new BakedQuad(p0, p1, p2, p3, quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                rotate(quad.direction()), material, transformNormals(quad.bakedNormals()), quad.bakedColors());
    }

    private Vector3f transformedPosition(org.joml.Vector3fc originalPosition) {
        return transform.transformPosition(new Vector3f(originalPosition));
    }

    private Direction rotate(Direction direction) {
        Vector3f normal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        transform.transformDirection(normal).normalize();
        return Direction.getNearest(Math.round(normal.x), Math.round(normal.y), Math.round(normal.z), Direction.UP);
    }

    private BakedNormals transformNormals(BakedNormals normals) {
        int[] transformed = new int[4];
        for (int i = 0; i < transformed.length; i++) {
            int packed = normals.normal(i);
            if (BakedNormals.isUnspecified(packed)) transformed[i] = packed;
            else {
                Vector3f vector = BakedNormals.unpack(packed, null);
                normalTransform.transform(vector).normalize();
                transformed[i] = BakedNormals.pack(vector);
            }
        }
        return BakedNormals.of(transformed[0], transformed[1], transformed[2], transformed[3]);
    }

    @Override public boolean useAmbientOcclusion() { return original.useAmbientOcclusion(); }
    @Override public TriState ambientOcclusion() { return original.ambientOcclusion(); }
    @Override public Material.Baked particleMaterial() { return original.particleMaterial(); }
    @Override public int materialFlags() { return original.materialFlags(); }
}

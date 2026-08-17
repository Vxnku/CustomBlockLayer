package ru.vxnku.customblocklayer.render.cit;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BakedModel wrapper that positions raw CIT Resewn 3D models cleanly into in-world block coordinates.
 * Automatically grounds models at floor level (Y=0), centers them on the block, and rotates to match block facing.
 */
public class TransformedCitBakedModel implements BakedModel {
    private final BakedModel originalModel;
    private final List<BakedQuad> transformedUnculledQuads;

    public TransformedCitBakedModel(@NotNull BakedModel originalModel, @Nullable Direction facing, @NotNull ru.vxnku.customblocklayer.config.CustomBlockDefinition def) {
        this.originalModel = originalModel;

        // 1. Gather all original raw quads
        Random random = Random.create(42L);
        List<BakedQuad> allOriginalQuads = new ArrayList<>(originalModel.getQuads(null, null, random));
        for (Direction dir : Direction.values()) {
            allOriginalQuads.addAll(originalModel.getQuads(null, dir, random));
        }

        // 2. Compute bounding box of raw geometry
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (BakedQuad quad : allOriginalQuads) {
            int[] src = quad.getVertexData();
            for (int i = 0; i < 4; i++) {
                int offset = i * 8;
                float x = Float.intBitsToFloat(src[offset + 0]);
                float y = Float.intBitsToFloat(src[offset + 1]);
                float z = Float.intBitsToFloat(src[offset + 2]);

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            }
        }

        // 3. Build Matrix
        MatrixStack matrices = new MatrixStack();

        // Center on block anchor + custom offsets
        matrices.translate(0.5f + def.getOffsetX(), 0.0f + def.getOffsetY(), 0.5f + def.getOffsetZ());

        // Rotation matching facing + extraRotation from definition
        if (facing != null) {
            float rotY = switch (facing) {
                case NORTH -> 90.0f;
                case SOUTH -> -90.0f;
                case WEST -> 0.0f;
                case EAST -> 180.0f;
                default -> 0.0f;
            };
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY + def.getExtraRotation()));
        } else if (def.getExtraRotation() != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(def.getExtraRotation()));
        }

        // Apply scale from definition
        float scale = def.getScale();
        if (scale <= 0.0f) scale = 1.0f;
        if (scale != 1.0f) {
            matrices.scale(scale, scale, scale);
        }

        // Center model geometry and auto-ground to floor (Y=0)
        float centerX = (minX + maxX) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;
        float groundOffsetY = (minY < 0.0f) ? -minY : 0.0f;

        // Move to local origin, apply ground offset, and translate to anchor
        matrices.translate(-centerX, groundOffsetY, -centerZ);

        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // 4. Transform all quads
        List<BakedQuad> transformed = new ArrayList<>(allOriginalQuads.size());
        for (BakedQuad quad : allOriginalQuads) {
            transformed.add(transformQuad(quad, posMatrix));
        }

        this.transformedUnculledQuads = Collections.unmodifiableList(transformed);
    }

    private BakedQuad transformQuad(BakedQuad quad, Matrix4f posMatrix) {
        int[] src = quad.getVertexData();
        int[] dst = new int[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float x = Float.intBitsToFloat(src[offset + 0]);
            float y = Float.intBitsToFloat(src[offset + 1]);
            float z = Float.intBitsToFloat(src[offset + 2]);

            Vector4f pos = new Vector4f(x, y, z, 1.0F);
            pos.mul(posMatrix);

            dst[offset + 0] = Float.floatToRawIntBits(pos.x());
            dst[offset + 1] = Float.floatToRawIntBits(pos.y());
            dst[offset + 2] = Float.floatToRawIntBits(pos.z());
        }

        return new BakedQuad(dst, quad.getColorIndex(), quad.getFace(), quad.getSprite(), quad.hasShade());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        if (face == null) {
            return transformedUnculledQuads;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return originalModel.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return originalModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return originalModel.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return originalModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }
}

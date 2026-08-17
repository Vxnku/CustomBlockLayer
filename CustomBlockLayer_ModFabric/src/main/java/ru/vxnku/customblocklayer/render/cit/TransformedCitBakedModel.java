package ru.vxnku.customblocklayer.render.cit;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.model.json.Transformation;
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
 * BakedModel wrapper that transforms raw CIT Resewn item quads into in-world block coordinates.
 * Applies scale (2.87x), height offset, and facing rotation matching OptiFine/CIT item frame display.
 */
public class TransformedCitBakedModel implements BakedModel {
    private final BakedModel originalModel;
    private final List<BakedQuad> transformedUnculledQuads;

    public TransformedCitBakedModel(@NotNull BakedModel originalModel, @Nullable Direction facing) {
        this.originalModel = originalModel;

        // Build transformation matrix
        MatrixStack matrices = new MatrixStack();
        matrices.translate(0.5, 0.5, 0.5);

        if (facing != null) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        }

        // Apply CIT Resewn / Blockbench display transform for FIXED (Item Frame)
        Transformation fixedTransform = originalModel.getTransformation().getTransformation(ModelTransformationMode.FIXED);
        if (fixedTransform != null && !fixedTransform.equals(Transformation.IDENTITY)) {
            fixedTransform.apply(false, matrices);
        }

        matrices.translate(-0.5, -0.5, -0.5);

        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // Transform all original quads (both unculled and culled) into unculled block quads
        Random random = Random.create(42L);
        List<BakedQuad> allOriginalQuads = new ArrayList<>(originalModel.getQuads(null, null, random));
        for (Direction dir : Direction.values()) {
            allOriginalQuads.addAll(originalModel.getQuads(null, dir, random));
        }

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
        // All transformed 3D geometry is returned as unculled quads (face == null)
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

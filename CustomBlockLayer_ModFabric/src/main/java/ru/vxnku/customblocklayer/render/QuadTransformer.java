package ru.vxnku.customblocklayer.render;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;

import java.util.Arrays;

/**
 * Utility to transform BakedQuads and remap UV coordinates to custom sprites.
 */
public class QuadTransformer {
    private static final int VERTEX_SIZE = 8;
    private static final int UV_OFFSET = 4;

    /**
     * Creates a copy of the given BakedQuad with its UVs and Sprite remapped to newSprite.
     */
    public static BakedQuad remapQuad(BakedQuad original, Sprite newSprite) {
        if (original == null || newSprite == null) {
            return original;
        }

        Sprite oldSprite = original.getSprite();
        int[] originalVertices = original.getVertexData();
        int[] newVertices = Arrays.copyOf(originalVertices, originalVertices.length);

        float oldMinU = oldSprite != null ? oldSprite.getMinU() : 0.0f;
        float oldMaxU = oldSprite != null ? oldSprite.getMaxU() : 1.0f;
        float oldMinV = oldSprite != null ? oldSprite.getMinV() : 0.0f;
        float oldMaxV = oldSprite != null ? oldSprite.getMaxV() : 1.0f;

        float deltaOldU = oldMaxU - oldMinU;
        float deltaOldV = oldMaxV - oldMinV;
        if (Math.abs(deltaOldU) < 1.0e-6f) deltaOldU = 1.0f;
        if (Math.abs(deltaOldV) < 1.0e-6f) deltaOldV = 1.0f;

        float newMinU = newSprite.getMinU();
        float newMaxU = newSprite.getMaxU();
        float newMinV = newSprite.getMinV();
        float newMaxV = newSprite.getMaxV();

        for (int i = 0; i < 4; i++) {
            int base = i * VERTEX_SIZE;
            float u = Float.intBitsToFloat(newVertices[base + UV_OFFSET]);
            float v = Float.intBitsToFloat(newVertices[base + UV_OFFSET + 1]);

            // Normalized relative coordinate [0..1]
            float uRel = (u - oldMinU) / deltaOldU;
            float vRel = (v - oldMinV) / deltaOldV;

            // Remap to new sprite range
            float uNew = newMinU + uRel * (newMaxU - newMinU);
            float vNew = newMinV + vRel * (newMaxV - newMinV);

            newVertices[base + UV_OFFSET] = Float.floatToRawIntBits(uNew);
            newVertices[base + UV_OFFSET + 1] = Float.floatToRawIntBits(vNew);
        }

        return new BakedQuad(
            newVertices,
            -1, // Reset tintIndex to -1 so custom colors aren't tinted green by biome foliage/grass!
            original.getFace(),
            newSprite,
            original.hasShade()
        );
    }
}

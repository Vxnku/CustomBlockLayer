package ru.vxnku.customblocklayer.render;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Custom BakedModel dynamically generating true 8-pixel thick Vertical Slab geometry
 * with mathematically exact UV texture mapping on all 6 faces without distortion.
 */
public class VerticalSlabBakedModel implements BakedModel, FabricBakedModel {
    private final BakedModel originalModel;
    private final CustomBlockDefinition definition;
    private final Map<String, List<BakedQuad>> quadsCache = new ConcurrentHashMap<>();

    public VerticalSlabBakedModel(BakedModel originalModel, CustomBlockDefinition definition) {
        this.originalModel = originalModel;
        this.definition = definition;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random) {
        Direction facing = Direction.NORTH;
        if (state != null) {
            if (state.contains(Properties.HORIZONTAL_FACING)) {
                facing = state.get(Properties.HORIZONTAL_FACING);
            } else if (state.contains(Properties.FACING)) {
                facing = state.get(Properties.FACING);
            }
        }

        final Direction finalFacing = facing;
        String cacheKey = (side != null ? side.getName() : "null") + "_" + facing.getName() + "_" + (state == null ? "item" : "block");
        return quadsCache.computeIfAbsent(cacheKey, k -> generateQuadsForFacing(finalFacing, side));
    }

    private List<BakedQuad> generateQuadsForFacing(Direction facing, @Nullable Direction side) {
        float minX = 0.0f, maxX = 1.0f;
        float minY = 0.0f, maxY = 1.0f;
        float minZ = 0.0f, maxZ = 1.0f;

        // In vanilla Trapdoor: open trapdoor is at the opposite edge of facing
        switch (facing) {
            case NORTH -> { minZ = 0.5f; maxZ = 1.0f; } // South half
            case SOUTH -> { minZ = 0.0f; maxZ = 0.5f; } // North half
            case WEST  -> { minX = 0.5f; maxX = 1.0f; } // East half
            case EAST  -> { minX = 0.0f; maxX = 0.5f; } // West half
            default    -> { minZ = 0.5f; maxZ = 1.0f; }
        }

        List<BakedQuad> quads = new ArrayList<>();

        if (side != null) {
            Sprite sprite = CustomBlockRegistry.getSpriteForFace(definition, side);
            if (sprite != null) {
                BakedQuad q = buildBakedQuad(side, minX, minY, minZ, maxX, maxY, maxZ, sprite);
                if (q != null) quads.add(q);
            }
        } else {
            // Unculled / all faces for side==null or item
            for (Direction dir : Direction.values()) {
                Sprite sprite = CustomBlockRegistry.getSpriteForFace(definition, dir);
                if (sprite != null) {
                    BakedQuad q = buildBakedQuad(dir, minX, minY, minZ, maxX, maxY, maxZ, sprite);
                    if (q != null) quads.add(q);
                }
            }
        }

        return quads;
    }

    private BakedQuad buildBakedQuad(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Sprite sprite) {
        int[] vertexData = new int[32];
        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        float u0, u1, u2, u3;
        float v0, v1, v2, v3;
        float x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3;

        // Helper linear interpolations
        float uXMin = minU + minX * (maxU - minU);
        float uXMax = minU + maxX * (maxU - minU);
        float vZMin = minV + minZ * (maxV - minV);
        float vZMax = minV + maxZ * (maxV - minV);
        float uZMin = minU + minZ * (maxU - minU);
        float uZMax = minU + maxZ * (maxU - minU);

        switch (face) {
            case DOWN -> {
                x0 = minX; y0 = minY; z0 = maxZ;
                x1 = minX; y1 = minY; z1 = minZ;
                x2 = maxX; y2 = minY; z2 = minZ;
                x3 = maxX; y3 = minY; z3 = maxZ;

                u0 = uXMin; v0 = vZMax;
                u1 = uXMin; v1 = vZMin;
                u2 = uXMax; v2 = vZMin;
                u3 = uXMax; v3 = vZMax;
            }
            case UP -> {
                x0 = minX; y0 = maxY; z0 = minZ;
                x1 = minX; y1 = maxY; z1 = maxZ;
                x2 = maxX; y2 = maxY; z2 = maxZ;
                x3 = maxX; y3 = maxY; z3 = minZ;

                u0 = uXMin; v0 = vZMin;
                u1 = uXMin; v1 = vZMax;
                u2 = uXMax; v2 = vZMax;
                u3 = uXMax; v3 = vZMin;
            }
            case NORTH -> {
                x0 = maxX; y0 = maxY; z0 = minZ;
                x1 = maxX; y1 = minY; z1 = minZ;
                x2 = minX; y2 = minY; z2 = minZ;
                x3 = minX; y3 = maxY; z3 = minZ;

                u0 = minU + (1.0f - maxX) * (maxU - minU); v0 = minV;
                u1 = u0; v1 = maxV;
                u2 = minU + (1.0f - minX) * (maxU - minU); v2 = maxV;
                u3 = u2; v3 = minV;
            }
            case SOUTH -> {
                x0 = minX; y0 = maxY; z0 = maxZ;
                x1 = minX; y1 = minY; z1 = maxZ;
                x2 = maxX; y2 = minY; z2 = maxZ;
                x3 = maxX; y3 = maxY; z3 = maxZ;

                u0 = uXMin; v0 = minV;
                u1 = uXMin; v1 = maxV;
                u2 = uXMax; v2 = maxV;
                u3 = uXMax; v3 = minV;
            }
            case WEST -> {
                x0 = minX; y0 = maxY; z0 = minZ;
                x1 = minX; y1 = minY; z1 = minZ;
                x2 = minX; y2 = minY; z2 = maxZ;
                x3 = minX; y3 = maxY; z3 = maxZ;

                u0 = uZMin; v0 = minV;
                u1 = uZMin; v1 = maxV;
                u2 = uZMax; v2 = maxV;
                u3 = uZMax; v3 = minV;
            }
            case EAST -> {
                x0 = maxX; y0 = maxY; z0 = maxZ;
                x1 = maxX; y1 = minY; z1 = maxZ;
                x2 = maxX; y2 = minY; z2 = minZ;
                x3 = maxX; y3 = maxY; z3 = minZ;

                u0 = minU + (1.0f - maxZ) * (maxU - minU); v0 = minV;
                u1 = u0; v1 = maxV;
                u2 = minU + (1.0f - minZ) * (maxU - minU); v2 = maxV;
                u3 = u2; v3 = minV;
            }
            default -> { return null; }
        }

        putVertex(vertexData, 0, x0, y0, z0, u0, v0);
        putVertex(vertexData, 1, x1, y1, z1, u1, v1);
        putVertex(vertexData, 2, x2, y2, z2, u2, v2);
        putVertex(vertexData, 3, x3, y3, z3, u3, v3);

        return new BakedQuad(vertexData, -1, face, sprite, true);
    }

    private void putVertex(int[] data, int vertexIndex, float x, float y, float z, float u, float v) {
        int base = vertexIndex * 8;
        data[base] = Float.floatToRawIntBits(x);
        data[base + 1] = Float.floatToRawIntBits(y);
        data[base + 2] = Float.floatToRawIntBits(z);
        data[base + 3] = -1; // Color 0xFFFFFFFF
        data[base + 4] = Float.floatToRawIntBits(u);
        data[base + 5] = Float.floatToRawIntBits(v);
        data[base + 6] = 0;  // Light
        data[base + 7] = 0;  // Normal
    }

    private RenderMaterial getDefaultMaterial() {
        if (RendererAccess.INSTANCE.hasRenderer()) {
            return RendererAccess.INSTANCE.getRenderer().materialFinder().find();
        }
        return null;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        QuadEmitter emitter = context.getEmitter();
        Random random = randomSupplier.get();
        RenderMaterial material = getDefaultMaterial();

        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = getQuads(state, direction, random);
            for (BakedQuad quad : quads) {
                emitter.fromVanilla(quad, material, direction);
                emitter.emit();
            }
        }

        List<BakedQuad> unculled = getQuads(state, null, random);
        for (BakedQuad quad : unculled) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        QuadEmitter emitter = context.getEmitter();
        Random random = randomSupplier.get();
        RenderMaterial material = getDefaultMaterial();

        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = getQuads(null, direction, random);
            for (BakedQuad quad : quads) {
                emitter.fromVanilla(quad, material, direction);
                emitter.emit();
            }
        }

        List<BakedQuad> unculled = getQuads(null, null, random);
        for (BakedQuad quad : unculled) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean hasDepth() {
        return true;
    }

    @Override
    public boolean isSideLit() {
        return true;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        Sprite custom = CustomBlockRegistry.getSpriteForFace(definition, Direction.NORTH);
        if (custom != null) {
            return custom;
        }
        return originalModel.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return originalModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return originalModel.getOverrides();
    }
}

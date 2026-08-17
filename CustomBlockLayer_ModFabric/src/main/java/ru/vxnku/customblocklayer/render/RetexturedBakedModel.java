package ru.vxnku.customblocklayer.render;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
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
 * A BakedModel wrapper that dynamically remaps quad sprites to custom textures.
 * Fully supports 2D item/inventory icons, directional orientation (facing),
 * open/closed container states, tall plants, and multi-sided custom blocks.
 * Implements FabricBakedModel with explicit RenderMaterial to guarantee 100% compatibility with Sodium and Indigo.
 */
public class RetexturedBakedModel implements BakedModel, FabricBakedModel {
    private final BakedModel originalModel;
    private final CustomBlockDefinition definition;
    private final Map<String, List<BakedQuad>> quadsCache = new ConcurrentHashMap<>();

    public RetexturedBakedModel(BakedModel originalModel, CustomBlockDefinition definition) {
        this.originalModel = originalModel;
        this.definition = definition;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random) {
        String cacheKey = (side != null ? side.getName() : "null") + "_" + (state != null ? state.toString() : "item");
        List<BakedQuad> cached = quadsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<BakedQuad> originalQuads = originalModel.getQuads(state, side, random);
        List<BakedQuad> remapped = remapQuadsList(originalQuads, side, state);
        quadsCache.put(cacheKey, remapped);
        return remapped;
    }

    private List<BakedQuad> remapQuadsList(List<BakedQuad> originalQuads, @Nullable Direction side, @Nullable BlockState state) {
        if (originalQuads == null || originalQuads.isEmpty()) {
            return originalQuads != null ? originalQuads : List.of();
        }

        // Support for Tall Plants (DoubleBlockHalf.UPPER / LOWER)
        Direction halfOverride = null;
        if (state != null && state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.get(Properties.DOUBLE_BLOCK_HALF);
            halfOverride = (half == DoubleBlockHalf.UPPER) ? Direction.UP : Direction.DOWN;
        }

        List<BakedQuad> result = new ArrayList<>(originalQuads.size());
        for (BakedQuad quad : originalQuads) {
            Sprite newSprite = resolveCustomSprite(quad, side, state, halfOverride);
            if (newSprite != null) {
                result.add(QuadTransformer.remapQuad(quad, newSprite));
            } else {
                result.add(quad);
            }
        }
        return result;
    }

    private Sprite resolveCustomSprite(BakedQuad quad, @Nullable Direction side, @Nullable BlockState state, @Nullable Direction halfOverride) {
        // 1. Dedicated 2D Item / Inventory Icon rendering
        if (state == null && definition.getItemTexture() != null) {
            Sprite itemSprite = CustomBlockRegistry.getSprite(definition.getItemTexture());
            if (itemSprite != null) {
                return itemSprite;
            }
        }

        Sprite orig = quad.getSprite();
        String origPath = (orig != null && orig.getContents() != null) ? orig.getContents().getId().getPath().toLowerCase() : "";

        boolean isOpen = (state != null && state.contains(Properties.OPEN) && Boolean.TRUE.equals(state.get(Properties.OPEN)))
                || origPath.contains("open");

        // 2. Open Lid Texture (e.g. barrel open top / interior)
        if (isOpen && definition.getOpenTopTexture() != null) {
            if (origPath.contains("open") || origPath.contains("top") || (quad.getFace() == Direction.UP || side == Direction.UP)) {
                Sprite openSprite = CustomBlockRegistry.getSprite(definition.getOpenTopTexture());
                if (openSprite != null) {
                    return openSprite;
                }
            }
        }

        // 3. Semantic matching by original texture name keywords
        if (origPath.contains("top") || origPath.contains("up")) {
            Sprite s = CustomBlockRegistry.getSpriteForFace(definition, Direction.UP);
            if (s != null) return s;
        } else if (origPath.contains("bottom") || origPath.contains("down")) {
            Sprite s = CustomBlockRegistry.getSpriteForFace(definition, Direction.DOWN);
            if (s != null) return s;
        } else if (origPath.contains("side")) {
            Sprite s = CustomBlockRegistry.getSpriteForFace(definition, Direction.NORTH);
            if (s != null) return s;
        } else if (origPath.contains("front")) {
            Sprite s = CustomBlockRegistry.getSpriteForFace(definition, Direction.NORTH);
            if (s != null) return s;
        }

        // 4. Directional / Half matching fallback
        Direction quadFace = quad.getFace() != null ? quad.getFace() : side;
        Direction targetDirection = halfOverride != null ? halfOverride : quadFace;
        if (targetDirection != null) {
            Sprite s = CustomBlockRegistry.getSpriteForFace(definition, targetDirection);
            if (s != null) {
                return s;
            }
        }

        // 5. Default texture fallback
        if (definition.getDefaultTexture() != null) {
            return CustomBlockRegistry.getSprite(definition.getDefaultTexture());
        }

        return null;
    }

    private RenderMaterial getDefaultMaterial() {
        if (RendererAccess.INSTANCE.hasRenderer()) {
            return RendererAccess.INSTANCE.getRenderer().materialFinder().find();
        }
        return null;
    }

    // FabricBakedModel implementation for Sodium / Indigo / FRAPI
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
        return originalModel.isBuiltin();
    }

    @Override
    public Sprite getParticleSprite() {
        Sprite custom = CustomBlockRegistry.getSpriteForFace(definition, Direction.UP);
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

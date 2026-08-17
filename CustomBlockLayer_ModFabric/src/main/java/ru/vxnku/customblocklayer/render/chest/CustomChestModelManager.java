package ru.vxnku.customblocklayer.render.chest;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.cache.BlockOverrideCache;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated manager for custom chest entity textures and render layers.
 * Preserves vanilla 3D animated opening/closing of single and double chests with custom texture maps.
 * Compatible with Vanilla, Sodium, Iris, Enhanced Block Entities (EBE), and ETF.
 */
public class CustomChestModelManager {
    private static final Map<String, RenderLayer> CHEST_LAYER_CACHE = new ConcurrentHashMap<>();
    public static final ThreadLocal<String> CURRENT_ITEM_CUSTOM_ID = new ThreadLocal<>();

    // Cached model parts from ChestBlockEntityRenderer
    private static ModelPart singleLid;
    private static ModelPart singleLatch;
    private static ModelPart singleBase;

    private static ModelPart doubleLeftLid;
    private static ModelPart doubleLeftLatch;
    private static ModelPart doubleLeftBase;

    private static ModelPart doubleRightLid;
    private static ModelPart doubleRightLatch;
    private static ModelPart doubleRightBase;

    public static void initParts(
        ModelPart sLid, ModelPart sLatch, ModelPart sBase,
        ModelPart dlLid, ModelPart dlLatch, ModelPart dlBase,
        ModelPart drLid, ModelPart drLatch, ModelPart drBase
    ) {
        singleLid = sLid;
        singleLatch = sLatch;
        singleBase = sBase;

        doubleLeftLid = dlLid;
        doubleLeftLatch = dlLatch;
        doubleLeftBase = dlBase;

        doubleRightLid = drLid;
        doubleRightLatch = drLatch;
        doubleRightBase = drBase;
    }

    public static boolean hasParts() {
        return singleLid != null && singleBase != null;
    }

    public static void clear() {
        CHEST_LAYER_CACHE.clear();
    }

    public static boolean renderChest(
        @NotNull ChestBlockEntity entity,
        float tickDelta,
        @NotNull MatrixStack matrices,
        @NotNull VertexConsumerProvider vertexConsumers,
        int light,
        int overlay
    ) {
        String customId = null;
        if (entity.hasWorld()) {
            if (BlockOverrideCache.has(entity.getPos())) {
                customId = BlockOverrideCache.get(entity.getPos());
            }
        } else {
            customId = CURRENT_ITEM_CUSTOM_ID.get();
        }

        if (customId == null || !hasParts()) {
            return false;
        }

        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null || !def.isChest()) {
            return false;
        }

        BlockState state = entity.hasWorld() ? entity.getCachedState() : Blocks.CHEST.getDefaultState();
        Direction facing = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : Direction.SOUTH;
        ChestType chestType = state.contains(Properties.CHEST_TYPE) ? state.get(Properties.CHEST_TYPE) : ChestType.SINGLE;

        RenderLayer customLayer = getChestRenderLayer(customId, chestType);
        if (customLayer == null) {
            return false;
        }

        matrices.push();
        float rot = facing.asRotation();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rot));
        matrices.translate(-0.5F, -0.5F, -0.5F);

        float openProgress = 0.0F;
        if (entity.hasWorld()) {
            openProgress = entity.getAnimationProgress(tickDelta);
            openProgress = 1.0F - openProgress;
            openProgress = 1.0F - openProgress * openProgress * openProgress;
        }

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(customLayer);
        if (chestType == ChestType.LEFT) {
            renderPart(matrices, vertexConsumer, doubleLeftLid, doubleLeftLatch, doubleLeftBase, openProgress, light, overlay);
        } else if (chestType == ChestType.RIGHT) {
            renderPart(matrices, vertexConsumer, doubleRightLid, doubleRightLatch, doubleRightBase, openProgress, light, overlay);
        } else {
            renderPart(matrices, vertexConsumer, singleLid, singleLatch, singleBase, openProgress, light, overlay);
        }

        matrices.pop();
        return true;
    }

    private static void renderPart(
        MatrixStack matrices, VertexConsumer vertices,
        ModelPart lid, ModelPart latch, ModelPart base,
        float openFactor, int light, int overlay
    ) {
        lid.pitch = -(openFactor * 0.5F * (float) Math.PI);
        latch.pitch = lid.pitch;
        lid.render(matrices, vertices, light, overlay);
        latch.render(matrices, vertices, light, overlay);
        base.render(matrices, vertices, light, overlay);
    }

    @Nullable
    public static RenderLayer getChestRenderLayer(@Nullable String customId, @NotNull ChestType chestType) {
        if (customId == null) return null;

        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null) return null;

        String cacheKey = def.getId() + "_" + chestType.asString();
        RenderLayer cached = CHEST_LAYER_CACHE.get(cacheKey);
        if (cached != null) return cached;

        Identifier textureId = resolveChestTexture(def, chestType);
        if (textureId == null) return null;

        String path = textureId.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }

        Identifier fullTextureId = Identifier.of(textureId.getNamespace(), path);
        RenderLayer layer = RenderLayer.getEntityCutout(fullTextureId);
        CHEST_LAYER_CACHE.put(cacheKey, layer);
        return layer;
    }

    @Nullable
    public static Identifier resolveChestTexture(@NotNull CustomBlockDefinition def, @NotNull ChestType chestType) {
        if (chestType == ChestType.LEFT) {
            Identifier left = def.getChestLeftTexture();
            if (left != null) return left;
        } else if (chestType == ChestType.RIGHT) {
            Identifier right = def.getChestRightTexture();
            if (right != null) return right;
        }

        Identifier texture = def.getChestTexture();
        if (texture != null) {
            return texture;
        }
        if (def.getDefaultTexture() != null) {
            return def.getDefaultTexture();
        }
        return null;
    }
}

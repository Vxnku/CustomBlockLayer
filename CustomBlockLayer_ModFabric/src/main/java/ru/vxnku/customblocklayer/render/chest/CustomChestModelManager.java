package ru.vxnku.customblocklayer.render.chest;

import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated manager for custom chest entity textures and render layers.
 * Preserves vanilla 3D animated opening/closing of single and double chests with custom texture maps.
 */
public class CustomChestModelManager {
    private static final Map<String, RenderLayer> CHEST_LAYER_CACHE = new ConcurrentHashMap<>();
    public static final ThreadLocal<String> CURRENT_ITEM_CUSTOM_ID = new ThreadLocal<>();

    public static void clear() {
        CHEST_LAYER_CACHE.clear();
    }

    @Nullable
    public static RenderLayer getChestRenderLayer(@Nullable String customId, @NotNull ChestType chestType) {
        if (customId == null) return null;

        String cacheKey = customId + "_" + chestType.asString();
        RenderLayer cached = CHEST_LAYER_CACHE.get(cacheKey);
        if (cached != null) return cached;

        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null) return null;

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

package ru.vxnku.customblocklayer.render.chest;

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
 * Preserves vanilla 3D animated opening/closing of chests with custom texture maps.
 */
public class CustomChestModelManager {
    private static final Map<String, RenderLayer> CHEST_LAYER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> CHEST_TEXTURE_CACHE = new ConcurrentHashMap<>();
    public static final ThreadLocal<String> CURRENT_ITEM_CUSTOM_ID = new ThreadLocal<>();

    public static void clear() {
        CHEST_LAYER_CACHE.clear();
        CHEST_TEXTURE_CACHE.clear();
    }

    @Nullable
    public static RenderLayer getChestRenderLayer(@Nullable String customId) {
        if (customId == null) return null;

        RenderLayer cached = CHEST_LAYER_CACHE.get(customId);
        if (cached != null) return cached;

        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null) return null;

        Identifier textureId = resolveChestTexture(def);
        if (textureId == null) return null;

        // Append .png if missing
        if (!textureId.getPath().endsWith(".png")) {
            textureId = Identifier.of(textureId.getNamespace(), "textures/" + textureId.getPath() + ".png");
        }

        RenderLayer layer = RenderLayer.getEntityCutout(textureId);
        CHEST_LAYER_CACHE.put(customId, layer);
        return layer;
    }

    @Nullable
    public static Identifier resolveChestTexture(@NotNull CustomBlockDefinition def) {
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

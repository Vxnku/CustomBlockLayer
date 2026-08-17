package ru.vxnku.customblocklayer.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches and provides RetexturedBakedModels and VerticalSlabBakedModels.
 */
public class RetexturedModelManager {
    private static final Map<String, BakedModel> MODEL_CACHE = new ConcurrentHashMap<>();

    public static void clear() {
        MODEL_CACHE.clear();
    }

    public static BakedModel getModel(BakedModel originalModel, String customId, @Nullable BlockState state) {
        if (customId == null || originalModel == null) {
            return originalModel;
        }

        CustomBlockDefinition def = CustomBlockRegistry.getDefinition(customId);
        if (def == null) {
            return originalModel;
        }

        if (state != null) {
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            if (!def.matchesBlock(blockId)) {
                return originalModel;
            }
        }

        String cacheKey = System.identityHashCode(originalModel) + "_" + customId;
        if (def.isVerticalSlab()) {
            return MODEL_CACHE.computeIfAbsent(cacheKey, k -> new VerticalSlabBakedModel(originalModel, def));
        }

        return MODEL_CACHE.computeIfAbsent(cacheKey, k -> new RetexturedBakedModel(originalModel, def));
    }
}

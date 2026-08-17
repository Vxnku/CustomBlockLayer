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
        ru.vxnku.customblocklayer.render.cit.CitModelBridge.clear();
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
        if (state != null && def.isChest()) {
            return EmptyBakedModel.INSTANCE;
        }

        if (def.isVerticalSlab()) {
            return MODEL_CACHE.computeIfAbsent(cacheKey, k -> new VerticalSlabBakedModel(originalModel, def));
        }

        if (def.isJsonModel()) {
            // First check CIT Resewn soft bridge (provides 100% accurate OptiFine CIT texture/quad mapping)
            if (ru.vxnku.customblocklayer.render.cit.CitModelBridge.isCitAvailable()) {
                BakedModel citModel = ru.vxnku.customblocklayer.render.cit.CitModelBridge.getTransformedCitModel(def, state);
                if (citModel != null) {
                    return citModel;
                }
            }

            BakedModel jsonModel = CustomBlockRegistry.getJsonModel(customId);
            if (jsonModel != null) {
                return jsonModel;
            }
        }

        return MODEL_CACHE.computeIfAbsent(cacheKey, k -> new RetexturedBakedModel(originalModel, def));
    }
}

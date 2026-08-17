package ru.vxnku.customblocklayer.render.json;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Dedicated manager for loading, registering, and baking Blockbench 3D JSON models.
 */
public class JsonModelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlockLayer");
    private static final Set<Identifier> EXTRA_MODELS = new HashSet<>();

    public static void init() {
        ModelLoadingPlugin.register(pluginContext -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                ru.vxnku.customblocklayer.config.PropertiesResourceReloadListener.loadDefinitions(client.getResourceManager());
            }

            EXTRA_MODELS.clear();
            for (CustomBlockDefinition def : CustomBlockRegistry.getAllDefinitions()) {
                if (def.isJsonModel()) {
                    Identifier modelId = resolveModelIdentifier(def);
                    if (modelId != null) {
                        EXTRA_MODELS.add(modelId);
                        pluginContext.addModels(modelId);
                        LOGGER.info("Registered extra JSON model for loading: '{}' -> {}", def.getId(), modelId);
                    }
                }
            }
        });
    }

    public static void updateBakedModels() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getBakedModelManager() == null) return;

        for (CustomBlockDefinition def : CustomBlockRegistry.getAllDefinitions()) {
            if (def.isJsonModel()) {
                Identifier modelId = resolveModelIdentifier(def);
                if (modelId != null) {
                    BakedModel baked = client.getBakedModelManager().getModel(modelId);
                    if (baked == null || baked.equals(client.getBakedModelManager().getMissingModel())) {
                        baked = client.getBakedModelManager().getModel(new net.minecraft.client.util.ModelIdentifier(modelId, ""));
                    }
                    if (baked != null && !baked.equals(client.getBakedModelManager().getMissingModel())) {
                        CustomBlockRegistry.registerJsonModel(def.getId(), baked);
                        LOGGER.info("Registered 3D JSON BakedModel for '{}' -> {}", def.getId(), modelId);
                    } else {
                        LOGGER.warn("Failed to resolve 3D JSON BakedModel for '{}' at {}", def.getId(), modelId);
                    }
                }
            }
        }
    }

    private static Identifier resolveModelIdentifier(CustomBlockDefinition def) {
        String path = def.getModelPath();
        if (path != null && !path.trim().isEmpty()) {
            path = path.trim();
            if (path.contains(":")) {
                String[] parts = path.split(":", 2);
                return Identifier.of(parts[0], parts[1]);
            }
            return Identifier.of("minecraft", path);
        }

        // Default convention: cbl:block/<data_id> or minecraft:block/<data_id>
        return Identifier.of("customblocklayer", "block/" + def.getId());
    }
}

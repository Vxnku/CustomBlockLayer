package ru.vxnku.customblocklayer.render.cit;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft dependency bridge to CIT Resewn (and OptiFine CIT format).
 * Seamlessly queries CIT Resewn's baked item models and converts them into world block models.
 */
public class CitModelBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("CBL-CIT");
    private static final boolean CIT_AVAILABLE = FabricLoader.getInstance().isModLoaded("citresewn");
    private static final Map<String, BakedModel> TRANSFORMED_MODEL_CACHE = new ConcurrentHashMap<>();

    static {
        if (CIT_AVAILABLE) {
            String ver = FabricLoader.getInstance().getModContainer("citresewn")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
            LOGGER.info("[CBL-CIT] CIT Resewn detected (v{}). Direct 3D CIT Model Bridging is active.", ver);
        } else {
            LOGGER.info("[CBL-CIT] CIT Resewn not found. Standalone model rendering will be used.");
        }
    }

    public static boolean isCitAvailable() {
        return CIT_AVAILABLE;
    }

    public static void logConnectionHandshake() {
        if (CIT_AVAILABLE) {
            String ver = FabricLoader.getInstance().getModContainer("citresewn")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
            LOGGER.info("[CBL-CIT] Server connection established: Handshake with CIT Resewn (v{}) confirmed.", ver);
        } else {
            LOGGER.info("[CBL-CIT] Server connection established: CIT Resewn not active.");
        }
    }

    public static void clear() {
        TRANSFORMED_MODEL_CACHE.clear();
    }

    @Nullable
    public static BakedModel getTransformedCitModel(@NotNull CustomBlockDefinition def, @Nullable BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getItemRenderer() == null) {
            return null;
        }

        Direction facing = (state != null && state.contains(Properties.HORIZONTAL_FACING)) 
            ? state.get(Properties.HORIZONTAL_FACING) 
            : Direction.SOUTH;

        String cacheKey = def.getId() + "_" + facing.asString();
        BakedModel cached = TRANSFORMED_MODEL_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 1. Resolve matching item base
        Item baseItem = Items.ANVIL;
        if (!def.getMatchBlocks().isEmpty()) {
            Identifier firstMatch = def.getMatchBlocks().iterator().next();
            Item matchedItem = Registries.ITEM.get(firstMatch);
            if (matchedItem != Items.AIR) {
                baseItem = matchedItem;
            }
        }

        // 2. Build virtual ItemStack with Custom Name to trigger CIT matching
        ItemStack stack = new ItemStack(baseItem);
        String displayName = def.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = def.getId();
        }
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));

        // 3. Query ItemRenderer (CIT Resewn intercepts this and returns its 3D model)
        BakedModel rawCitModel = client.getItemRenderer().getModel(stack, client.world, client.player, 0);
        BakedModel defaultItemModel = client.getItemRenderer().getModels().getModel(baseItem);

        // If CIT Resewn successfully returned a custom model
        if (rawCitModel != null && !rawCitModel.equals(defaultItemModel) && !rawCitModel.equals(client.getBakedModelManager().getMissingModel())) {
            BakedModel transformed = new TransformedCitBakedModel(rawCitModel, facing, def);
            TRANSFORMED_MODEL_CACHE.put(cacheKey, transformed);
            LOGGER.info("[CBL-CIT] Successfully resolved and transformed CIT Model for '{}' (facing: {}, scale: {})", def.getId(), facing, def.getScale());
            return transformed;
        }

        return null;
    }
}

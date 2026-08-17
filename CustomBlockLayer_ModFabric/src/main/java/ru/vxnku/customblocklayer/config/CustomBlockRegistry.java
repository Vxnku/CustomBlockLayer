package ru.vxnku.customblocklayer.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry holding all loaded CustomBlockDefinitions and their resolved Sprites.
 */
public class CustomBlockRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlockLayer");
    private static final Map<String, CustomBlockDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<Identifier, Sprite> SPRITES = new ConcurrentHashMap<>();
    private static final Map<String, net.minecraft.client.render.model.BakedModel> JSON_MODELS = new ConcurrentHashMap<>();
    private static final Identifier MISSINGNO = Identifier.of("minecraft", "missingno");

    public static void clear() {
        DEFINITIONS.clear();
        SPRITES.clear();
        JSON_MODELS.clear();
        ru.vxnku.customblocklayer.render.chest.CustomChestModelManager.clear();
    }

    public static void registerJsonModel(String id, net.minecraft.client.render.model.BakedModel model) {
        if (id != null && model != null) {
            JSON_MODELS.put(id, model);
        }
    }

    public static net.minecraft.client.render.model.BakedModel getJsonModel(String id) {
        return id != null ? JSON_MODELS.get(id) : null;
    }

    public static boolean hasJsonModel(String id) {
        return id != null && JSON_MODELS.containsKey(id);
    }

    public static void register(CustomBlockDefinition definition) {
        DEFINITIONS.put(definition.getId(), definition);
    }

    public static CustomBlockDefinition getDefinition(String id) {
        return DEFINITIONS.get(id);
    }

    public static Collection<CustomBlockDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static boolean hasDefinition(String id) {
        return DEFINITIONS.containsKey(id);
    }

    /**
     * Resolves all referenced texture sprites from the Block Atlas after resource reload.
     */
    public static void updateSprites() {
        SPRITES.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null && client.getResourceManager() == null) {
            return;
        }

        SpriteAtlasTexture blockAtlas = client.getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        if (blockAtlas == null) {
            return;
        }

        for (CustomBlockDefinition def : DEFINITIONS.values()) {
            for (Identifier textureId : def.getAllReferencedTextures()) {
                Sprite sprite = findSpriteInAtlas(blockAtlas, textureId);
                if (sprite != null && !sprite.getContents().getId().equals(MISSINGNO)) {
                    SPRITES.put(textureId, sprite);
                    LOGGER.info("Resolved Sprite for '{}' -> {}", textureId, sprite.getContents().getId());
                } else {
                    LOGGER.warn("Failed to find valid Sprite for '{}' in block atlas!", textureId);
                }
            }
        }
    }

    private static Sprite findSpriteInAtlas(SpriteAtlasTexture blockAtlas, Identifier textureId) {
        if (textureId == null) return null;

        // 1. Direct lookup
        Sprite sprite = blockAtlas.getSprite(textureId);
        if (sprite != null && !sprite.getContents().getId().equals(MISSINGNO)) {
            return sprite;
        }

        String path = textureId.getPath();
        String cleanPath = path;
        if (cleanPath.startsWith("textures/")) {
            cleanPath = cleanPath.substring("textures/".length());
        }
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        // List of path variants to try
        String[] pathVariants;
        if (cleanPath.startsWith("cbl/")) {
            String withoutCbl = cleanPath.substring("cbl/".length());
            pathVariants = new String[]{
                cleanPath,
                withoutCbl,
                "textures/" + cleanPath,
                "textures/cbl/" + withoutCbl,
                "block/" + cleanPath,
                "block/" + withoutCbl
            };
        } else {
            pathVariants = new String[]{
                cleanPath,
                "cbl/" + cleanPath,
                "textures/" + cleanPath,
                "textures/cbl/" + cleanPath,
                "block/" + cleanPath
            };
        }

        String[] namespaces = new String[]{
            textureId.getNamespace(),
            textureId.getNamespace().equals("minecraft") ? "customblocklayer" : "minecraft"
        };

        for (String ns : namespaces) {
            for (String p : pathVariants) {
                Identifier testId = Identifier.of(ns, p);
                sprite = blockAtlas.getSprite(testId);
                if (sprite != null && !sprite.getContents().getId().equals(MISSINGNO)) {
                    return sprite;
                }
            }
        }

        return null;
    }

    public static Sprite getSprite(Identifier textureId) {
        if (textureId == null) return null;
        Sprite cached = SPRITES.get(textureId);
        if (cached == null) {
            updateSprites();
            cached = SPRITES.get(textureId);
        }
        return cached;
    }

    public static Sprite getSpriteForFace(CustomBlockDefinition def, Direction direction) {
        if (def == null) return null;
        Identifier textureId = def.getTextureForFace(direction);
        return getSprite(textureId);
    }
}

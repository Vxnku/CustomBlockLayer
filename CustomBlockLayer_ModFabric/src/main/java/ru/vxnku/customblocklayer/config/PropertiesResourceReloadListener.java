package ru.vxnku.customblocklayer.config;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vxnku.customblocklayer.item.CBLItemGroups;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("CustomBlockLayer");
    public static final Identifier ID = Identifier.of("customblocklayer", "cbl_properties");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        CustomBlockRegistry.clear();
        ru.vxnku.customblocklayer.render.RetexturedModelManager.clear();
        int count = 0;

        Map<Identifier, Resource> allResources = new HashMap<>();
        allResources.putAll(manager.findResources("cbl", id -> id.getPath().endsWith(".properties")));
        allResources.putAll(manager.findResources("optifine/cbl", id -> id.getPath().endsWith(".properties")));

        for (Map.Entry<Identifier, Resource> entry : allResources.entrySet()) {
            Identifier resourceId = entry.getKey();
            try (InputStream stream = entry.getValue().getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                Properties props = new Properties();
                props.load(reader);

                String path = resourceId.getPath();
                int lastSlash = path.lastIndexOf('/');
                int dot = path.lastIndexOf('.');
                String fileNameNoExt = (lastSlash >= 0 && dot > lastSlash) ? path.substring(lastSlash + 1, dot) : "custom_block";
                String parentDir = lastSlash >= 0 ? path.substring(0, lastSlash) : "";

                String dataId = props.getProperty("data");
                if (dataId == null || dataId.trim().isEmpty()) {
                    dataId = fileNameNoExt;
                }
                dataId = dataId.trim();

                CustomBlockDefinition def = new CustomBlockDefinition(dataId);

                // type (e.g. block, vertical_slab)
                String typeStr = props.getProperty("type");
                if (typeStr != null && !typeStr.trim().isEmpty()) {
                    def.setType(typeStr.trim());
                }

                // name / displayName
                String name = props.getProperty("name");
                if (name == null || name.trim().isEmpty()) {
                    name = props.getProperty("displayName");
                }
                if (name != null && !name.trim().isEmpty()) {
                    def.setDisplayName(name.trim());
                }

                // colum / column / tab / category
                String colum = props.getProperty("colum");
                if (colum == null || colum.trim().isEmpty()) {
                    colum = props.getProperty("column");
                }
                if (colum == null || colum.trim().isEmpty()) {
                    colum = props.getProperty("tab");
                }
                if (colum == null || colum.trim().isEmpty()) {
                    colum = props.getProperty("category");
                }
                if (colum != null && !colum.trim().isEmpty()) {
                    def.setColumn(colum.trim());
                }

                // matchBlocks
                String matchBlocksStr = props.getProperty("matchBlocks");
                if (matchBlocksStr != null && !matchBlocksStr.trim().isEmpty()) {
                    for (String blockStr : matchBlocksStr.split("[,\s]+")) {
                        if (!blockStr.trim().isEmpty()) {
                            def.addMatchBlock(parseIdentifier(blockStr.trim(), "minecraft"));
                        }
                    }
                }

                // Default texture
                String defaultTextureStr = props.getProperty("texture");
                if (defaultTextureStr != null && !defaultTextureStr.trim().isEmpty()) {
                    def.setDefaultTexture(resolveTextureId(resourceId.getNamespace(), parentDir, defaultTextureStr.trim()));
                }

                // Face textures
                parseFaceTexture(props, "texture.top", Direction.UP, resourceId.getNamespace(), parentDir, def);
                parseFaceTexture(props, "texture.up", Direction.UP, resourceId.getNamespace(), parentDir, def);
                parseFaceTexture(props, "texture.bottom", Direction.DOWN, resourceId.getNamespace(), parentDir, def);
                parseFaceTexture(props, "texture.down", Direction.DOWN, resourceId.getNamespace(), parentDir, def);

                parseFaceTexture(props, "texture.north", Direction.NORTH, resourceId.getNamespace(), parentDir, def);
                parseFaceTexture(props, "texture.south", Direction.SOUTH, resourceId.getNamespace(), parentDir, def);
                parseFaceTexture(props, "texture.east", Direction.EAST, resourceId.getNamespace(), parentDir, def);
                parseFaceTexture(props, "texture.west", Direction.WEST, resourceId.getNamespace(), parentDir, def);

                String sideTextureStr = props.getProperty("texture.side");
                if (sideTextureStr != null && !sideTextureStr.trim().isEmpty()) {
                    Identifier sideId = resolveTextureId(resourceId.getNamespace(), parentDir, sideTextureStr.trim());
                    for (Direction dir : Direction.Type.HORIZONTAL) {
                        if (def.getTextureForFace(dir) == def.getDefaultTexture()) {
                            def.setFaceTexture(dir, sideId);
                        }
                    }
                }

                // Open state texture (e.g. barrel open lid)
                String openTopStr = props.getProperty("texture.top.open");
                if (openTopStr == null || openTopStr.trim().isEmpty()) {
                    openTopStr = props.getProperty("texture.open.top");
                }
                if (openTopStr == null || openTopStr.trim().isEmpty()) {
                    openTopStr = props.getProperty("texture.top_open");
                }
                if (openTopStr == null || openTopStr.trim().isEmpty()) {
                    openTopStr = props.getProperty("texture.open");
                }
                if (openTopStr == null || openTopStr.trim().isEmpty()) {
                    openTopStr = props.getProperty("open.texture");
                }
                if (openTopStr != null && !openTopStr.trim().isEmpty()) {
                    def.setOpenTopTexture(resolveTextureId(resourceId.getNamespace(), parentDir, openTopStr.trim()));
                }

                // 2D Item / Inventory Icon Texture
                String itemTextureStr = props.getProperty("texture.item");
                if (itemTextureStr == null || itemTextureStr.trim().isEmpty()) {
                    itemTextureStr = props.getProperty("texture.icon");
                }
                if (itemTextureStr == null || itemTextureStr.trim().isEmpty()) {
                    itemTextureStr = props.getProperty("item.texture");
                }
                if (itemTextureStr == null || itemTextureStr.trim().isEmpty()) {
                    itemTextureStr = props.getProperty("item");
                }
                if (itemTextureStr != null && !itemTextureStr.trim().isEmpty()) {
                    def.setItemTexture(resolveTextureId(resourceId.getNamespace(), parentDir, itemTextureStr.trim()));
                }

                if (def.getDefaultTexture() == null && def.getAllReferencedTextures().isEmpty()) {
                    def.setDefaultTexture(resolveTextureId(resourceId.getNamespace(), parentDir, fileNameNoExt));
                }

                CustomBlockRegistry.register(def);
                CBLItemGroups.registerColumnTab(def.getColumn());
                count++;
                LOGGER.info("Registered CBL Block: '{}' (type='{}', colum='{}') -> defaultTexture: '{}'",
                    def.getId(), def.getType(), def.getColumn(), def.getDefaultTexture());
            } catch (Exception e) {
                LOGGER.error("Failed to load CBL properties file: {}", resourceId, e);
            }
        }

        LOGGER.info("Successfully loaded {} CustomBlockLayer definitions from resource packs.", count);
    }

    private void parseFaceTexture(Properties props, String key, Direction dir, String namespace, String parentDir, CustomBlockDefinition def) {
        String val = props.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            def.setFaceTexture(dir, resolveTextureId(namespace, parentDir, val.trim()));
        }
    }

    private Identifier resolveTextureId(String currentNamespace, String parentDir, String pathOrId) {
        if (pathOrId.endsWith(".png")) {
            pathOrId = pathOrId.substring(0, pathOrId.length() - 4);
        }
        
        String namespace = currentNamespace;
        String path = pathOrId;
        if (pathOrId.contains(":")) {
            String[] parts = pathOrId.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }

        // Clean redundant prefixes
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (!path.contains("/")) {
            if (!parentDir.isEmpty()) {
                String cleanParent = parentDir.startsWith("textures/") ? parentDir.substring("textures/".length()) : parentDir;
                return Identifier.of(namespace, cleanParent + "/" + path);
            }
            return Identifier.of(namespace, "cbl/" + path);
        }
        return Identifier.of(namespace, path);
    }

    private Identifier parseIdentifier(String str, String defaultNamespace) {
        if (str.contains(":")) {
            return Identifier.of(str);
        }
        return Identifier.of(defaultNamespace, str);
    }
}

package ru.vxnku.customblocklayer.config;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data definition parsed from .properties file
 */
public class CustomBlockDefinition {
    private final String id;
    private String type = "block";
    private String displayName;
    private String column = "CustomBlockLayer";
    private final Set<Identifier> matchBlocks = new HashSet<>();
    private Identifier defaultTexture;
    private Identifier openTopTexture;
    private Identifier itemTexture;
    private Identifier chestTexture;
    private Identifier chestLeftTexture;
    private Identifier chestRightTexture;
    private String modelPath;
    private float scale = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float offsetZ = 0.0f;
    private float extraRotation = 0.0f;
    private final Map<Direction, Identifier> faceTextures = new EnumMap<>(Direction.class);

    public CustomBlockDefinition(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null && !type.trim().isEmpty()) {
            this.type = type.trim().toLowerCase();
        }
    }

    public boolean isVerticalSlab() {
        return "vertical_slab".equalsIgnoreCase(type)
            || "verticalslab".equalsIgnoreCase(type)
            || "v_slab".equalsIgnoreCase(type)
            || id.contains("vertical_slab");
    }

    public boolean isChest() {
        return "chest".equalsIgnoreCase(type)
            || "animated_chest".equalsIgnoreCase(type)
            || "custom_chest".equalsIgnoreCase(type);
    }

    public boolean isJsonModel() {
        return "json".equalsIgnoreCase(type)
            || "model".equalsIgnoreCase(type)
            || modelPath != null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        if (column != null && !column.trim().isEmpty()) {
            this.column = column.trim();
        }
    }

    public Set<Identifier> getMatchBlocks() {
        return matchBlocks;
    }

    public void addMatchBlock(Identifier blockId) {
        this.matchBlocks.add(blockId);
    }

    public boolean matchesBlock(Identifier blockId) {
        return matchBlocks.isEmpty() || matchBlocks.contains(blockId);
    }

    public Identifier getDefaultTexture() {
        return defaultTexture;
    }

    public void setDefaultTexture(Identifier defaultTexture) {
        this.defaultTexture = defaultTexture;
    }

    public Identifier getOpenTopTexture() {
        return openTopTexture;
    }

    public void setOpenTopTexture(Identifier openTopTexture) {
        this.openTopTexture = openTopTexture;
    }

    public Identifier getItemTexture() {
        return itemTexture;
    }

    public void setItemTexture(Identifier itemTexture) {
        this.itemTexture = itemTexture;
    }

    public void setFaceTexture(Direction direction, Identifier texture) {
        this.faceTextures.put(direction, texture);
    }

    public Identifier getTextureForFace(Direction direction) {
        if (direction != null && faceTextures.containsKey(direction)) {
            return faceTextures.get(direction);
        }
        if (defaultTexture != null) {
            return defaultTexture;
        }
        if (!faceTextures.isEmpty()) {
            return faceTextures.values().iterator().next();
        }
        return null;
    }

    public Identifier getChestTexture() {
        return chestTexture != null ? chestTexture : defaultTexture;
    }

    public void setChestTexture(Identifier chestTexture) {
        this.chestTexture = chestTexture;
    }

    public Identifier getChestLeftTexture() {
        return chestLeftTexture != null ? chestLeftTexture : getChestTexture();
    }

    public void setChestLeftTexture(Identifier chestLeftTexture) {
        this.chestLeftTexture = chestLeftTexture;
    }

    public Identifier getChestRightTexture() {
        return chestRightTexture != null ? chestRightTexture : getChestTexture();
    }

    public void setChestRightTexture(Identifier chestRightTexture) {
        this.chestRightTexture = chestRightTexture;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public float getOffsetZ() {
        return offsetZ;
    }

    public void setOffsetZ(float offsetZ) {
        this.offsetZ = offsetZ;
    }

    public float getExtraRotation() {
        return extraRotation;
    }

    public void setExtraRotation(float extraRotation) {
        this.extraRotation = extraRotation;
    }

    public Set<Identifier> getAllReferencedTextures() {
        Set<Identifier> set = new HashSet<>(faceTextures.values());
        if (defaultTexture != null) {
            set.add(defaultTexture);
        }
        if (openTopTexture != null) {
            set.add(openTopTexture);
        }
        if (itemTexture != null) {
            set.add(itemTexture);
        }
        if (chestTexture != null) {
            set.add(chestTexture);
        }
        if (chestLeftTexture != null) {
            set.add(chestLeftTexture);
        }
        if (chestRightTexture != null) {
            set.add(chestRightTexture);
        }
        return set;
    }
}

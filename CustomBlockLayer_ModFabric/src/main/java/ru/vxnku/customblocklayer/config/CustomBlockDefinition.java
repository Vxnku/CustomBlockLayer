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
        return set;
    }
}

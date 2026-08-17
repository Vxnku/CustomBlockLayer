package ru.vxnku.customblocklayer.registry;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.item.CBLItemFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Structured data representing a known Custom Block definition on the server.
 * Used for WorldEdit integrations, commands, GUI menus, and validation.
 */
public class CustomBlockEntry {
    private final String id;
    private final String displayName;
    private final Material baseMaterial;
    private final Set<Material> matchMaterials = new HashSet<>();
    private final String defaultTexture;
    private final Map<String, String> faceTextures = new HashMap<>();
    private final String sourcePath;

    public CustomBlockEntry(@NotNull String id,
                            @NotNull String displayName,
                            @NotNull Material baseMaterial,
                            @Nullable String defaultTexture,
                            @Nullable String sourcePath) {
        this.id = id;
        this.displayName = displayName;
        this.baseMaterial = baseMaterial;
        this.defaultTexture = defaultTexture;
        this.sourcePath = sourcePath;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public Material getBaseMaterial() {
        return baseMaterial;
    }

    @NotNull
    public Set<Material> getMatchMaterials() {
        return Collections.unmodifiableSet(matchMaterials);
    }

    public void addMatchMaterial(@NotNull Material material) {
        this.matchMaterials.add(material);
    }

    public boolean matches(@NotNull Material material) {
        return matchMaterials.isEmpty() || matchMaterials.contains(material);
    }

    @Nullable
    public String getDefaultTexture() {
        return defaultTexture;
    }

    public void setFaceTexture(@NotNull String face, @NotNull String texture) {
        this.faceTextures.put(face.toLowerCase(), texture);
    }

    @Nullable
    public String getFaceTexture(@NotNull String face) {
        return faceTextures.get(face.toLowerCase());
    }

    @NotNull
    public Map<String, String> getFaceTextures() {
        return Collections.unmodifiableMap(faceTextures);
    }

    @Nullable
    public String getSourcePath() {
        return sourcePath;
    }

    @NotNull
    public ItemStack createItem(int amount) {
        return CBLItemFactory.createItem(id, baseMaterial, amount);
    }

    @NotNull
    public ItemStack createItem() {
        return createItem(1);
    }

    @Override
    public String toString() {
        return "CustomBlockEntry{id='" + id + "', name='" + displayName + "', base=" + baseMaterial + "}";
    }
}

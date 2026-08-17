package ru.vxnku.customblocklayer.registry;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Server-side registry that scans plugin resource folders or ZIP packs
 * to build a structured catalog of all available custom blocks.
 * Designed for WorldEdit integrations, administrative tools, and API consumers.
 */
public class ServerBlockRegistry {
    private final CustomBlockLayerPlugin plugin;
    private final Map<String, CustomBlockEntry> entriesById = new ConcurrentHashMap<>();
    private final Map<Material, List<CustomBlockEntry>> entriesByMaterial = new ConcurrentHashMap<>();

    public ServerBlockRegistry(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        entriesById.clear();
        entriesByMaterial.clear();

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Directories to scan
        List<File> scanFolders = List.of(
            new File(dataFolder, "resourcepack"),
            new File(dataFolder, "packs"),
            new File(dataFolder, "cbl"),
            new File(dataFolder, "content")
        );

        for (File dir : scanFolders) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            scanDirectory(dir);
        }

        plugin.getLogger().info("[CBL-Registry] Успешно загружено " + entriesById.size() + " определений кастомных блоков для WorldEdit/API.");
    }

    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (file.getName().endsWith(".properties")) {
                parsePropertiesFile(file);
            } else if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
                scanZipFile(file);
            }
        }
    }

    private void scanZipFile(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".properties") && entry.getName().contains("cbl")) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        parsePropertiesStream(is, entry.getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CBL-Registry] Ошибка при чтении ZIP-файла: " + zipFile.getName() + ": " + e.getMessage());
        }
    }

    private void parsePropertiesFile(File file) {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            parsePropertiesStream(is, file.getPath());
        } catch (Exception e) {
            plugin.getLogger().warning("[CBL-Registry] Ошибка чтения .properties файла " + file.getName() + ": " + e.getMessage());
        }
    }

    private void parsePropertiesStream(InputStream is, String sourcePath) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            Properties props = new Properties();
            props.load(reader);

            String fileName = new File(sourcePath).getName();
            String fileNameNoExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

            String dataId = props.getProperty("data");
            if (dataId == null || dataId.trim().isEmpty()) {
                dataId = fileNameNoExt;
            }
            dataId = dataId.trim();

            String displayName = props.getProperty("name");
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = props.getProperty("displayName");
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = formatDisplayName(dataId);
            }
            displayName = displayName.trim();

            String texture = props.getProperty("texture");

            // Match Blocks & Base Material
            String matchBlocksStr = props.getProperty("matchBlocks");
            Material baseMaterial = Material.STONE;
            Set<Material> matchedMaterials = new HashSet<>();

            if (matchBlocksStr != null && !matchBlocksStr.trim().isEmpty()) {
                for (String part : matchBlocksStr.split("[,\\s]+")) {
                    if (!part.trim().isEmpty()) {
                        Material mat = parseMaterial(part.trim());
                        if (mat != null) {
                            matchedMaterials.add(mat);
                            if (baseMaterial == Material.STONE) {
                                baseMaterial = mat;
                            }
                        }
                    }
                }
            }

            if (baseMaterial == Material.STONE) {
                baseMaterial = inferBaseMaterial(dataId);
            }

            CustomBlockEntry entry = new CustomBlockEntry(dataId, displayName, baseMaterial, texture, sourcePath);
            for (Material m : matchedMaterials) {
                entry.addMatchMaterial(m);
            }

            // Face textures
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("texture.")) {
                    String face = key.substring("texture.".length());
                    entry.setFaceTexture(face, props.getProperty(key).trim());
                }
            }

            registerEntry(entry);
        } catch (Exception e) {
            plugin.getLogger().warning("[CBL-Registry] Ошибка парсинга свойства (" + sourcePath + "): " + e.getMessage());
        }
    }

    public synchronized void registerEntry(@NotNull CustomBlockEntry entry) {
        entriesById.put(entry.getId(), entry);
        entriesByMaterial.computeIfAbsent(entry.getBaseMaterial(), k -> new ArrayList<>()).add(entry);
        for (Material m : entry.getMatchMaterials()) {
            if (m != entry.getBaseMaterial()) {
                entriesByMaterial.computeIfAbsent(m, k -> new ArrayList<>()).add(entry);
            }
        }
    }

    @Nullable
    public CustomBlockEntry getBlock(@NotNull String id) {
        return entriesById.get(id);
    }

    public boolean isKnown(@NotNull String id) {
        return entriesById.containsKey(id);
    }

    @NotNull
    public Collection<CustomBlockEntry> getAllBlocks() {
        return Collections.unmodifiableCollection(entriesById.values());
    }

    @NotNull
    public Set<String> getKnownIds() {
        return Collections.unmodifiableSet(entriesById.keySet());
    }

    @NotNull
    public List<CustomBlockEntry> getBlocksMatching(@NotNull Material material) {
        return entriesByMaterial.getOrDefault(material, Collections.emptyList());
    }

    private Material parseMaterial(String name) {
        if (name.contains(":")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        return Material.matchMaterial(name.toUpperCase());
    }

    private Material inferBaseMaterial(String id) {
        String lower = id.toLowerCase();
        if (lower.contains("stair")) return Material.QUARTZ_STAIRS;
        if (lower.contains("slab")) return Material.QUARTZ_SLAB;
        if (lower.contains("crate") || lower.contains("plank")) return Material.OAK_PLANKS;
        if (lower.contains("ore")) return Material.DEEPSLATE;
        return Material.STONE;
    }

    private String formatDisplayName(String id) {
        String[] parts = id.split("[_\\s-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }
}

package ru.vxnku.customblocklayer.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.registry.CustomBlockEntry;

import java.util.HashMap;
import java.util.Map;

public class CBLItemFactory {
    public static final NamespacedKey CBL_KEY = new NamespacedKey("customblocklayer", "cbl");
    public static final NamespacedKey MINECRAFT_CBL_KEY = new NamespacedKey("minecraft", "cbl");

    private static final Map<String, String> TRANSLATIONS = new HashMap<>();
    static {
        // Concrete Stairs (16 colors)
        TRANSLATIONS.put("white_concrete_stairs", "Ступеньки из Белого бетона");
        TRANSLATIONS.put("orange_concrete_stairs", "Ступеньки из Оранжевого бетона");
        TRANSLATIONS.put("magenta_concrete_stairs", "Ступеньки из Пурпурного бетона");
        TRANSLATIONS.put("light_blue_concrete_stairs", "Ступеньки из Светло-синего бетона");
        TRANSLATIONS.put("yellow_concrete_stairs", "Ступеньки из Желтого бетона");
        TRANSLATIONS.put("lime_concrete_stairs", "Ступеньки из Лаймового бетона");
        TRANSLATIONS.put("pink_concrete_stairs", "Ступеньки из Розового бетона");
        TRANSLATIONS.put("gray_concrete_stairs", "Ступеньки из Серого бетона");
        TRANSLATIONS.put("light_gray_concrete_stairs", "Ступеньки из Светло-серого бетона");
        TRANSLATIONS.put("cyan_concrete_stairs", "Ступеньки из Бирюзового бетона");
        TRANSLATIONS.put("purple_concrete_stairs", "Ступеньки из Фиолетового бетона");
        TRANSLATIONS.put("blue_concrete_stairs", "Ступеньки из Синего бетона");
        TRANSLATIONS.put("brown_concrete_stairs", "Ступеньки из Коричневого бетона");
        TRANSLATIONS.put("green_concrete_stairs", "Ступеньки из Зеленого бетона");
        TRANSLATIONS.put("red_concrete_stairs", "Ступеньки из Красного бетона");
        TRANSLATIONS.put("black_concrete_stairs", "Ступеньки из Черного бетона");

        // Concrete Slabs (16 colors)
        TRANSLATIONS.put("white_concrete_slab", "Плита из Белого бетона");
        TRANSLATIONS.put("orange_concrete_slab", "Плита из Оранжевого бетона");
        TRANSLATIONS.put("magenta_concrete_slab", "Плита из Пурпурного бетона");
        TRANSLATIONS.put("light_blue_concrete_slab", "Плита из Светло-синего бетона");
        TRANSLATIONS.put("yellow_concrete_slab", "Плита из Желтого бетона");
        TRANSLATIONS.put("lime_concrete_slab", "Плита из Лаймового бетона");
        TRANSLATIONS.put("pink_concrete_slab", "Плита из Розового бетона");
        TRANSLATIONS.put("gray_concrete_slab", "Плита из Серого бетона");
        TRANSLATIONS.put("light_gray_concrete_slab", "Плита из Светло-серого бетона");
        TRANSLATIONS.put("cyan_concrete_slab", "Плита из Бирюзового бетона");
        TRANSLATIONS.put("purple_concrete_slab", "Плита из Фиолетового бетона");
        TRANSLATIONS.put("blue_concrete_slab", "Плита из Синего бетона");
        TRANSLATIONS.put("brown_concrete_slab", "Плита из Коричневого бетона");
        TRANSLATIONS.put("green_concrete_slab", "Плита из Зеленого бетона");
        TRANSLATIONS.put("red_concrete_slab", "Плита из Красного бетона");
        TRANSLATIONS.put("black_concrete_slab", "Плита из Черного бетона");
    }

    @NotNull
    public static ItemStack createItem(@NotNull String customId, @NotNull Material baseMaterial, int amount) {
        ItemStack item = new ItemStack(baseMaterial, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(CBL_KEY, PersistentDataType.STRING, customId);
            pdc.set(MINECRAFT_CBL_KEY, PersistentDataType.STRING, customId);

            String formattedName = resolveDisplayName(customId);
            meta.displayName(Component.text(formattedName, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));

            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack createItem(@NotNull String customId, @NotNull Material baseMaterial) {
        return createItem(customId, baseMaterial, 1);
    }

    @NotNull
    public static ItemStack createItem(@NotNull String customId, int amount) {
        Material base = resolveBaseMaterial(customId);
        return createItem(customId, base, amount);
    }

    @NotNull
    public static ItemStack createItem(@NotNull String customId) {
        return createItem(customId, 1);
    }

    @Nullable
    public static String getCustomBlockId(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(CBL_KEY, PersistentDataType.STRING)) {
            return pdc.get(CBL_KEY, PersistentDataType.STRING);
        }
        if (pdc.has(MINECRAFT_CBL_KEY, PersistentDataType.STRING)) {
            return pdc.get(MINECRAFT_CBL_KEY, PersistentDataType.STRING);
        }

        try {
            String comp = meta.getAsComponentString();
            String found = extractCblId(comp);
            if (found != null) return found;

            String str = meta.getAsString();
            found = extractCblId(str);
            if (found != null) return found;
        } catch (Exception ignored) {}

        return null;
    }

    private static String extractCblId(String text) {
        if (text == null || !text.contains("cbl")) return null;
        int idx = text.indexOf("cbl");
        if (idx == -1) return null;
        int colon = text.indexOf(':', idx);
        if (colon == -1) return null;
        int start = colon + 1;
        while (start < text.length() && (text.charAt(start) == ' ' || text.charAt(start) == '"' || text.charAt(start) == '\'')) {
            start++;
        }
        int end = start;
        while (end < text.length() && text.charAt(end) != '"' && text.charAt(end) != '\'' && text.charAt(end) != ',' && text.charAt(end) != '}' && text.charAt(end) != ']') {
            end++;
        }
        if (start < end) {
            return text.substring(start, end).trim();
        }
        return null;
    }

    public static boolean isCustomBlock(@Nullable ItemStack item) {
        return getCustomBlockId(item) != null;
    }

    private static Material resolveBaseMaterial(String customId) {
        try {
            if (CustomBlockLayerPlugin.getInstance() != null && CustomBlockLayerPlugin.getInstance().getBlockRegistry() != null) {
                CustomBlockEntry entry = CustomBlockLayerPlugin.getInstance().getBlockRegistry().getBlock(customId);
                if (entry != null) {
                    return entry.getBaseMaterial();
                }
            }
        } catch (Exception ignored) {}

        String idLower = customId.toLowerCase();
        if (idLower.contains("stair")) return Material.QUARTZ_STAIRS;
        if (idLower.contains("slab")) return Material.QUARTZ_SLAB;
        if (idLower.contains("crate") || idLower.contains("plank")) return Material.OAK_PLANKS;
        if (idLower.contains("ore")) return Material.DEEPSLATE;
        return Material.STONE;
    }

    private static String resolveDisplayName(String id) {
        try {
            if (CustomBlockLayerPlugin.getInstance() != null && CustomBlockLayerPlugin.getInstance().getBlockRegistry() != null) {
                CustomBlockEntry entry = CustomBlockLayerPlugin.getInstance().getBlockRegistry().getBlock(id);
                if (entry != null && entry.getDisplayName() != null && !entry.getDisplayName().isEmpty()) {
                    return entry.getDisplayName();
                }
            }
        } catch (Exception ignored) {}

        if (TRANSLATIONS.containsKey(id)) {
            return TRANSLATIONS.get(id);
        }
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

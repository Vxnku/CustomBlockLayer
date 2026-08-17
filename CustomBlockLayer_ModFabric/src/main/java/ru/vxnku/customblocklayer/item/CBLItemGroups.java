package ru.vxnku.customblocklayer.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;
import ru.vxnku.customblocklayer.config.CustomBlockRegistry;
import ru.vxnku.customblocklayer.util.CBLItemHelper;

import java.lang.reflect.Field;
import java.util.*;

public class CBLItemGroups {
    private static final Logger LOGGER = LoggerFactory.getLogger("CBL-Tabs");
    private static final Map<String, RegistryKey<ItemGroup>> REGISTERED_TABS = new HashMap<>();

    // Standard Vanilla Dye Color Sorting Order
    private static final List<String> VANILLA_COLOR_ORDER = List.of(
        "white",
        "light_gray",
        "gray",
        "black",
        "brown",
        "red",
        "orange",
        "yellow",
        "lime",
        "green",
        "cyan",
        "light_blue",
        "blue",
        "purple",
        "magenta",
        "pink"
    );

    // Standard Wood Types Sorting Order
    private static final List<String> WOOD_ORDER = List.of(
        "oak",
        "spruce",
        "birch",
        "jungle",
        "acacia",
        "dark_oak",
        "mangrove",
        "cherry"
    );

    public static void init() {
        // Purely dynamic - root fallback tab only
        registerColumnTab("CustomBlockLayer");
    }

    public static synchronized void registerColumnTab(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            columnName = "CustomBlockLayer";
        }
        columnName = columnName.trim();

        if (REGISTERED_TABS.containsKey(columnName)) {
            return;
        }

        String safeId = toSafeId(columnName);
        RegistryKey<ItemGroup> tabKey = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of("customblocklayer", safeId));
        final String finalColumnName = columnName;

        ItemGroup tab = FabricItemGroup.builder()
            .icon(() -> {
                List<CustomBlockDefinition> defs = getSortedDefinitions(finalColumnName);
                if (!defs.isEmpty()) {
                    return CBLItemHelper.createCustomBlockItem(defs.get(0));
                }
                return new ItemStack(Items.CHISELED_STONE_BRICKS);
            })
            .displayName(Text.literal(finalColumnName))
            .entries((displayContext, entries) -> {
                List<CustomBlockDefinition> defs = getSortedDefinitions(finalColumnName);
                for (CustomBlockDefinition def : defs) {
                    entries.add(CBLItemHelper.createCustomBlockItem(def));
                }
            })
            .build();

        try {
            unfreezeAndRegister(tabKey, tab);
            REGISTERED_TABS.put(columnName, tabKey);
            LOGGER.info("[CBL-Tabs] Successfully registered dynamic Creative Tab: '{}' (key={})", finalColumnName, tabKey.getValue());
        } catch (Exception e) {
            LOGGER.error("[CBL-Tabs] Failed to register dynamic ItemGroup for column: " + finalColumnName, e);
        }
    }

    private static String toSafeId(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("цвет") || lower.contains("color")) return "colored_blocks";
        if (lower.contains("аним") || lower.contains("anim")) return "animations";
        if (lower.contains("раст") || lower.contains("plant") || lower.contains("flow")) return "plants";
        if (lower.contains("дек") || lower.contains("deco")) return "decor";
        if (lower.contains("древ") || lower.contains("wood") || lower.contains("дерев") || lower.contains("полноц") || lower.contains("bark")) return "bark_wood";
        if (lower.contains("custom") || lower.contains("main")) return "main";

        StringBuilder sb = new StringBuilder();
        for (char c : lower.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('_');
            } else {
                sb.append((int) c).append('_');
            }
        }
        String res = sb.toString();
        return res.isEmpty() ? "custom_tab" : res;
    }

    private static void unfreezeAndRegister(RegistryKey<ItemGroup> key, ItemGroup group) {
        try {
            Registry.register(Registries.ITEM_GROUP, key, group);
        } catch (IllegalStateException frozenEx) {
            try {
                Class<?> clazz = Registries.ITEM_GROUP.getClass();
                Field frozenField = null;
                while (clazz != null && clazz != Object.class) {
                    for (Field f : clazz.getDeclaredFields()) {
                        // Find the boolean field in SimpleRegistry (named 'frozen' in dev, or obfuscated at runtime)
                        if (f.getType() == boolean.class) {
                            frozenField = f;
                            break;
                        }
                    }
                    if (frozenField != null) break;
                    clazz = clazz.getSuperclass();
                }
                if (frozenField != null) {
                    frozenField.setAccessible(true);
                    frozenField.set(Registries.ITEM_GROUP, false);

                    Registry.register(Registries.ITEM_GROUP, key, group);

                    frozenField.set(Registries.ITEM_GROUP, true);
                    LOGGER.info("[CBL-Tabs] Successfully dynamically unfreezed and registered ItemGroup: {}", key.getValue());
                } else {
                    LOGGER.error("[CBL-Tabs] Could not find boolean 'frozen' field in hierarchy of {}", Registries.ITEM_GROUP.getClass());
                }
            } catch (Exception e) {
                LOGGER.error("[CBL-Tabs] Failed to unfreeze ItemGroup registry: ", e);
            }
        }
    }

    public static List<CustomBlockDefinition> getSortedDefinitions(String columnName) {
        List<CustomBlockDefinition> list = new ArrayList<>();
        boolean isMainTab = "CustomBlockLayer".equalsIgnoreCase(columnName);

        for (CustomBlockDefinition def : CustomBlockRegistry.getAllDefinitions()) {
            // Main tab contains ALL custom blocks so the player can always find any item in one place!
            if (isMainTab) {
                list.add(def);
            } else if (columnName.equalsIgnoreCase(def.getColumn())) {
                list.add(def);
            }
        }

        // Sort by: 1. Shape/Type, 2. Wood/Color Order, 3. Display Name
        list.sort(Comparator
            .comparingInt(CBLItemGroups::getShapePriority)
            .thenComparingInt(CBLItemGroups::getWoodOrColorPriority)
            .thenComparing(def -> def.getDisplayName() != null ? def.getDisplayName() : def.getId())
        );

        return list;
    }

    private static int getShapePriority(CustomBlockDefinition def) {
        String id = def.getId().toLowerCase();
        if (id.contains("stair")) return 1;
        if (id.contains("vertical_slab") || def.isVerticalSlab()) return 3;
        if (id.contains("slab")) return 2;
        if (id.contains("wall")) return 4;
        if (id.contains("fence_gate") || id.contains("gate")) return 5;
        if (id.contains("fence")) return 6;
        if (id.contains("plant") || id.contains("flower") || id.contains("rose")) return 7;
        if (id.contains("block") || id.contains("plank") || id.contains("concrete")) return 8;
        return 10;
    }

    private static int getWoodOrColorPriority(CustomBlockDefinition def) {
        String id = def.getId().toLowerCase();
        for (int i = 0; i < WOOD_ORDER.size(); i++) {
            String wood = WOOD_ORDER.get(i);
            if (id.startsWith(wood + "_") || id.contains("_" + wood + "_") || id.endsWith("_" + wood)) {
                return i;
            }
        }
        for (int i = 0; i < VANILLA_COLOR_ORDER.size(); i++) {
            String color = VANILLA_COLOR_ORDER.get(i);
            if (id.startsWith(color + "_") || id.contains("_" + color + "_") || id.endsWith("_" + color)) {
                return 100 + i;
            }
        }
        return 1000;
    }
}

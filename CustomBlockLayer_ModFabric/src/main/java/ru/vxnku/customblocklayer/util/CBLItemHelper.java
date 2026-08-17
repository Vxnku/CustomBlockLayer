package ru.vxnku.customblocklayer.util;

import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import ru.vxnku.customblocklayer.config.CustomBlockDefinition;

public class CBLItemHelper {

    public static ItemStack createCustomBlockItem(CustomBlockDefinition def) {
        Item baseItem = Items.STONE;
        if (!def.getMatchBlocks().isEmpty()) {
            Identifier firstMatch = def.getMatchBlocks().iterator().next();
            Block block = Registries.BLOCK.get(firstMatch);
            Item asItem = block.asItem();
            if (asItem != Items.AIR) {
                baseItem = asItem;
            }
        }
        return createCustomBlockItem(baseItem, def.getId(), def.getDisplayName());
    }

    public static ItemStack createCustomBlockItem(Item baseItem, String customBlockId, String displayName) {
        ItemStack stack = new ItemStack(baseItem != null ? baseItem : Items.STONE);
        applyCustomBlock(stack, customBlockId, displayName);
        return stack;
    }

    public static ItemStack createCustomBlockItem(String customBlockId) {
        return createCustomBlockItem(Items.STONE, customBlockId, null);
    }

    public static void applyCustomBlock(ItemStack stack, String customBlockId, String displayName) {
        // Match exact Paper PersistentDataContainer structure
        NbtCompound nbt = new NbtCompound();
        NbtCompound bukkitValues = new NbtCompound();
        bukkitValues.putString("customblocklayer:cbl", customBlockId);
        bukkitValues.putString("minecraft:cbl", customBlockId);
        nbt.put("PublicBukkitValues", bukkitValues);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        String formatted = (displayName != null && !displayName.isEmpty()) ? displayName : formatName(customBlockId);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(formatted).styled(style -> style.withColor(Formatting.YELLOW).withItalic(false)));
    }

    public static String getCustomBlockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return null;
        NbtCompound nbt = comp.copyNbt();
        
        if (nbt.contains("PublicBukkitValues")) {
            NbtCompound bv = nbt.getCompound("PublicBukkitValues");
            if (bv.contains("customblocklayer:cbl")) {
                return bv.getString("customblocklayer:cbl");
            }
            if (bv.contains("minecraft:cbl")) {
                return bv.getString("minecraft:cbl");
            }
        }
        if (nbt.contains("cbl")) {
            return nbt.getString("cbl");
        }
        return null;
    }

    public static boolean isCustomBlock(ItemStack stack) {
        return getCustomBlockId(stack) != null;
    }

    public static String formatName(String id) {
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

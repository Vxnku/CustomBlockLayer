package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.registry.CustomBlockEntry;
import ru.vxnku.customblocklayer.registry.ServerBlockRegistry;

import java.util.stream.Stream;

public class CBLPatternParser extends InputParser<Pattern> {

    public CBLPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public @Nullable Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input == null || input.trim().isEmpty()) return null;
        input = input.trim();

        String rawId = input;
        String statesStr = null;

        if (input.contains("[")) {
            int bracket = input.indexOf('[');
            rawId = input.substring(0, bracket);
            statesStr = input.substring(bracket);
        }

        if (rawId.startsWith("cbl:") || rawId.startsWith("customblocklayer:")) {
            rawId = rawId.substring(rawId.indexOf(':') + 1);
        } else {
            // Check if it's a known CBL block ID directly
            ServerBlockRegistry reg = CustomBlockLayerPlugin.getInstance().getBlockRegistry();
            if (reg == null || !reg.isKnown(rawId)) {
                return null;
            }
        }

        ServerBlockRegistry registry = CustomBlockLayerPlugin.getInstance().getBlockRegistry();
        if (registry == null) return null;

        CustomBlockEntry entry = registry.getBlock(rawId);
        if (entry != null) {
            BlockType blockType = BukkitAdapter.asBlockType(entry.getBaseMaterial());
            if (blockType == null) return null;

            BlockState state = blockType.getDefaultState();
            if (statesStr != null) {
                try {
                    BlockState parsedState = worldEdit.getBlockFactory().parseFromInput(blockType.getId() + statesStr, context).toImmutableState();
                    if (parsedState != null) {
                        state = parsedState;
                    }
                } catch (Exception ignored) {}
            }

            return new CBLPattern(entry, state);
        }

        return null;
    }

    @Override
    public Stream<String> getSuggestions(String input, ParserContext context) {
        ServerBlockRegistry registry = CustomBlockLayerPlugin.getInstance().getBlockRegistry();
        if (registry == null) return Stream.empty();

        return registry.getKnownIds().stream()
            .flatMap(id -> Stream.of("cbl:" + id, id))
            .filter(str -> str.toLowerCase().startsWith(input.toLowerCase()));
    }
}

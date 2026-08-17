package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.registry.InputParser;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.registry.ServerBlockRegistry;

import java.util.stream.Stream;

public class CBLMaskParser extends InputParser<Mask> {

    public CBLMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public @Nullable Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input == null || input.trim().isEmpty()) return null;
        input = input.trim();

        if (input.startsWith("cbl:") || input.startsWith("customblocklayer:")) {
            String targetId = input.substring(input.indexOf(':') + 1).trim();
            CBLMask mask = new CBLMask(targetId);
            if (context.getWorld() != null) {
                BukkitWorld bukkitWorld = BukkitAdapter.asBukkitWorld(context.getWorld());
                if (bukkitWorld != null) {
                    mask.setWorld(bukkitWorld.getWorld());
                }
            }
            return mask;
        }
        return null;
    }

    @Override
    public Stream<String> getSuggestions(String input, ParserContext context) {
        ServerBlockRegistry registry = CustomBlockLayerPlugin.getInstance().getBlockRegistry();
        if (registry == null) return Stream.empty();

        return registry.getKnownIds().stream()
            .map(id -> "cbl:" + id)
            .filter(str -> str.toLowerCase().startsWith(input.toLowerCase()));
    }
}

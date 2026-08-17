package ru.vxnku.customblocklayer.integration.worldedit;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

public class CBLWorldEditIntegration {

    public static void init(@NotNull CustomBlockLayerPlugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            plugin.getLogger().info("[CBL-WorldEdit] WorldEdit не найден, интеграция отключена.");
            return;
        }

        try {
            WorldEdit we = WorldEdit.getInstance();

            // 1. Register Pattern Parser (works in //set, //walls, //cyl, //sphere, //brush, etc.)
            we.getPatternFactory().register(new CBLPatternParser(we));

            // 2. Register Mask Parser (works in //replace cbl:id ...)
            we.getMaskFactory().register(new CBLMaskParser(we));

            // 3. Register EditSession Event Listener (intercepts all world edits)
            we.getEventBus().register(new CBLEditSessionListener());

            plugin.getLogger().info("[CBL-WorldEdit] Интеграция с WorldEdit успешно зарегистрирована (//set cbl:<id>, //replace, кисти, маски)!");
        } catch (Exception e) {
            plugin.getLogger().warning("[CBL-WorldEdit] Ошибка инициализации интеграции с WorldEdit: " + e.getMessage());
        }
    }
}

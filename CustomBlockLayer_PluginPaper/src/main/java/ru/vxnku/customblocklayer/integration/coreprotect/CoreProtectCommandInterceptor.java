package ru.vxnku.customblocklayer.integration.coreprotect;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.jetbrains.annotations.NotNull;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts CoreProtect rollback and restore commands to support
 * custom block filters (e.g. b:cbl/<id>, include:cbl/<id>, cbl/<id>)
 * and provides dynamic tab-completion from the server block registry.
 */
public class CoreProtectCommandInterceptor implements Listener {
    private final CustomBlockLayerPlugin plugin;
    private final CoreProtectRollbackHandler rollbackHandler;

    private static final Pattern TIME_PATTERN = Pattern.compile("(?i)(?:t|time):(\\d+)([smhdw])?");
    private static final Pattern RADIUS_PATTERN = Pattern.compile("(?i)(?:r|radius):(\\d+)");
    private static final Pattern USER_PATTERN = Pattern.compile("(?i)(?:u|user):([\\w#-]+)");

    public CoreProtectCommandInterceptor(@NotNull CustomBlockLayerPlugin plugin, @NotNull CoreProtectRollbackHandler rollbackHandler) {
        this.plugin = plugin;
        this.rollbackHandler = rollbackHandler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().trim();
        if (handleCommand(event.getPlayer(), msg)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        String msg = "/" + event.getCommand().trim();
        if (handleCommand(event.getSender(), msg)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;

        String raw = buffer.substring(1);
        String[] parts = raw.split("\\s+", -1);
        if (parts.length < 2) return;

        String root = parts[0].toLowerCase();
        String sub = parts[1].toLowerCase();

        boolean isCoreProtect = root.equals("co") || root.equals("core") || root.equals("coreprotect") || root.equals("cbl");
        boolean isRollbackOrRestore = sub.equals("rollback") || sub.equals("rb") || sub.equals("undo") || sub.equals("restore") || sub.equals("rs");

        if (!isCoreProtect || !isRollbackOrRestore) {
            return;
        }

        String lastArg = parts[parts.length - 1].toLowerCase();
        List<String> completions = new ArrayList<>(event.getCompletions());
        Set<String> knownIds = plugin.getBlockRegistry().getKnownIds();

        if (lastArg.startsWith("b:cbl/") || lastArg.startsWith("include:cbl/") || lastArg.startsWith("blocks:cbl/")) {
            String prefix = lastArg.substring(0, lastArg.indexOf("cbl/") + 4);
            String filter = lastArg.substring(prefix.length());
            for (String id : knownIds) {
                if (id.toLowerCase().startsWith(filter)) {
                    completions.add(prefix + id);
                }
            }
        } else if (lastArg.startsWith("cbl/")) {
            String filter = lastArg.substring(4);
            for (String id : knownIds) {
                if (id.toLowerCase().startsWith(filter)) {
                    completions.add("cbl/" + id);
                }
            }
        } else if (lastArg.startsWith("b:") || lastArg.startsWith("include:")) {
            String prefix = lastArg.startsWith("b:") ? "b:" : "include:";
            String filter = lastArg.substring(prefix.length());
            completions.add(prefix + "cbl/");
            for (String id : knownIds) {
                if (id.toLowerCase().startsWith(filter) || ("cbl/" + id).toLowerCase().startsWith(filter)) {
                    completions.add(prefix + "cbl/" + id);
                }
            }
        } else if (lastArg.isEmpty() || "cbl".startsWith(lastArg) || "cbl/".startsWith(lastArg)) {
            completions.add("cbl/");
            completions.add("b:cbl/");
            completions.add("include:cbl/");
        }

        if (!completions.isEmpty()) {
            event.setCompletions(completions);
        }
    }

    private boolean handleCommand(CommandSender sender, String commandLine) {
        if (!commandLine.startsWith("/")) return false;
        String[] parts = commandLine.substring(1).split("\\s+");
        if (parts.length < 2) return false;

        String root = parts[0].toLowerCase();
        String sub = parts[1].toLowerCase();

        boolean isCoreProtect = root.equals("co") || root.equals("core") || root.equals("coreprotect");
        boolean isCbl = root.equals("cbl");

        boolean isRollback = sub.equals("rollback") || sub.equals("rb") || sub.equals("undo");
        boolean isRestore = sub.equals("restore") || sub.equals("rs");

        if ((isCoreProtect || isCbl) && (isRollback || isRestore)) {
            // Check for custom block filter (cbl/<id>, cbl:<id>, b:cbl/..., include:cbl/..., etc.)
            String customBlockFilter = extractCustomBlockFilter(parts);
            boolean isExplicitRestore = isRestore || commandLine.contains("action:+block") || commandLine.contains("a:+block") || commandLine.contains("action:1");

            // If a custom block filter is present, CBL handles the selective rollback entirely
            if (customBlockFilter != null) {
                int timeSeconds = parseTimeSeconds(commandLine);
                int radius = parseRadius(commandLine);
                String userFilter = parseUser(parts);

                Location centerLoc;
                if (sender instanceof Player p) {
                    centerLoc = p.getLocation();
                } else {
                    centerLoc = Bukkit.getWorlds().get(0).getSpawnLocation();
                }

                rollbackHandler.executeSelectiveRollback(
                    sender, customBlockFilter, timeSeconds, radius, centerLoc, userFilter, isExplicitRestore
                );
                return true;
            }

            // If general CoreProtect rollback without custom filter, let CoreProtect run,
            // but schedule a sync pass to ensure any CBL blocks rolled back by CoreProtect have PDC restored
            if (isCoreProtect) {
                int radius = parseRadius(commandLine);
                int timeSeconds = parseTimeSeconds(commandLine);
                String userFilter = parseUser(parts);
                Location centerLoc = (sender instanceof Player p) ? p.getLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    rollbackHandler.executeSelectiveRollback(
                        sender, "*", timeSeconds, radius, centerLoc, userFilter, isExplicitRestore
                    );
                }, 4L);
            }
        }

        return false;
    }

    private String extractCustomBlockFilter(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            String arg = parts[i].trim();
            String lower = arg.toLowerCase();

            // Format 1: include:cbl/id, b:cbl/id, include:cbl:id, b:cbl:id
            if (lower.startsWith("b:") || lower.startsWith("include:") || lower.startsWith("blocks:")) {
                String subVal = arg.substring(arg.indexOf(':') + 1).trim();
                // Check if next token is the ID (e.g. "include: cbl/red_block")
                if (subVal.isEmpty() && i + 1 < parts.length) {
                    subVal = parts[i + 1].trim();
                }
                String extracted = normalizeCblId(subVal);
                if (extracted != null) return extracted;
            }

            // Format 2: standalone cbl/id or cbl:id
            String direct = normalizeCblId(arg);
            if (direct != null) {
                return direct;
            }
        }
        return null;
    }

    private String normalizeCblId(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        if (lower.startsWith("cbl/")) {
            return text.substring(4).trim();
        }
        if (lower.startsWith("cbl:")) {
            return text.substring(4).trim();
        }
        if (lower.equals("cbl") || lower.equals("cbl/*") || lower.equals("cbl:*")) {
            return "*";
        }
        return null;
    }

    private int parseTimeSeconds(String command) {
        Matcher m = TIME_PATTERN.matcher(command);
        if (m.find()) {
            int amount = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            if (unit == null || unit.equalsIgnoreCase("s")) return amount;
            if (unit.equalsIgnoreCase("m")) return amount * 60;
            if (unit.equalsIgnoreCase("h")) return amount * 3600;
            if (unit.equalsIgnoreCase("d")) return amount * 86400;
            if (unit.equalsIgnoreCase("w")) return amount * 604800;
        }
        return 3600 * 24 * 30; // Default 30 days if unspecified
    }

    private int parseRadius(String command) {
        Matcher m = RADIUS_PATTERN.matcher(command);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0; // 0 = global / current chunk or world
    }

    private String parseUser(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            String arg = parts[i].trim();
            if (arg.toLowerCase().startsWith("u:") || arg.toLowerCase().startsWith("user:")) {
                String val = arg.substring(arg.indexOf(':') + 1).trim();
                if (val.isEmpty() && i + 1 < parts.length) {
                    val = parts[i + 1].trim();
                }
                return val;
            }
        }
        Matcher m = USER_PATTERN.matcher(String.join(" ", parts));
        if (m.find()) {
            return m.group(1);
        }
        // Also check if second/third token is a username (e.g. /co rollback Vxnku ...)
        if (parts.length >= 3) {
            String candidate = parts[2];
            if (!candidate.contains(":") && !candidate.startsWith("/") && !candidate.startsWith("#")) {
                return candidate;
            }
        }
        return null;
    }
}

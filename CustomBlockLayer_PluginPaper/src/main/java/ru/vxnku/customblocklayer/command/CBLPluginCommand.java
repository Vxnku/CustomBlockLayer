package ru.vxnku.customblocklayer.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.vxnku.customblocklayer.CustomBlockLayerPlugin;
import ru.vxnku.customblocklayer.item.CBLItemFactory;

import java.util.ArrayList;
import java.util.List;

public class CBLPluginCommand implements CommandExecutor, TabCompleter {
    private final CustomBlockLayerPlugin plugin;

    public CBLPluginCommand(@NotNull CustomBlockLayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("--- CustomBlockLayer v" + plugin.getPluginMeta().getVersion() + " ---", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/cbl give <player> <customId> [amount]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/cbl set <customId>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/cbl remove", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/cbl reload", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give" -> {
                if (!sender.hasPermission("customblocklayer.admin")) {
                    sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Использование: /cbl give <игрок> <customId> [количество]", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Игрок не найден!", NamedTextColor.RED));
                    return true;
                }
                String customId = args[2];
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignored) {}
                }
                ItemStack item = CBLItemFactory.createItem(customId, amount <= 0 ? 1 : amount);
                target.getInventory().addItem(item);
                sender.sendMessage(Component.text("Выдано " + amount + "x " + customId + " игроку " + target.getName(), NamedTextColor.GREEN));
                return true;
            }

            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Только для игроков!", NamedTextColor.RED));
                    return true;
                }
                if (!player.hasPermission("customblocklayer.admin")) {
                    player.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Использование: /cbl set <customId>", NamedTextColor.RED));
                    return true;
                }
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || targetBlock.isEmpty()) {
                    player.sendMessage(Component.text("Вы должны смотреть на блок!", NamedTextColor.RED));
                    return true;
                }
                String customId = args[1];
                Location loc = targetBlock.getLocation();
                plugin.getStorageManager().getStorage().setBlock(loc, customId);
                plugin.getNetworkManager().sendSetBlock(loc, customId);
                player.sendMessage(Component.text("Установлен кастомный блок '" + customId + "' на позиции " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.GREEN));
                return true;
            }

            case "remove" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Только для игроков!", NamedTextColor.RED));
                    return true;
                }
                if (!player.hasPermission("customblocklayer.admin")) {
                    player.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED));
                    return true;
                }
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null || targetBlock.isEmpty()) {
                    player.sendMessage(Component.text("Вы должны смотреть на блок!", NamedTextColor.RED));
                    return true;
                }
                Location loc = targetBlock.getLocation();
                plugin.getStorageManager().getStorage().removeBlock(loc);
                plugin.getNetworkManager().sendClearBlock(loc);
                player.sendMessage(Component.text("Кастомный блок удален с позиции " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.YELLOW));
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("customblocklayer.admin")) {
                    sender.sendMessage(Component.text("У вас нет прав!", NamedTextColor.RED));
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(Component.text("Конфигурация CustomBlockLayer перезагружена!", NamedTextColor.GREEN));
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("give");
            list.add("set");
            list.add("remove");
            list.add("reload");
        } else if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }
        }
        return list;
    }
}

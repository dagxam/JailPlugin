package com.jail.command;

import com.jail.JailManager;
import com.jail.JailPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PrisonCommand implements CommandExecutor, TabCompleter {

    private final JailPlugin plugin;

    public PrisonCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        JailManager manager = plugin.getJailManager();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "jail": {
                // /prison jail <игрок> [минуты]
                if (args.length < 2) {
                    sender.sendMessage(JailManager.colorize("&c/prison jail <игрок> [минуты]"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    sender.sendMessage(JailManager.colorize(
                            manager.getMessage("admin-player-not-found")));
                    return true;
                }

                int seconds;
                if (args.length >= 3) {
                    try {
                        int minutes = Integer.parseInt(args[2]);
                        seconds = minutes * 60;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(JailManager.colorize(
                                "&cУкажите число минут!"));
                        return true;
                    }
                } else {
                    seconds = manager.getSentenceTime("default");
                }

                manager.jailPlayer(target, seconds, "арест администратором");

                // Дополнительное уведомление
                target.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-jail-notify")));

                sender.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-jailed")
                                .replace("%player%", target.getName())
                                .replace("%time%", manager.formatTimeWords(seconds))));
                return true;
            }

            case "release": {
                // /prison release <игрок>
                if (args.length < 2) {
                    sender.sendMessage(JailManager.colorize("&c/prison release <игрок>"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(JailManager.colorize(
                            manager.getMessage("admin-player-not-found")));
                    return true;
                }

                if (!manager.isJailed(target.getUniqueId())) {
                    sender.sendMessage(JailManager.colorize(
                            manager.getMessage("admin-not-jailed")));
                    return true;
                }

                manager.releasePlayer(target.getUniqueId());
                target.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-release-notify")));
                sender.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-released")
                                .replace("%player%", target.getName())));
                return true;
            }

            case "list": {
                // /prison list
                Map<UUID, Integer> all = manager.getAllPrisoners();

                sender.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-list-header")));

                if (all.isEmpty()) {
                    sender.sendMessage(JailManager.colorize(
                            manager.getMessage("admin-list-empty")));
                    return true;
                }

                for (Map.Entry<UUID, Integer> entry : all.entrySet()) {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null) name = entry.getKey().toString().substring(0, 8);
                    int mins = entry.getValue() / 60;

                    sender.sendMessage(JailManager.colorize(
                            manager.getMessage("admin-list-entry")
                                    .replace("%player%", name)
                                    .replace("%mins%", String.valueOf(mins))));
                }
                return true;
            }

            case "tp": {
                // /prison tp
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Только для игроков!");
                    return true;
                }
                Player player = (Player) sender;
                Location cell = manager.getRandomCell();
                player.teleport(cell);
                sender.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-tp")));
                return true;
            }

            case "reload": {
                // /prison reload
                manager.reloadSettings();
                sender.sendMessage(JailManager.colorize(
                        manager.getMessage("admin-reloaded")));
                return true;
            }

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(JailManager.colorize("&6&l⛓ Команды тюрьмы:"));
        sender.sendMessage(JailManager.colorize("&e/prison jail <игрок> [минуты] &7— посадить"));
        sender.sendMessage(JailManager.colorize("&e/prison release <игрок> &7— освободить"));
        sender.sendMessage(JailManager.colorize("&e/prison list &7— список заключённых"));
        sender.sendMessage(JailManager.colorize("&e/prison tp &7— телепорт в тюрьму"));
        sender.sendMessage(JailManager.colorize("&e/prison reload &7— перезагрузить конфиг"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(
                    Arrays.asList("jail", "release", "list", "tp", "reload"),
                    args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("jail")) {
                // Все онлайн-игроки
                return filterStartsWith(
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList()),
                        args[1]);
            }
            if (sub.equals("release")) {
                // Только заключённые
                return filterStartsWith(
                        plugin.getJailManager().getAllPrisoners().keySet().stream()
                                .map(uuid -> {
                                    Player p = Bukkit.getPlayer(uuid);
                                    return p != null ? p.getName() : null;
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()),
                        args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("jail")) {
            return Arrays.asList("1", "2", "3", "5", "10", "15", "30");
        }

        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PrisonCommand
        implements CommandExecutor, TabCompleter {

    private final JailPlugin plugin;


    public PrisonCommand(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (
                !sender.hasPermission(
                        "prison.admin"
                )
        ) {

            sender.sendMessage(
                    JailManager.colorize(
                            "&cУ вас нет прав."
                    )
            );

            return true;
        }


        JailManager manager =
                plugin.getJailManager();


        if (args.length == 0) {

            help(sender);

            return true;
        }


        switch (
                args[0].toLowerCase(
                        Locale.ROOT
                )
        ) {

            case "jail" ->
                    jail(
                            sender,
                            manager,
                            args
                    );

            case "release" ->
                    release(
                            sender,
                            manager,
                            args
                    );

            case "list" ->
                    list(
                            sender,
                            manager
                    );

            case "tp" ->
                    tp(
                            sender,
                            manager
                    );

            case "setcell" ->
                    setCell(
                            sender,
                            manager,
                            args
                    );

            case "setrelease" ->
                    setRelease(
                            sender,
                            manager
                    );

            case "cells" ->
                    cells(
                            sender,
                            manager
                    );

            case "removecell" ->
                    removeCell(
                            sender,
                            manager,
                            args
                    );

            case "reload" -> {

                manager.reloadSettings();

                sender.sendMessage(
                        JailManager.colorize(
                                manager.getMessage(
                                        "admin-reloaded"
                                )
                        )
                );
            }

            default ->
                    help(sender);
        }


        return true;
    }


    private void jail(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (args.length < 2) {

            sender.sendMessage(
                    JailManager.colorize(
                            "&c/prison jail <игрок> [минуты]"
                    )
            );

            return;
        }


        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );


        if (target == null) {

            sender.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "admin-player-not-found"
                            )
                    )
            );

            return;
        }


        int seconds =
                manager.getSentenceTime(
                        "default"
                );


        if (args.length >= 3) {

            try {

                seconds =
                        Math.max(
                                1,
                                Integer.parseInt(
                                        args[2]
                                )
                        ) * 60;

            } catch (NumberFormatException exception) {

                sender.sendMessage(
                        JailManager.colorize(
                                "&cУкажите количество минут числом."
                        )
                );

                return;
            }
        }


        manager.jailPlayer(
                target,
                seconds,
                "арест администратором"
        );


        target.sendMessage(
                JailManager.colorize(
                        manager.getMessage(
                                "admin-jail-notify"
                        )
                )
        );


        sender.sendMessage(
                JailManager.colorize(
                        manager
                                .getMessage(
                                        "admin-jailed"
                                )
                                .replace(
                                        "%player%",
                                        target.getName()
                                )
                                .replace(
                                        "%time%",
                                        manager.formatTimeWords(
                                                seconds
                                        )
                                )
                )
        );
    }


    private void release(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (args.length < 2) {

            sender.sendMessage(
                    JailManager.colorize(
                            "&c/prison release <игрок>"
                    )
            );

            return;
        }


        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );


        if (target == null) {

            sender.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "admin-player-not-found"
                            )
                    )
            );

            return;
        }


        if (
                !manager.isJailed(
                        target.getUniqueId()
                )
        ) {

            sender.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "admin-not-jailed"
                            )
                    )
            );

            return;
        }


        manager.releasePlayer(
                target.getUniqueId()
        );


        target.sendMessage(
                JailManager.colorize(
                        manager.getMessage(
                                "admin-release-notify"
                        )
                )
        );


        sender.sendMessage(
                JailManager.colorize(
                        manager
                                .getMessage(
                                        "admin-released"
                                )
                                .replace(
                                        "%player%",
                                        target.getName()
                                )
                )
        );
    }


    private void list(
            CommandSender sender,
            JailManager manager
    ) {

        sender.sendMessage(
                JailManager.colorize(
                        manager.getMessage(
                                "admin-list-header"
                        )
                )
        );


        if (
                manager
                        .getAllPrisoners()
                        .isEmpty()
        ) {

            sender.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "admin-list-empty"
                            )
                    )
            );

            return;
        }


        for (
                Map.Entry<UUID, Integer> entry :
                manager
                        .getAllPrisoners()
                        .entrySet()
        ) {

            String name =
                    Optional
                            .ofNullable(
                                    Bukkit
                                            .getOfflinePlayer(
                                                    entry.getKey()
                                            )
                                            .getName()
                            )
                            .orElse(
                                    entry
                                            .getKey()
                                            .toString()
                                            .substring(
                                                    0,
                                                    8
                                            )
                            );


            int time =
                    entry.getValue();


            sender.sendMessage(
                    JailManager.colorize(

                            manager
                                    .getMessage(
                                            "admin-list-entry"
                                    )

                                    .replace(
                                            "%player%",
                                            name
                                    )

                                    .replace(
                                            "%mins%",
                                            String.valueOf(
                                                    time / 60
                                            )
                                    )

                                    .replace(
                                            "%secs%",
                                            String.valueOf(
                                                    time % 60
                                            )
                                    )
                    )
            );
        }
    }


    private void tp(
            CommandSender sender,
            JailManager manager
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(
                    "Только для игроков."
            );

            return;
        }


        Location cell =
                manager.getRandomCell();


        if (cell == null) {

            sender.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "no-cells"
                            )
                    )
            );

            return;
        }


        player.teleport(
                cell
        );


        sender.sendMessage(
                JailManager.colorize(
                        manager.getMessage(
                                "admin-tp"
                        )
                )
        );
    }


    private void setCell(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(
                    "Только для игроков."
            );

            return;
        }


        String id;


        if (args.length >= 2) {

            id =
                    args[1];

        } else {

            id =
                    "cell-" +
                            (
                                    manager.getCellCount()
                                            + 1
                            );
        }


        manager.setCell(
                id,
                player.getLocation()
        );


        sender.sendMessage(
                JailManager.colorize(
                        manager
                                .getMessage(
                                        "cell-added"
                                )
                                .replace(
                                        "%cell%",
                                        id
                                )
                )
        );
    }


    private void setRelease(
            CommandSender sender,
            JailManager manager
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(
                    "Только для игроков."
            );

            return;
        }


        manager.setRelease(
                player.getLocation()
        );


        sender.sendMessage(
                JailManager.colorize(
                        manager.getMessage(
                                "release-set"
                        )
                )
        );
    }


    private void cells(
            CommandSender sender,
            JailManager manager
    ) {

        sender.sendMessage(
                JailManager.colorize(
                        manager
                                .getMessage(
                                        "cell-list"
                                )
                                .replace(
                                        "%count%",
                                        String.valueOf(
                                                manager.getCellCount()
                                        )
                                )
                )
        );


        for (
                String id :
                manager.getCellIds()
        ) {

            sender.sendMessage(
                    JailManager.colorize(
                            "&7- &f" + id
                    )
            );
        }
    }


    private void removeCell(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (args.length < 2) {

            sender.sendMessage(
                    JailManager.colorize(
                            "&c/prison removecell <название>"
                    )
            );

            return;
        }


        if (
                !manager.removeCell(
                        args[1]
                )
        ) {

            sender.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "cell-not-found"
                            )
                    )
            );

            return;
        }


        sender.sendMessage(
                JailManager.colorize(
                        manager
                                .getMessage(
                                        "cell-removed"
                                )
                                .replace(
                                        "%cell%",
                                        args[1]
                                )
                )
        );
    }


    private void help(
            CommandSender sender
    ) {

        sender.sendMessage(
                JailManager.colorize(
                        "&6&l⛓ Управление тюрьмой"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison jail <игрок> [минуты] &7— посадить"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison release <игрок> &7— освободить"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison list &7— заключённые"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison tp &7— телепорт в камеру"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison setcell [название] &7— создать камеру"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison setrelease &7— точка освобождения"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison cells &7— список камер"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison removecell <название> &7— удалить камеру"
                )
        );

        sender.sendMessage(
                JailManager.colorize(
                        "&e/prison reload &7— перезагрузить конфигурацию"
                )
        );
    }


    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (args.length == 1) {

            return filter(

                    List.of(
                            "jail",
                            "release",
                            "list",
                            "tp",
                            "setcell",
                            "setrelease",
                            "cells",
                            "removecell",
                            "reload"
                    ),

                    args[0]
            );
        }


        if (
                args.length == 2
                        && args[0].equalsIgnoreCase(
                        "jail"
                )
        ) {

            return filter(

                    Bukkit
                            .getOnlinePlayers()
                            .stream()
                            .map(
                                    Player::getName
                            )
                            .toList(),

                    args[1]
            );
        }


        if (
                args.length == 2
                        && args[0].equalsIgnoreCase(
                        "release"
                )
        ) {

            return filter(
                    managerNames(),
                    args[1]
            );
        }


        if (
                args.length == 2
                        && args[0].equalsIgnoreCase(
                        "removecell"
                )
        ) {

            return filter(

                    plugin
                            .getJailManager()
                            .getCellIds()
                            .stream()
                            .toList(),

                    args[1]
            );
        }


        return Collections.emptyList();
    }


    private List<String> managerNames() {

        return plugin
                .getJailManager()
                .getAllPrisoners()
                .keySet()
                .stream()

                .map(
                        Bukkit::getPlayer
                )

                .filter(
                        Objects::nonNull
                )

                .map(
                        Player::getName
                )

                .collect(
                        Collectors.toList()
                );
    }


    private List<String> filter(
            List<String> values,
            String input
    ) {

        String lower =
                input.toLowerCase(
                        Locale.ROOT
                );


        return values
                .stream()

                .filter(
                        value ->
                                value
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                                        .startsWith(
                                                lower
                                        )
                )

                .toList();
    }
}

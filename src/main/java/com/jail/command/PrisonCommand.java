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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Главная команда управления тюрьмой.
 *
 * Использование:
 *
 * /prison
 *
 * Доступные команды:
 *
 * /prison jail <игрок> [минуты]
 * /prison release <игрок>
 * /prison list
 * /prison tp
 * /prison setcell [название]
 * /prison setrelease
 * /prison cells
 * /prison removecell <название>
 * /prison reload
 */
public final class PrisonCommand
        implements CommandExecutor, TabCompleter {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    public PrisonCommand(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Обработка команды /prison.
     */
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        JailManager manager =
                plugin.getJailManager();


        /*
         * Проверяем права администратора.
         */

        if (
                !sender.hasPermission(
                        "prison.admin"
                )
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "no-permission"
                            )
                    )
            );


            return true;
        }


        /*
         * Если команда введена без аргументов,
         * показываем справку.
         */

        if (args.length == 0) {

            help(sender);

            return true;
        }


        /*
         * Определяем подкоманду.
         */

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

                        JailManager.component(

                                manager.getMessage(
                                        "admin-reloaded"
                                )
                        )
                );
            }


            case "entityjail" -> {

                if (
                        !(sender instanceof Player player)
                ) {

                    sender.sendMessage(

                            JailManager.component(

                                    manager.getMessage(
                                            "player-only"
                                    )
                            )
                    );

                    return true;
                }

                plugin
                        .getEntityJailListener()
                        .startSelection(
                                player
                        );
            }


            default ->

                    help(
                            sender
                    );
        }


        return true;
    }


    /**
     * /prison jail <игрок> [минуты]
     *
     * Заключает игрока в тюрьму.
     */
    private void jail(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        /*
         * Проверяем наличие имени игрока.
         */

        if (args.length < 2) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "usage-jail"
                            )
                    )
            );


            return;
        }


        /*
         * Ищем игрока.
         */

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );


        if (target == null) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "admin-player-not-found"
                            )
                    )
            );


            return;
        }


        /*
         * По умолчанию используется
         * срок из sentences.default.
         */

        int seconds =
                manager.getSentenceTime(
                        "default"
                );


        /*
         * Если указаны минуты,
         * используем указанный срок.
         */

        if (args.length >= 3) {

            try {

                int minutes =
                        Integer.parseInt(
                                args[2]
                        );


                if (minutes <= 0) {

                    sender.sendMessage(

                            JailManager.component(

                                    manager.getMessage(
                                            "invalid-number"
                                    )
                            )
                    );


                    return;
                }


                seconds =
                        minutes * 60;


            } catch (
                    NumberFormatException exception
            ) {

                sender.sendMessage(

                        JailManager.component(

                                manager.getMessage(
                                        "invalid-number"
                                )
                        )
                );


                return;
            }
        }


        /*
         * Заключаем игрока.
         *
         * Причина тоже будет русской.
         */

        manager.jailPlayer(

                target,

                seconds,

                "заключение администратором"
        );


        /*
         * Сообщение игроку.
         */

        target.sendMessage(

                JailManager.component(

                        manager.getMessage(
                                "admin-jail-notify"
                        )
                )
        );


        /*
         * Сообщение администратору.
         */

        sender.sendMessage(

                JailManager.component(

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
                                        manager.formatTime(
                                                seconds
                                        )
                                )
                )
        );
    }


    /**
     * /prison release <игрок>
     *
     * Освобождает игрока.
     */
    private void release(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (args.length < 2) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "usage-release"
                            )
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

                    JailManager.component(

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

                    JailManager.component(

                            manager.getMessage(
                                    "admin-not-jailed"
                            )
                    )
            );


            return;
        }


        /*
         * Освобождаем игрока.
         */

        manager.releasePlayer(

                target.getUniqueId()

        );


        /*
         * Дополнительное уведомление игрока.
         */

        target.sendMessage(

                JailManager.component(

                        manager.getMessage(
                                "admin-release-notify"
                        )
                )
        );


        /*
         * Сообщение администратору.
         */

        sender.sendMessage(

                JailManager.component(

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


    /**
     * /prison list
     *
     * Показывает список заключённых.
     */
    private void list(
            CommandSender sender,
            JailManager manager
    ) {

        sender.sendMessage(

                JailManager.component(

                        manager.getMessage(
                                "admin-list-header"
                        )
                )
        );


        /*
         * Если заключённых нет.
         */

        if (
                manager
                        .getAllPrisoners()
                        .isEmpty()
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "admin-list-empty"
                            )
                    )
            );


            return;
        }


        /*
         * Выводим каждого заключённого.
         */

        for (
                Map.Entry<UUID, Integer> entry :
                manager
                        .getAllPrisoners()
                        .entrySet()
        ) {

            UUID uuid =
                    entry.getKey();


            int time =
                    entry.getValue();


            String name =
                    Optional
                            .ofNullable(

                                    Bukkit
                                            .getOfflinePlayer(
                                                    uuid
                                            )
                                            .getName()

                            )
                            .orElse(

                                    uuid
                                            .toString()
                                            .substring(
                                                    0,
                                                    8
                                            )
                            );


            String formattedTime =
                    manager.formatTime(
                            time
                    );


            sender.sendMessage(

                    JailManager.component(

                            manager
                                    .getMessage(
                                            "admin-list-entry"
                                    )

                                    .replace(
                                            "%player%",
                                            name
                                    )

                                    .replace(
                                            "%time%",
                                            formattedTime
                                    )
                    )
            );
        }
    }


    /**
     * /prison tp
     *
     * Телепортирует администратора
     * в случайную камеру.
     */
    private void tp(
            CommandSender sender,
            JailManager manager
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "player-only"
                            )
                    )
            );


            return;
        }


        Location cell =
                manager.getRandomCell();


        if (cell == null) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "no-cells"
                            )
                    )
            );


            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "no-cells-hint"
                            )
                    )
            );


            return;
        }


        player.teleport(
                cell
        );


        sender.sendMessage(

                JailManager.component(

                        manager.getMessage(
                                "admin-tp"
                        )
                )
        );
    }


    /**
     * /prison setcell [название]
     *
     * Создаёт камеру в текущем месте.
     */
    private void setCell(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "player-only"
                            )
                    )
            );


            return;
        }


        String id;


        /*
         * Если название указано,
         * используем его.
         */

        if (args.length >= 2) {

            id =
                    args[1];

        } else {

            /*
             * Автоматическое название:
             *
             * cell-1
             * cell-2
             * cell-3
             */

            id =
                    "cell-" +
                            (
                                    manager.getCellCount()
                                            + 1
                            );
        }


        /*
         * Создаём камеру.
         */

        boolean success =
                manager.setCell(
                        id,
                        player.getLocation()
                );


        if (!success) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "cell-create-error"
                            )
                    )
            );


            return;
        }


        sender.sendMessage(

                JailManager.component(

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


    /**
     * /prison setrelease
     *
     * Устанавливает точку освобождения.
     */
    private void setRelease(
            CommandSender sender,
            JailManager manager
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "player-only"
                            )
                    )
            );


            return;
        }


        manager.setRelease(
                player.getLocation()
        );


        sender.sendMessage(

                JailManager.component(

                        manager.getMessage(
                                "release-set"
                        )
                )
        );
    }


    /**
     * /prison cells
     *
     * Показывает все камеры.
     */
    private void cells(
            CommandSender sender,
            JailManager manager
    ) {

        sender.sendMessage(

                JailManager.component(

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


        if (
                manager.getCellCount() == 0
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "no-cells-hint"
                            )
                    )
            );


            return;
        }


        for (
                String id :
                manager.getCellIds()
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager
                                    .getMessage(
                                            "cell-list-entry"
                                    )

                                    .replace(
                                            "%cell%",
                                            id
                                    )
                    )
            );
        }
    }


    /**
     * /prison removecell <название>
     *
     * Удаляет камеру.
     */
    private void removeCell(
            CommandSender sender,
            JailManager manager,
            String[] args
    ) {

        if (args.length < 2) {

            sender.sendMessage(

                    JailManager.component(

                            manager.getMessage(
                                    "usage-removecell"
                            )
                    )
            );


            return;
        }


        String id =
                args[1];


        if (
                !manager.removeCell(
                        id
                )
        ) {

            sender.sendMessage(

                    JailManager.component(

                            manager
                                    .getMessage(
                                            "cell-not-found"
                                    )

                                    .replace(
                                            "%cell%",
                                            id
                                    )
                    )
            );


            return;
        }


        sender.sendMessage(

                JailManager.component(

                        manager
                                .getMessage(
                                        "cell-removed"
                                )

                                .replace(
                                        "%cell%",
                                        id
                                )
                )
        );
    }


    /**
     * Показывает справку по /prison.
     */
    private void help(
            CommandSender sender
    ) {

        sender.sendMessage("");


        sender.sendMessage(

                JailManager.component(

                        "&6&l⛓ Система тюрьмы"
                )
        );


        sender.sendMessage("");


        sender.sendMessage(

                JailManager.component(

                        "&e/prison jail <игрок> [минуты]"
                                +
                                " &7— заключить игрока"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison release <игрок>"
                                +
                                " &7— освободить игрока"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison list"
                                +
                                " &7— список заключённых"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison tp"
                                +
                                " &7— телепортироваться в камеру"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison setcell [название]"
                                +
                                " &7— создать камеру"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison setrelease"
                                +
                                " &7— установить точку освобождения"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison cells"
                                +
                                " &7— список камер"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison removecell <название>"
                                +
                                " &7— удалить камеру"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison reload"
                                +
                                " &7— перезагрузить настройки"
                )
        );


        sender.sendMessage(

                JailManager.component(

                        "&e/prison entityjail"
                                +
                                " &7— выбрать сущность и заключить её в тюрьму"
                )
        );


        sender.sendMessage("");
    }


    /**
     * Автодополнение команды /prison.
     */
    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        /*
         * Первый аргумент.
         */

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
                            "reload",
                            "entityjail"

                    ),

                    args[0]
            );
        }


        /*
         * Игрок для /prison jail.
         */

        if (
                args.length == 2

                        &&

                args[0].equalsIgnoreCase(
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


        /*
         * Игрок для /prison release.
         */

        if (
                args.length == 2

                        &&

                args[0].equalsIgnoreCase(
                        "release"
                )
        ) {

            return filter(

                    prisonerNames(),

                    args[1]
            );
        }


        /*
         * Камера для /prison removecell.
         */

        if (
                args.length == 2

                        &&

                args[0].equalsIgnoreCase(
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


    /**
     * Возвращает имена заключённых,
     * находящихся онлайн.
     */
    private List<String> prisonerNames() {

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


    /**
     * Фильтрует варианты автодополнения.
     */
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

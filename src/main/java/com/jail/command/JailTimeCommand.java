package com.jail.command;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;


/**
 * Команда просмотра оставшегося срока.
 *
 * Использование:
 *
 * /jailtime
 *
 * Также доступен русский алиас:
 *
 * /срок
 */
public final class JailTimeCommand
        implements CommandExecutor {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    public JailTimeCommand(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Обработка команды /jailtime.
     */
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        /*
         * Команда предназначена для игроков.
         */

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(
                    JailManager.component(
                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "player-only"
                                    )
                    )
            );

            return true;
        }


        JailManager manager =
                plugin.getJailManager();


        /*
         * Проверяем, находится ли игрок
         * в тюрьме.
         */

        if (
                !manager.isJailed(
                        player.getUniqueId()
                )
        ) {

            player.sendMessage(
                    JailManager.component(
                            manager.getMessage(
                                    "jailtime-free"
                            )
                    )
            );

            return true;
        }


        /*
         * Получаем оставшееся время.
         */

        int time =
                manager.getTimeRemaining(
                        player.getUniqueId()
                );


        /*
         * Форматируем время по-русски.
         *
         * Например:
         *
         * 1 минута
         * 2 минуты 15 секунд
         * 5 минут
         */

        String formattedTime =
                manager.formatTime(
                        time
                );


        /*
         * Отправляем игроку сообщение.
         */

        player.sendMessage(

                JailManager.component(

                        manager
                                .getMessage(
                                        "jailtime-remaining"
                                )

                                .replace(
                                        "%time%",
                                        formattedTime
                                )
                )
        );


        return true;
    }
}

package com.jail.command;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;


/**
 * Команда просмотра текущих координат игрока.
 *
 * Использование:
 *
 * /getcoords
 */
public final class GetCoordsCommand
        implements CommandExecutor {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    /**
     * Создаёт обработчик команды.
     *
     * @param plugin главный класс плагина
     */
    public GetCoordsCommand(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Обработка команды /getcoords.
     */
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        /*
         * Команда доступна только игрокам.
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


        /*
         * Получаем данные игрока.
         */

        String world =
                player
                        .getWorld()
                        .getName();


        double x =
                player.getX();


        double y =
                player.getY();


        double z =
                player.getZ();


        float yaw =
                player.getYaw();


        float pitch =
                player.getPitch();


        /*
         * Заголовок.
         */

        player.sendMessage("");

        player.sendMessage(
                JailManager.component(
                        "&6&l📍 Ваши координаты"
                )
        );

        player.sendMessage("");


        /*
         * Мир.
         */

        player.sendMessage(

                JailManager.component(

                        "&eМир: &f" +
                                world
                )
        );


        /*
         * Координата X.
         */

        player.sendMessage(

                JailManager.component(

                        "&eX: &f" +
                                String.format(
                                        "%.2f",
                                        x
                                )
                )
        );


        /*
         * Координата Y.
         */

        player.sendMessage(

                JailManager.component(

                        "&eY: &f" +
                                String.format(
                                        "%.2f",
                                        y
                                )
                )
        );


        /*
         * Координата Z.
         */

        player.sendMessage(

                JailManager.component(

                        "&eZ: &f" +
                                String.format(
                                        "%.2f",
                                        z
                                )
                )
        );


        /*
         * Направление взгляда.
         */

        player.sendMessage(

                JailManager.component(

                        "&eПоворот: &f" +
                                String.format(
                                        "%.2f",
                                        yaw
                                )
                )
        );


        /*
         * Вертикальный угол взгляда.
         */

        player.sendMessage(

                JailManager.component(

                        "&eНаклон: &f" +
                                String.format(
                                        "%.2f",
                                        pitch
                                )
                )
        );


        player.sendMessage("");


        return true;
    }
}

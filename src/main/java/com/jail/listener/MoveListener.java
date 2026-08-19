package com.jail.listener;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;


/**
 * Контролирует перемещение заключённых.
 *
 * Заключённый не может покинуть
 * установленный радиус своей камеры.
 *
 * Радиус задаётся в config.yml:
 *
 * cell-radius: 3.0
 */
public final class MoveListener
        implements Listener {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    public MoveListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Обрабатывает перемещение игрока.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onMove(
            PlayerMoveEvent event
    ) {

        Player player =
                event.getPlayer();


        JailManager manager =
                plugin.getJailManager();


        /*
         * Если игрок не находится
         * в тюрьме — ничего не делаем.
         */

        if (
                !manager.isJailed(
                        player.getUniqueId()
                )
        ) {

            return;
        }


        /*
         * Если игрок просто поворачивает голову,
         * координаты не изменились.
         *
         * В этом случае нет необходимости
         * выполнять дополнительные проверки.
         */

        if (
                event.getFrom().getX()
                        ==
                        event.getTo().getX()

                        &&

                event.getFrom().getY()
                        ==
                        event.getTo().getY()

                        &&

                event.getFrom().getZ()
                        ==
                        event.getTo().getZ()
        ) {

            return;
        }


        /*
         * Получаем центр камеры.
         */

        Location cell =
                manager.getCellLocation(
                        player.getUniqueId()
                );


        /*
         * Если камера не найдена,
         * движение не блокируем.
         */

        if (
                cell == null
                        ||
                event.getTo() == null
        ) {

            return;
        }


        Location to =
                event.getTo();


        /*
         * Если игрок каким-либо образом
         * пытается перейти в другой мир,
         * возвращаем его в камеру.
         */

        if (
                to.getWorld() == null
                        ||
                cell.getWorld() == null
                        ||
                !to.getWorld().equals(
                        cell.getWorld()
                )
        ) {

            event.setTo(
                    cell
            );


            player.sendActionBar(

                    JailManager.component(

                            manager.getMessage(
                                    "movement-blocked"
                            )
                    )
            );


            return;
        }


        /*
         * Получаем максимальный радиус камеры.
         */

        double radius =
                manager.getCellRadius();


        /*
         * Используем квадрат расстояния,
         * чтобы не выполнять лишний
         * квадратный корень.
         */

        double maxDistanceSquared =
                radius * radius;


        double currentDistanceSquared =
                to.distanceSquared(
                        cell
                );


        /*
         * Игрок вышел за пределы камеры.
         */

        if (
                currentDistanceSquared
                        >
                        maxDistanceSquared
        ) {

            /*
             * Возвращаем его
             * в центр камеры.
             */

            event.setTo(
                    cell
            );


            /*
             * Показываем сообщение.
             *
             * Текст находится в config.yml.
             */

            player.sendActionBar(

                    JailManager.component(

                            manager.getMessage(
                                    "movement-blocked"
                            )
                    )
            );
        }
    }
}

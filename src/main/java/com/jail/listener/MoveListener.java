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
         * Получаем центр камеры.
         */

        Location cell =
                manager.getCellLocation(
                        player.getUniqueId()
                );


        /*
         * Если камера не найдена,
         * не блокируем движение.
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
         * оказался в другом мире,
         * возвращаем его в камеру.
         */

        if (
                to.getWorld() == null
                        ||
                !to.getWorld().equals(
                        cell.getWorld()
                )
        ) {

            event.setTo(
                    cell
            );

            return;
        }


        /*
         * Получаем максимальный радиус.
         */

        double radius =
                manager.getCellRadius();


        /*
         * Сравниваем расстояние.
         *
         * Используем distanceSquared,
         * чтобы не вычислять квадратный корень
         * каждый тик движения.
         */

        double maxDistanceSquared =
                radius * radius;


        double currentDistanceSquared =
                to.distanceSquared(
                        cell
                );


        /*
         * Если игрок вышел за пределы камеры,
         * возвращаем его обратно.
         */

        if (
                currentDistanceSquared
                        >
                        maxDistanceSquared
        ) {

            /*
             * Отменяем движение.
             */

            event.setTo(
                    cell
            );


            /*
             * Показываем предупреждение.
             *
             * Чтобы сообщение не отправлялось
             * десятки раз подряд при движении,
             * оно показывается только при фактическом
             * выходе за пределы.
             */

            player.sendActionBar(

                    JailManager.component(

                            "&c⛓ Вы не можете покинуть свою камеру!"
                    )
            );
        }
    }
}

package com.jail.listener;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;


/**
 * Обработчик подключения и возрождения игроков.
 *
 * Отвечает за:
 *
 * - возвращение заключённого в камеру после входа;
 * - возвращение заключённого в камеру после смерти;
 * - восстановление положения заключённого.
 */
public final class ConnectionListener
        implements Listener {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    public ConnectionListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Игрок вошёл на сервер.
     *
     * Если игрок находится в тюрьме,
     * через небольшой промежуток времени
     * возвращаем его в назначенную камеру.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onJoin(
            PlayerJoinEvent event
    ) {

        Player player =
                event.getPlayer();


        UUID uuid =
                player.getUniqueId();


        /*
         * Небольшая задержка нужна для того,
         * чтобы мир игрока и его состояние
         * полностью загрузились.
         */

        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,

                        () -> {

                            /*
                             * Игрок мог выйти за это время.
                             */

                            if (!player.isOnline()) {

                                return;
                            }


                            JailManager manager =
                                    plugin.getJailManager();


                            /*
                             * Проверяем,
                             * находится ли игрок в тюрьме.
                             */

                            if (
                                    !manager.isJailed(
                                            uuid
                                    )
                            ) {

                                return;
                            }


                            /*
                             * Получаем камеру.
                             */

                            Location cell =
                                    manager.getCellLocation(
                                            uuid
                                    );


                            /*
                             * Возвращаем игрока
                             * в камеру.
                             */

                            if (cell != null) {

                                player.teleport(
                                        cell
                                );
                            }


                            /*
                             * Получаем оставшееся время.
                             */

                            int time =
                                    manager.getTimeRemaining(
                                            uuid
                                    );


                            /*
                             * Форматируем время
                             * по-русски.
                             */

                            String formattedTime =
                                    manager.formatTime(
                                            time
                                    );


                            /*
                             * Сообщаем игроку,
                             * что он всё ещё заключён.
                             */

                            player.sendMessage("");


                            player.sendMessage(

                                    JailManager.component(

                                            manager.getMessage(
                                                    "join-still-jailed"
                                            )
                                    )
                            );


                            player.sendMessage(

                                    JailManager.component(

                                            manager
                                                    .getMessage(
                                                            "join-still-jailed-time"
                                                    )

                                                    .replace(
                                                            "%time%",
                                                            formattedTime
                                                    )
                                    )
                            );


                            player.sendMessage("");
                        },

                        2L
                );
    }


    /**
     * Игрок возродился.
     *
     * Если он заключён,
     * точка возрождения устанавливается
     * в его камере.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onRespawn(
            PlayerRespawnEvent event
    ) {

        Player player =
                event.getPlayer();


        UUID uuid =
                player.getUniqueId();


        JailManager manager =
                plugin.getJailManager();


        /*
         * Если игрок не заключён,
         * ничего не делаем.
         */

        if (
                !manager.isJailed(
                        uuid
                )
        ) {

            return;
        }


        /*
         * Получаем камеру.
         */

        Location cell =
                manager.getCellLocation(
                        uuid
                );


        if (cell == null) {

            return;
        }


        /*
         * Устанавливаем камеру
         * как точку возрождения.
         */

        event.setRespawnLocation(
                cell
        );


        /*
         * Дополнительно телепортируем игрока
         * после завершения процесса возрождения.
         *
         * Это помогает избежать ситуаций,
         * когда другой плагин изменяет точку
         * возрождения игрока.
         */

        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,

                        () -> {

                            if (
                                    !player.isOnline()
                                            ||
                                    !manager.isJailed(
                                            uuid
                                    )
                            ) {

                                return;
                            }


                            Location currentCell =
                                    manager.getCellLocation(
                                            uuid
                                    );


                            if (currentCell != null) {

                                player.teleport(
                                        currentCell
                                );
                            }

                        },

                        2L
                );
    }
}

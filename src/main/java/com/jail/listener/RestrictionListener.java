package com.jail.listener;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.List;


/**
 * Ограничения для заключённых.
 *
 * В тюрьме запрещается:
 *
 * - ломать блоки;
 * - устанавливать блоки;
 * - атаковать сущностей;
 * - выбрасывать предметы;
 * - подбирать предметы;
 * - использовать запрещённые команды;
 * - изменять инвентарь.
 *
 * Администраторы с правом prison.admin
 * освобождаются от этих ограничений.
 */
public final class RestrictionListener
        implements Listener {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    public RestrictionListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Проверяет, находится ли игрок
     * в тюрьме.
     */
    private boolean jailed(
            Player player
    ) {

        return plugin
                .getJailManager()
                .isJailed(
                        player.getUniqueId()
                );
    }


    /**
     * Проверяет, является ли игрок
     * администратором.
     */
    private boolean admin(
            Player player
    ) {

        return player.hasPermission(
                "prison.admin"
        );
    }


    /**
     * Запрет разрушения блоков.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onBreak(
            BlockBreakEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !jailed(player)
                        ||
                admin(player)
        ) {

            return;
        }


        event.setCancelled(
                true
        );


        player.sendActionBar(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "block-break-blocked"
                                )
                )
        );
    }


    /**
     * Запрет установки блоков.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onPlace(
            BlockPlaceEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !jailed(player)
                        ||
                admin(player)
        ) {

            return;
        }


        event.setCancelled(
                true
        );


        player.sendActionBar(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "block-place-blocked"
                                )
                )
        );
    }


    /**
     * Запрет атаки других сущностей.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onDamage(
            EntityDamageByEntityEvent event
    ) {

        /*
         * Проверяем только прямого атакующего.
         */

        if (
                event.getDamager()
                        instanceof Player player
        ) {

            if (
                    jailed(player)
                            &&
                    !admin(player)
            ) {

                event.setCancelled(
                        true
                );


                player.sendActionBar(

                        JailManager.component(

                                plugin
                                        .getJailManager()
                                        .getMessage(
                                                "damage-blocked"
                                        )
                        )
                );
            }
        }
    }


    /**
     * Запрет выбрасывания предметов.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onDrop(
            PlayerDropItemEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !jailed(player)
                        ||
                admin(player)
        ) {

            return;
        }


        event.setCancelled(
                true
        );


        player.sendActionBar(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "drop-blocked"
                                )
                )
        );
    }


    /**
     * Запрет подбора предметов.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onPickup(
            EntityPickupItemEvent event
    ) {

        /*
         * Нас интересует только игрок.
         */

        if (
                !(event.getEntity()
                        instanceof Player player)
        ) {

            return;
        }


        if (
                !jailed(player)
                        ||
                admin(player)
        ) {

            return;
        }


        event.setCancelled(
                true
        );


        player.sendActionBar(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "pickup-blocked"
                                )
                )
        );
    }


    /**
     * Запрет изменения инвентаря.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onInventory(
            InventoryClickEvent event
    ) {

        /*
         * Проверяем, что действие
         * выполняет игрок.
         */

        if (
                !(event.getWhoClicked()
                        instanceof Player player)
        ) {

            return;
        }


        if (
                !jailed(player)
                        ||
                admin(player)
        ) {

            return;
        }


        /*
         * Полностью запрещаем
         * изменение инвентаря.
         */

        event.setCancelled(
                true
        );
    }


    /**
     * Запрет использования команд
     * заключённым.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onCommand(
            PlayerCommandPreprocessEvent event
    ) {

        Player player =
                event.getPlayer();


        /*
         * Если игрок свободен —
         * ничего не делаем.
         */

        if (
                !jailed(player)
        ) {

            return;
        }


        /*
         * Администратор может
         * использовать команды.
         */

        if (
                admin(player)
        ) {

            return;
        }


        String raw =
                event.getMessage();


        /*
         * Если это не команда,
         * ничего не делаем.
         */

        if (
                !raw.startsWith("/")
        ) {

            return;
        }


        /*
         * Получаем название команды.
         */

        String command =
                raw
                        .substring(1)
                        .split("\\s+")[0]
                        .toLowerCase();


        /*
         * Убираем namespace:
         *
         * minecraft:tp
         *
         * превращается в:
         *
         * tp
         */

        if (
                command.contains(":")
        ) {

            command =
                    command.substring(
                            command.indexOf(':') + 1
                    );
        }


        /*
         * Получаем список разрешённых команд.
         */

        List<String> allowed =
                plugin
                        .getJailManager()
                        .getAllowedCommands();


        /*
         * Команда разрешена.
         */

        if (
                allowed.contains(
                        command
                )
        ) {

            return;
        }


        /*
         * Всё остальное запрещаем.
         */

        event.setCancelled(
                true
        );


        player.sendActionBar(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "commands-blocked"
                                )
                )
        );
    }
}

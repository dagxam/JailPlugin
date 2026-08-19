package com.jail.listener;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDeathEvent;


/**
 * Обработчик убийств.
 *
 * Автоматически отправляет игрока в тюрьму,
 * если он убил:
 *
 * - другого игрока;
 * - жителя;
 * - железного голема;
 * - странствующего торговца.
 */
public final class DeathListener
        implements Listener {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    public DeathListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Обрабатывает смерть сущности.
     */
    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onDeath(
            EntityDeathEvent event
    ) {

        LivingEntity victim =
                event.getEntity();


        /*
         * Получаем игрока, который убил сущность.
         */

        Player killer =
                victim.getKiller();


        /*
         * Если убийцы нет,
         * наказание не применяется.
         */

        if (killer == null) {

            return;
        }


        /*
         * Игрок с правом prison.bypass
         * не получает автоматическое наказание.
         */

        if (
                killer.hasPermission(
                        "prison.bypass"
                )
        ) {

            return;
        }


        JailManager manager =
                plugin.getJailManager();


        String reason = null;

        String sentenceKey = null;


        /*
         * Убийство другого игрока.
         */

        if (
                victim instanceof Player
        ) {

            /*
             * Самоубийство не считается убийством
             * другого игрока.
             */

            if (
                    killer.getUniqueId()
                            .equals(
                                    victim.getUniqueId()
                            )
            ) {

                return;
            }


            reason =
                    manager.getMessage(
                            "reason-player-kill"
                    );


            sentenceKey =
                    "player-kill";
        }


        /*
         * Убийство жителя.
         */

        else if (
                victim.getType()
                        ==
                        EntityType.VILLAGER
        ) {

            reason =
                    manager.getMessage(
                            "reason-villager-kill"
                    );


            sentenceKey =
                    "villager-kill";
        }


        /*
         * Убийство железного голема.
         */

        else if (
                victim.getType()
                        ==
                        EntityType.IRON_GOLEM
        ) {

            reason =
                    manager.getMessage(
                            "reason-golem-kill"
                    );


            sentenceKey =
                    "golem-kill";
        }


        /*
         * Убийство странствующего торговца.
         */

        else if (
                victim.getType()
                        ==
                        EntityType.WANDERING_TRADER
        ) {

            reason =
                    manager.getMessage(
                            "reason-trader-kill"
                    );


            sentenceKey =
                    "trader-kill";
        }


        /*
         * Если сущность не входит
         * в список наказаний,
         * ничего не делаем.
         */

        if (
                reason == null
                        ||
                sentenceKey == null
        ) {

            return;
        }


        /*
         * Получаем срок из config.yml.
         */

        int seconds =
                manager.getSentenceTime(
                        sentenceKey
                );


        /*
         * Отправляем игрока в тюрьму.
         */

        manager.jailPlayer(

                killer,

                seconds,

                reason
        );
    }
}

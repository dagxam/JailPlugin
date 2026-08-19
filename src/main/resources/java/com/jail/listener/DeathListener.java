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

public final class DeathListener
        implements Listener {

    private final JailPlugin plugin;


    public DeathListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onDeath(
            EntityDeathEvent event
    ) {

        LivingEntity victim =
                event.getEntity();


        Player killer =
                victim.getKiller();


        if (killer == null) {

            return;
        }


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

        String key = null;


        if (victim instanceof Player) {

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

            key =
                    "player-kill";
        }


        else if (
                victim.getType()
                        == EntityType.VILLAGER
        ) {

            reason =
                    manager.getMessage(
                            "reason-villager-kill"
                    );

            key =
                    "villager-kill";
        }


        else if (
                victim.getType()
                        == EntityType.IRON_GOLEM
        ) {

            reason =
                    manager.getMessage(
                            "reason-golem-kill"
                    );

            key =
                    "golem-kill";
        }


        else if (
                victim.getType()
                        == EntityType.WANDERING_TRADER
        ) {

            reason =
                    manager.getMessage(
                            "reason-trader-kill"
                    );

            key =
                    "trader-kill";
        }


        if (reason == null) {

            return;
        }


        manager.jailPlayer(

                killer,

                manager.getSentenceTime(
                        key
                ),

                reason
        );
    }
}

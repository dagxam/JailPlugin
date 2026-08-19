package com.jail.listener;

import com.jail.JailPlugin;

import org.bukkit.Location;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerMoveEvent;

public final class MoveListener
        implements Listener {

    private final JailPlugin plugin;


    public MoveListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onMove(
            PlayerMoveEvent event
    ) {

        Player player =
                event.getPlayer();


        if (
                !plugin
                        .getJailManager()
                        .isJailed(
                                player.getUniqueId()
                        )
        ) {

            return;
        }


        Location cell =
                plugin
                        .getJailManager()
                        .getCellLocation(
                                player.getUniqueId()
                        );


        if (
                cell == null
                        || event.getTo() == null
        ) {

            return;
        }


        Location to =
                event.getTo();


        if (
                !to.getWorld()
                        .equals(
                                cell.getWorld()
                        )
        ) {

            player.teleport(
                    cell
            );

            return;
        }


        double radius =
                plugin
                        .getJailManager()
                        .getCellRadius();


        if (
                to.distanceSquared(
                        cell
                )
                        > radius * radius
        ) {

            player.teleport(
                    cell
            );
        }
    }
}

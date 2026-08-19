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

public final class ConnectionListener
        implements Listener {

    private final JailPlugin plugin;


    public ConnectionListener(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


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


        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    !player.isOnline()
                                            || !plugin
                                            .getJailManager()
                                            .isJailed(uuid)
                            ) {

                                return;
                            }


                            Location cell =
                                    plugin
                                            .getJailManager()
                                            .getCellLocation(
                                                    uuid
                                            );


                            if (cell != null) {

                                player.teleport(
                                        cell
                                );
                            }


                            int time =
                                    plugin
                                            .getJailManager()
                                            .getTimeRemaining(
                                                    uuid
                                            );


                            player.sendMessage(

                                    JailManager.colorize(

                                            plugin
                                                    .getJailManager()
                                                    .getMessage(
                                                            "join-still-jailed"
                                                    )

                                                    .replace(
                                                            "%mins%",
                                                            String.valueOf(
                                                                    time / 60
                                                            )
                                                    )

                                                    .replace(
                                                            "%secs%",
                                                            String.valueOf(
                                                                    time % 60
                                                            )
                                                    )
                                    )
                            );

                        },
                        2L
                );
    }


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


        if (
                !plugin
                        .getJailManager()
                        .isJailed(uuid)
        ) {

            return;
        }


        Location cell =
                plugin
                        .getJailManager()
                        .getCellLocation(
                                uuid
                        );


        if (cell == null) {

            return;
        }


        event.setRespawnLocation(
                cell
        );


        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (
                                    player.isOnline()
                                            && plugin
                                            .getJailManager()
                                            .isJailed(uuid)
                            ) {

                                player.teleport(
                                        cell
                                );
                            }

                        },
                        2L
                );
    }
}

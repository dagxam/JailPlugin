package com.jail.command;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

public final class JailTimeCommand
        implements CommandExecutor {

    private final JailPlugin plugin;


    public JailTimeCommand(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (
                !(sender instanceof Player player)
        ) {

            sender.sendMessage(
                    "Только для игроков."
            );

            return true;
        }


        JailManager manager =
                plugin.getJailManager();


        if (
                !manager.isJailed(
                        player.getUniqueId()
                )
        ) {

            player.sendMessage(
                    JailManager.colorize(
                            manager.getMessage(
                                    "jailtime-free"
                            )
                    )
            );

            return true;
        }


        int time =
                manager.getTimeRemaining(
                        player.getUniqueId()
                );


        player.sendMessage(
                JailManager.colorize(

                        manager
                                .getMessage(
                                        "jailtime-remaining"
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


        return true;
    }
}

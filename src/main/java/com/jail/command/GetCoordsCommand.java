package com.jail.command;

import com.jail.JailManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

public final class GetCoordsCommand
        implements CommandExecutor {

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


        player.sendMessage(
                JailManager.colorize(
                        "&6Мир: &f"
                                + player
                                .getWorld()
                                .getName()
                )
        );


        player.sendMessage(
                JailManager.colorize(
                        "&6X: &f"
                                + String.format(
                                "%.2f",
                                player.getX()
                        )
                )
        );


        player.sendMessage(
                JailManager.colorize(
                        "&6Y: &f"
                                + String.format(
                                "%.2f",
                                player.getY()
                        )
                )
        );


        player.sendMessage(
                JailManager.colorize(
                        "&6Z: &f"
                                + String.format(
                                "%.2f",
                                player.getZ()
                        )
                )
        );


        return true;
    }
}

package com.jail.command;

import com.jail.JailManager;
import com.jail.JailPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JailTimeCommand implements CommandExecutor {

    private final JailPlugin plugin;

    public JailTimeCommand(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;
        JailManager manager = plugin.getJailManager();

        if (manager.isJailed(player.getUniqueId())) {
            int time = manager.getTimeRemaining(player.getUniqueId());
            int mins = time / 60;
            int secs = time % 60;

            player.sendMessage(JailManager.colorize(
                    manager.getMessage("jailtime-remaining")
                            .replace("%mins%", String.valueOf(mins))
                            .replace("%secs%", String.valueOf(secs))));
        } else {
            player.sendMessage(JailManager.colorize(
                    manager.getMessage("jailtime-free")));
        }

        return true;
    }
}

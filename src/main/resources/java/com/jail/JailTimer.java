package com.jail;

import org.bukkit.scheduler.BukkitRunnable;

public class JailTimer extends BukkitRunnable {

    private final JailPlugin plugin;

    public JailTimer(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getJailManager().tickTimers();
    }
}

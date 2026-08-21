package com.jail.listener;

import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Контролирует перемещение заключённых.
 */
public final class MoveListener implements Listener {

    private final JailPlugin plugin;

    public MoveListener(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Поворот головы не считается перемещением.
        if (from.getX() == to.getX()
                && from.getY() == to.getY()
                && from.getZ() == to.getZ()) {
            return;
        }

        Player player = event.getPlayer();
        JailManager manager = plugin.getJailManager();

        if (!manager.isJailed(player.getUniqueId())) {
            return;
        }

        Location cell = manager.getCellLocation(player.getUniqueId());

        if (cell == null || cell.getWorld() == null || to.getWorld() == null) {
            event.setTo(cell);
            return;
        }

        if (!to.getWorld().equals(cell.getWorld())) {
            event.setTo(cell);
            player.sendActionBar(JailManager.component(
                    manager.getMessage("movement-blocked")
            ));
            return;
        }

        double radius = manager.getCellRadius();

        if (to.distanceSquared(cell) > radius * radius) {
            event.setTo(cell);
            player.sendActionBar(JailManager.component(
                    manager.getMessage("movement-blocked")
            ));
        }
    }
}

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

public class RestrictionListener implements Listener {

    private final JailPlugin plugin;

    public RestrictionListener(JailPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isJailed(Player player) {
        return plugin.getJailManager().isJailed(player.getUniqueId());
    }

    // Запрет ломать блоки
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (isJailed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Запрет ставить блоки
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (isJailed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Запрет наносить урон
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (isJailed(attacker)) {
                event.setCancelled(true);
            }
        }
    }

    // Запрет выбрасывать предметы
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isJailed(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Запрет подбирать предметы
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isJailed(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Запрет кликов в инвентаре
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isJailed(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Блокировка команд (кроме разрешённых)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isJailed(player)) return;

        // Администраторы могут использовать все команды
        if (player.hasPermission("prison.admin")) return;

        // Извлекаем команду (без слеша и аргументов)
        String message = event.getMessage().toLowerCase();
        String cmd = message.split(" ")[0].substring(1); // убираем /

        // Убираем префикс плагина (например minecraft:, essentials:)
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        List<String> allowed = plugin.getJailManager().getAllowedCommands();
        if (!allowed.contains(cmd)) {
            event.setCancelled(true);
            player.sendMessage(JailManager.colorize(
                    plugin.getJailManager().getMessage("commands-blocked")));
        }
    }
}

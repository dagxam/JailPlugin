package com.jail.listener;

import com.jail.JailManager;
import com.jail.JailPlugin;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DeathListener implements Listener {

    private final JailPlugin plugin;

    public DeathListener(JailPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        // Нет убийцы-игрока — игнорируем
        if (killer == null) return;

        // Проверка иммунитета
        if (killer.hasPermission("prison.bypass")) return;

        JailManager manager = plugin.getJailManager();
        String reason = null;
        String sentenceKey = null;

        // Определяем тип убийства
        if (victim instanceof Player) {
            // Самоубийство — не наказываем
            if (killer.getUniqueId().equals(victim.getUniqueId())) return;
            reason = manager.getMessage("reason-player-kill");
            sentenceKey = "player-kill";

        } else if (victim.getType() == EntityType.VILLAGER) {
            reason = manager.getMessage("reason-villager-kill");
            sentenceKey = "villager-kill";

        } else if (victim.getType() == EntityType.IRON_GOLEM) {
            reason = manager.getMessage("reason-golem-kill");
            sentenceKey = "golem-kill";

        } else if (victim.getType() == EntityType.WANDERING_TRADER) {
            reason = manager.getMessage("reason-trader-kill");
            sentenceKey = "trader-kill";
        }

        // Если тип не подходит — выходим
        if (reason == null) return;

        // Сажаем в тюрьму
        int seconds = manager.getSentenceTime(sentenceKey);
        manager.jailPlayer(killer, seconds, reason);
    }
}

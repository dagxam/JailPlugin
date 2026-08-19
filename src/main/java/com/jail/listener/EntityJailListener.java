package com.jail.listener;

import com.jail.EntityJailManager;
import com.jail.JailManager;
import com.jail.JailPlugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ручное заключение сущностей администратором.
 *
 * Игроки этой системой не заключаются.
 * Игроки продолжают использовать существующую систему JailManager.
 */
public final class EntityJailListener implements Listener {

    private final JailPlugin plugin;
    private final EntityJailManager entityJailManager;

    private final Map<UUID, Boolean> selecting = new HashMap<>();
    private final Map<UUID, UUID> menuTargets = new HashMap<>();
    private final Map<UUID, UUID> customTimeTargets = new HashMap<>();

    private static final Component MENU_TITLE =
            Component.text("⛓ Выбор срока тюрьмы");

    public EntityJailListener(
            JailPlugin plugin,
            EntityJailManager entityJailManager
    ) {
        this.plugin = plugin;
        this.entityJailManager = entityJailManager;
    }

    public void startSelection(Player player) {
        if (player == null) return;

        if (!player.hasPermission("prison.entityjail")) {
            player.sendMessage(JailManager.component(
                    "&cУ вас нет права prison.entityjail."
            ));
            return;
        }

        selecting.put(player.getUniqueId(), true);
        menuTargets.remove(player.getUniqueId());
        customTimeTargets.remove(player.getUniqueId());

        player.sendMessage(JailManager.component(
                "&a✓ Режим выбора сущности включён."
        ));
        player.sendMessage(JailManager.component(
                "&7Нажмите &fПКМ &7по живой сущности."
        ));
        player.sendMessage(JailManager.component(
                "&7Игроки через эту систему не заключаются."
        ));
    }

    public void stopSelection(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        selecting.remove(uuid);
        menuTargets.remove(uuid);
        customTimeTargets.remove(uuid);

        player.sendMessage(JailManager.component(
                "&eРежим выбора сущности выключен."
        ));
    }

    public boolean isSelecting(Player player) {
        return player != null && selecting.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!isSelecting(player)) return;

        if (!player.hasPermission("prison.entityjail")) {
            stopSelection(player);
            return;
        }

        Entity entity = event.getRightClicked();
        event.setCancelled(true);

        if (entity instanceof Player) {
            player.sendMessage(JailManager.component(
                    "&cИгроков через ручное заключение сущностей сажать нельзя."
            ));
            return;
        }

        if (!(entity instanceof LivingEntity)) {
            player.sendMessage(JailManager.component(
                    "&cЭта сущность не является живой."
            ));
            return;
        }

        /*
         * Любая LivingEntity разрешена.
         * В том числе водные существа.
         */
        openTimeMenu(player, entity);
    }

    /**
     * ЛКМ по живой сущности.
     *
     * Используется дополнительно к ПКМ, чтобы администратор
     * мог выбрать любую живую сущность, включая водных мобов.
     *
     * Урон по сущности отменяется.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityLeftClick(
            EntityDamageByEntityEvent event
    ) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!isSelecting(player)) {
            return;
        }

        if (!player.hasPermission("prison.entityjail")) {
            event.setCancelled(true);
            stopSelection(player);
            return;
        }

        Entity entity = event.getEntity();

        /*
         * В режиме выбора сущности удар никогда
         * не должен наносить реальный урон.
         */
        event.setCancelled(true);

        /*
         * Игроков ручной системой не заключаем.
         */
        if (entity instanceof Player) {
            player.sendMessage(JailManager.component(
                    "&cИгроков через ручное заключение сущностей сажать нельзя."
            ));
            return;
        }

        /*
         * Любая живая сущность разрешена.
         *
         * Это включает:
         * - наземных мобов;
         * - водных мобов;
         * - рыб;
         * - аксолотлей;
         * - дельфинов;
         * - черепах;
         * - лягушек;
         * - лошадей;
         * - жителей;
         * - големов;
         * - и другие LivingEntity.
         */
        if (!(entity instanceof LivingEntity)) {
            player.sendMessage(JailManager.component(
                    "&cЭта сущность не является живой."
            ));
            return;
        }

        openTimeMenu(player, entity);
    }


    private void openTimeMenu(Player player, Entity entity) {
        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                MENU_TITLE
        );

        inventory.setItem(10, createItem(
                Material.CLOCK,
                "§e1 минута",
                "§7Заключить на 1 минуту."
        ));
        inventory.setItem(11, createItem(
                Material.CLOCK,
                "§e5 минут",
                "§7Заключить на 5 минут."
        ));
        inventory.setItem(12, createItem(
                Material.CLOCK,
                "§e10 минут",
                "§7Заключить на 10 минут."
        ));
        inventory.setItem(14, createItem(
                Material.CLOCK,
                "§e30 минут",
                "§7Заключить на 30 минут."
        ));
        inventory.setItem(15, createItem(
                Material.CLOCK,
                "§e1 час",
                "§7Заключить на 1 час."
        ));
        inventory.setItem(13, createItem(
                Material.WRITABLE_BOOK,
                "§bСвой срок",
                "§7Введите количество минут в чат."
        ));
        inventory.setItem(22, createItem(
                Material.BARRIER,
                "§cОтмена",
                "§7Отменить заключение."
        ));

        menuTargets.put(player.getUniqueId(), entity.getUniqueId());
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(MENU_TITLE)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        UUID entityUuid = menuTargets.get(player.getUniqueId());
        if (entityUuid == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 22) {
            cancelMenu(player);
            return;
        }

        if (slot == 13) {
            UUID uuid = player.getUniqueId();
            customTimeTargets.put(uuid, entityUuid);
            menuTargets.remove(uuid);
            player.closeInventory();

            player.sendMessage(JailManager.component(
                    "&bВведите срок в минутах в чат. &7Например: &f15"
            ));
            player.sendMessage(JailManager.component(
                    "&7Для отмены напишите: &fcancel"
            ));
            return;
        }

        int seconds = switch (slot) {
            case 10 -> 60;
            case 11 -> 300;
            case 12 -> 600;
            case 14 -> 1800;
            case 15 -> 3600;
            default -> 0;
        };

        if (seconds > 0) {
            jailSelectedEntity(player, entityUuid, seconds);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomTimeChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        UUID entityUuid = customTimeTargets.get(playerUuid);

        if (entityUuid == null) return;

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();

        if (input.equalsIgnoreCase("cancel")) {
            customTimeTargets.remove(playerUuid);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(JailManager.component(
                            "&eВвод срока отменён."
                    ))
            );
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(JailManager.component(
                            "&cВведите целое положительное число минут или &fcancel&c."
                    ))
            );
            return;
        }

        if (minutes < 1 || minutes > 10080) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(JailManager.component(
                            "&cСрок должен быть от 1 до 10080 минут."
                    ))
            );
            return;
        }

        customTimeTargets.remove(playerUuid);
        int seconds = minutes * 60;

        Bukkit.getScheduler().runTask(plugin, () ->
                jailSelectedEntity(player, entityUuid, seconds)
        );
    }

    private void jailSelectedEntity(
            Player player,
            UUID entityUuid,
            int seconds
    ) {
        Entity entity = findEntity(entityUuid);

        if (entity == null || entity.isDead()) {
            player.sendMessage(JailManager.component(
                    "&cСущность больше не найдена."
            ));
            return;
        }

        if (entity instanceof Player || !(entity instanceof LivingEntity)) {
            player.sendMessage(JailManager.component(
                    "&cЭту сущность нельзя заключить в тюрьму."
            ));
            return;
        }

        Location cell = plugin.getJailManager().getRandomCell();

        if (cell == null) {
            player.sendMessage(JailManager.component(
                    plugin.getJailManager().getMessage("no-cells")
            ));
            return;
        }

        if (!entityJailManager.jailEntity(entity, seconds, cell)) {
            player.sendMessage(JailManager.component(
                    "&cНе удалось заключить сущность в тюрьму."
            ));
            return;
        }

        selecting.remove(player.getUniqueId());
        menuTargets.remove(player.getUniqueId());

        player.sendMessage(JailManager.component(
                "&a✓ Сущность &f" + entity.getType().name()
                        + " &aзаключена на &f"
                        + plugin.getJailManager().formatTimeWords(seconds)
                        + "&a."
        ));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().title().equals(MENU_TITLE)) return;

        if (event.getPlayer() instanceof Player player) {
            menuTargets.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        entityJailManager.releaseEntity(event.getEntity().getUniqueId());
    }

    private void cancelMenu(Player player) {
        menuTargets.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(JailManager.component(
                "&eЗаключение отменено."
        ));
    }

    private ItemStack createItem(
            Material material,
            String name,
            String lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(List.of(Component.text(lore)));
            item.setItemMeta(meta);
        }

        return item;
    }

    private Entity findEntity(UUID uuid) {
        if (uuid == null) return null;

        for (var world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }

        return Bukkit.getEntity(uuid);
    }
}

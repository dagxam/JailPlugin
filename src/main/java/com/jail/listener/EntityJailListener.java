package com.jail.listener;

import com.jail.EntityJailManager;
import com.jail.JailManager;
import com.jail.JailPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Обработчик ручного заключения сущностей.
 *
 * Игроки этой системой НЕ заключаются.
 *
 * Администратор:
 *
 * /prison entityjail
 *
 * затем нажимает ЛКМ по сущности.
 *
 * После выбора сущности открывается меню
 * выбора срока заключения.
 */
public final class EntityJailListener
        implements Listener {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    /**
     * Менеджер заключённых сущностей.
     */
    private final EntityJailManager entityJailManager;


    /**
     * Игроки, находящиеся в режиме
     * выбора сущности.
     *
     * UUID администратора -> true
     */
    private final Map<UUID, Boolean> selecting =
            new HashMap<>();


    /**
     * Сущность, выбранная администратором.
     *
     * UUID администратора -> UUID сущности
     */
    private final Map<UUID, UUID> selectedEntities =
            new HashMap<>();


    /**
     * Меню выбора срока.
     *
     * UUID администратора -> UUID сущности
     */
    private final Map<UUID, UUID> menuTargets =
            new HashMap<>();


    /**
     * Название меню.
     */
    private static final String MENU_TITLE =
            "§8⛓ Выбор срока тюрьмы";


    /**
     * Создаёт listener.
     *
     * @param plugin главный класс плагина
     * @param entityJailManager менеджер сущностей
     */
    public EntityJailListener(
            JailPlugin plugin,
            EntityJailManager entityJailManager
    ) {

        this.plugin =
                plugin;

        this.entityJailManager =
                entityJailManager;
    }


    /**
     * Включает режим выбора сущности
     * для администратора.
     *
     * @param player администратор
     */
    public void startSelection(
            Player player
    ) {

        if (
                player == null
        ) {

            return;
        }


        if (
                !player.hasPermission(
                        "prison.entityjail"
                )
        ) {

            player.sendMessage(

                    JailManager.component(

                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "entity-no-permission"
                                    )
                    )
            );

            return;
        }


        selecting.put(
                player.getUniqueId(),
                true
        );


        selectedEntities.remove(
                player.getUniqueId()
        );


        player.sendMessage("");

        player.sendMessage(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "entity-select-start"
                                )
                )
        );

        player.sendMessage("");
    }


    /**
     * Выключает режим выбора сущности.
     *
     * @param player администратор
     */
    public void stopSelection(
            Player player
    ) {

        if (
                player == null
        ) {

            return;
        }


        UUID uuid =
                player.getUniqueId();


        selecting.remove(
                uuid
        );


        selectedEntities.remove(
                uuid
        );


        menuTargets.remove(
                uuid
        );


        player.sendMessage(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "entity-select-stop"
                                )
                )
        );
    }


    /**
     * Проверяет, находится ли игрок
     * в режиме выбора сущности.
     *
     * @param player игрок
     * @return true, если режим активен
     */
    public boolean isSelecting(
            Player player
    ) {

        return player != null
                &&
                selecting.containsKey(
                        player.getUniqueId()
                );
    }


    /**
     * ЛКМ по сущности.
     *
     * После нажатия открываем меню
     * выбора срока.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityInteract(
            PlayerInteractEntityEvent event
    ) {

        Player player =
                event.getPlayer();


        /*
         * Нас интересуют только игроки,
         * которые включили режим выбора.
         */

        if (
                !isSelecting(
                        player
                )
        ) {

            return;
        }


        /*
         * Проверяем разрешение ещё раз.
         */

        if (
                !player.hasPermission(
                        "prison.entityjail"
                )
        ) {

            stopSelection(
                    player
            );

            return;
        }


        /*
         * Игроков через эту систему
         * не заключаем.
         */

        Entity entity =
                event.getRightClicked();


        if (
                entity instanceof Player
        ) {

            event.setCancelled(
                    true
            );


            player.sendMessage(

                    JailManager.component(

                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "entity-player-not-allowed"
                                    )
                    )
            );


            return;
        }


        /*
         * Разрешаем только живые сущности.
         */

        if (
                !(entity instanceof org.bukkit.entity.LivingEntity)
        ) {

            event.setCancelled(
                    true
            );


            player.sendMessage(

                    JailManager.component(

                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "entity-not-living"
                                    )
                    )
            );


            return;
        }


        /*
         * Сохраняем выбранную сущность.
         */

        selectedEntities.put(
                player.getUniqueId(),
                entity.getUniqueId()
        );


        /*
         * Открываем меню.
         */

        event.setCancelled(
                true
        );


        openTimeMenu(
                player,
                entity
        );
    }


    /**
     * Открывает меню выбора срока.
     *
     * @param player администратор
     * @param entity выбранная сущность
     */
    private void openTimeMenu(
            Player player,
            Entity entity
    ) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        MENU_TITLE
                );


        /*
         * 1 минута.
         */

        inventory.setItem(
                10,
                createItem(
                        Material.CLOCK,
                        "§e1 минута",
                        "§7Заключить на 1 минуту."
                )
        );


        /*
         * 5 минут.
         */

        inventory.setItem(
                11,
                createItem(
                        Material.CLOCK,
                        "§e5 минут",
                        "§7Заключить на 5 минут."
                )
        );


        /*
         * 10 минут.
         */

        inventory.setItem(
                12,
                createItem(
                        Material.CLOCK,
                        "§e10 минут",
                        "§7Заключить на 10 минут."
                )
        );


        /*
         * 30 минут.
         */

        inventory.setItem(
                14,
                createItem(
                        Material.CLOCK,
                        "§e30 минут",
                        "§7Заключить на 30 минут."
                )
        );


        /*
         * 1 час.
         */

        inventory.setItem(
                15,
                createItem(
                        Material.CLOCK,
                        "§e1 час",
                        "§7Заключить на 1 час."
                )
        );


        /*
         * Свой срок.
         *
         * Пока кнопка информирует игрока,
         * что ввод своего срока будет добавлен
         * следующим этапом.
         */

        inventory.setItem(
                13,
                createItem(
                        Material.WRITABLE_BOOK,
                        "§bСвой срок",
                        "§7Ввести собственный срок."
                )
        );


        /*
         * Отмена.
         */

        inventory.setItem(
                22,
                createItem(
                        Material.BARRIER,
                        "§cОтмена",
                        "§7Отменить заключение."
                )
        );


        menuTargets.put(
                player.getUniqueId(),
                entity.getUniqueId()
        );


        player.openInventory(
                inventory
        );
    }


    /**
     * Обрабатывает нажатия в меню.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (
                !MENU_TITLE.equals(
                        event.getView().getTitle()
                )
        ) {

            return;
        }


        if (
                !(event.getWhoClicked()
                        instanceof Player player)
        ) {

            return;
        }


        event.setCancelled(
                true
        );


        UUID entityUuid =
                menuTargets.get(
                        player.getUniqueId()
                );


        if (
                entityUuid == null
        ) {

            player.closeInventory();

            return;
        }


        int slot =
                event.getRawSlot();


        /*
         * Нажата кнопка отмены.
         */

        if (
                slot == 22
        ) {

            cancelMenu(
                    player
            );

            return;
        }


        /*
         * Получаем срок
         * в зависимости от слота.
         */

        int seconds;


        switch (
                slot
        ) {

            case 10 -> seconds = 60;

            case 11 -> seconds = 300;

            case 12 -> seconds = 600;

            case 14 -> seconds = 1800;

            case 15 -> seconds = 3600;


            /*
             * Свой срок пока оставляем
             * отдельным следующим этапом.
             */

            case 13 -> {

                player.sendMessage(

                        JailManager.component(

                                plugin
                                        .getJailManager()
                                        .getMessage(
                                                "entity-custom-time-not-ready"
                                        )
                        )
                );

                return;
            }


            default -> {
                return;
            }
        }


        /*
         * Находим сущность.
         */

        Entity entity =
                findEntity(
                        entityUuid
                );


        if (
                entity == null
                        ||
                entity.isDead()
        ) {

            player.closeInventory();


            menuTargets.remove(
                    player.getUniqueId()
            );


            selectedEntities.remove(
                    player.getUniqueId()
            );


            player.sendMessage(

                    JailManager.component(

                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "entity-not-found"
                                    )
                    )
            );


            return;
        }


        /*
         * Игроки всё ещё запрещены.
         */

        if (
                entity instanceof Player
        ) {

            player.closeInventory();

            return;
        }


        /*
         * Получаем случайную камеру
         * через существующий JailManager.
         */

        Location cell =
                plugin
                        .getJailManager()
                        .getRandomCell();


        if (
                cell == null
        ) {

            player.closeInventory();


            player.sendMessage(

                    JailManager.component(

                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "no-cells"
                                    )
                    )
            );


            return;
        }


        /*
         * Заключаем сущность.
         */

        boolean success =
                entityJailManager.jailEntity(
                        entity,
                        seconds,
                        cell
                );


        player.closeInventory();


        menuTargets.remove(
                player.getUniqueId()
        );


        selectedEntities.remove(
                player.getUniqueId()
        );


        /*
         * Если не получилось.
         */

        if (
                !success
        ) {

            player.sendMessage(

                    JailManager.component(

                            plugin
                                    .getJailManager()
                                    .getMessage(
                                            "entity-jail-failed"
                                    )
                    )
            );


            return;
        }


        /*
         * Сообщаем администратору.
         */

        player.sendMessage("");

        player.sendMessage(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "entity-jailed"
                                )

                                .replace(
                                        "%entity%",
                                        entity
                                                .getType()
                                                .name()
                                )

                                .replace(
                                        "%time%",
                                        plugin
                                                .getJailManager()
                                                .formatTimeWords(
                                                        seconds
                                                )
                                )
                )
        );

        player.sendMessage("");
    }


    /**
     * Обработка закрытия меню.
     *
     * Если игрок просто закрыл GUI,
     * выбор отменяется.
     */
    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {

        if (
                !MENU_TITLE.equals(
                        event.getView().getTitle()
                )
        ) {

            return;
        }


        if (
                !(event.getPlayer()
                        instanceof Player player)
        ) {

            return;
        }


        UUID uuid =
                player.getUniqueId();


        menuTargets.remove(
                uuid
        );


        selectedEntities.remove(
                uuid
        );
    }


    /**
     * Если заключённая сущность погибла,
     * её запись удаляем.
     */
    @EventHandler
    public void onEntityDeath(
            EntityDeathEvent event
    ) {

        Entity entity =
                event.getEntity();


        UUID uuid =
                entity.getUniqueId();


        if (
                entityJailManager.isJailed(
                        uuid
                )
        ) {

            /*
             * Срок больше не имеет смысла,
             * потому что сущность погибла.
             */

            entityJailManager.releaseEntity(
                    uuid
            );
        }
    }


    /**
     * Отмена текущего выбора.
     */
    private void cancelMenu(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();


        menuTargets.remove(
                uuid
        );


        selectedEntities.remove(
                uuid
        );


        player.closeInventory();


        player.sendMessage(

                JailManager.component(

                        plugin
                                .getJailManager()
                                .getMessage(
                                        "entity-jail-cancelled"
                                )
                )
        );
    }


    /**
     * Создаёт предмет меню.
     */
    private ItemStack createItem(
            Material material,
            String name,
            String lore
    ) {

        ItemStack item =
                new ItemStack(
                        material
                );


        ItemMeta meta =
                item.getItemMeta();


        if (
                meta != null
        ) {

            meta.setDisplayName(
                    name
            );


            meta.setLore(
                    java.util.List.of(
                            lore
                    )
            );


            item.setItemMeta(
                    meta
            );
        }


        return item;
    }


    /**
     * Ищет сущность во всех загруженных мирах.
     */
    private Entity findEntity(
            UUID uuid
    ) {

        for (
                org.bukkit.World world :
                Bukkit.getWorlds()
        ) {

            Entity entity =
                    world.getEntity(
                            uuid
                    );


            if (
                    entity != null
            ) {

                return entity;
            }
        }


        return null;
    }
}

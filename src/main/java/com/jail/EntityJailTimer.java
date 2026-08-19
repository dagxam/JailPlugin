package com.jail;

import org.bukkit.scheduler.BukkitRunnable;


/**
 * Таймер заключённых сущностей.
 *
 * Работает отдельно от таймера игроков.
 *
 * 20 тиков = 1 секунда.
 */
public final class EntityJailTimer
        extends BukkitRunnable {


    /**
     * Менеджер заключённых сущностей.
     */
    private final EntityJailManager entityJailManager;


    /**
     * Создаёт таймер.
     *
     * @param entityJailManager менеджер сущностей
     */
    public EntityJailTimer(
            EntityJailManager entityJailManager
    ) {

        this.entityJailManager =
                entityJailManager;
    }


    /**
     * Выполняется каждую секунду.
     */
    @Override
    public void run() {

        entityJailManager.tickTimers();
    }
}

package com.jail;

import org.bukkit.scheduler.BukkitRunnable;


/**
 * Таймер системы тюрьмы.
 *
 * Запускается главным классом JailPlugin
 * один раз в секунду.
 *
 * Его задача — передавать управление
 * менеджеру тюрьмы для уменьшения
 * оставшихся сроков заключённых.
 */
public final class JailTimer
        extends BukkitRunnable {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    /**
     * Создаёт таймер тюрьмы.
     *
     * @param plugin главный класс плагина
     */
    public JailTimer(
            JailPlugin plugin
    ) {

        this.plugin = plugin;
    }


    /**
     * Выполняется один раз в секунду.
     */
    @Override
    public void run() {

        /*
         * Передаём один тик менеджеру.
         *
         * JailManager сам:
         *
         * - уменьшает срок;
         * - обновляет ActionBar;
         * - освобождает игроков;
         * - сохраняет необходимые данные.
         */

        plugin
                .getJailManager()
                .tickTimers();
    }
}

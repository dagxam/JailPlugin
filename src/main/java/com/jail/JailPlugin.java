package com.jail;

import com.jail.command.GetCoordsCommand;
import com.jail.command.JailTimeCommand;
import com.jail.command.PrisonCommand;

import com.jail.listener.ConnectionListener;
import com.jail.listener.DeathListener;
import com.jail.listener.MoveListener;
import com.jail.listener.RestrictionListener;

import org.bukkit.command.PluginCommand;

import org.bukkit.plugin.java.JavaPlugin;


/**
 * Главный класс JailPlugin.
 *
 * Отвечает за:
 *
 * - запуск плагина;
 * - остановку плагина;
 * - регистрацию команд;
 * - регистрацию событий;
 * - запуск таймера тюрьмы;
 * - загрузку и сохранение заключённых.
 */
public final class JailPlugin
        extends JavaPlugin {


    /**
     * Основной менеджер тюрьмы.
     */
    private JailManager jailManager;


    /**
     * Таймер заключения.
     */
    private JailTimer jailTimer;


    /**
     * Вызывается при запуске плагина.
     */
    @Override
    public void onEnable() {

        /*
         * Создаём папку плагина,
         * если её ещё нет.
         */

        if (!getDataFolder().exists()) {

            if (!getDataFolder().mkdirs()) {

                getLogger().warning(
                        "Не удалось создать папку плагина."
                );
            }
        }


        /*
         * Создаём config.yml,
         * если его ещё нет.
         */

        saveDefaultConfig();


        /*
         * Создаём менеджер тюрьмы.
         */

        jailManager =
                new JailManager(
                        this
                );


        /*
         * Загружаем заключённых
         * из prisoners.yml.
         */

        jailManager.loadPrisoners();


        /*
         * Регистрируем команды.
         */

        registerCommands();


        /*
         * Регистрируем обработчики событий.
         */

        registerListeners();


        /*
         * Запускаем таймер.
         *
         * 20 тиков = 1 секунда.
         */

        jailTimer =
                new JailTimer(
                        this
                );


        jailTimer.runTaskTimer(
                this,
                20L,
                20L
        );


        /*
         * Информация в консоли.
         */

        getLogger().info(
                "========================================"
        );

        getLogger().info(
                "JailPlugin успешно запущен."
        );

        getLogger().info(
                "Версия плагина: 2.0.0"
        );

        getLogger().info(
                "Система тюрьмы активна."
        );

        getLogger().info(
                "Загружено камер: " +
                        jailManager.getCellCount()
        );

        getLogger().info(
                "Загружено заключённых: " +
                        jailManager
                                .getAllPrisoners()
                                .size()
        );

        getLogger().info(
                "========================================"
        );
    }


    /**
     * Вызывается при остановке плагина.
     */
    @Override
    public void onDisable() {

        /*
         * Останавливаем таймер.
         */

        if (jailTimer != null) {

            jailTimer.cancel();

            jailTimer = null;
        }


        /*
         * Сохраняем заключённых.
         */

        if (jailManager != null) {

            jailManager.savePrisoners();
        }


        getLogger().info(
                "JailPlugin остановлен."
        );
    }


    /**
     * Регистрирует все команды плагина.
     */
    private void registerCommands() {

        /*
         * /prison
         */

        registerCommand(
                "prison",
                new PrisonCommand(this),
                true
        );


        /*
         * /jailtime
         */

        registerCommand(
                "jailtime",
                new JailTimeCommand(this),
                false
        );


        /*
         * /getcoords
         */

        registerCommand(
                "getcoords",
                new GetCoordsCommand(this),
                false
        );
    }


    /**
     * Универсальная регистрация команды.
     *
     * @param name название команды из plugin.yml
     * @param executor обработчик команды
     * @param tabComplete нужен ли автодополнитель команд
     */
    private void registerCommand(
            String name,
            Object executor,
            boolean tabComplete
    ) {

        PluginCommand command =
                getCommand(
                        name
                );


        /*
         * Если команда не указана
         * в plugin.yml — это ошибка настройки.
         */

        if (command == null) {

            throw new IllegalStateException(
                    "Команда '" +
                            name +
                            "' не найдена в plugin.yml."
            );
        }


        /*
         * Устанавливаем обработчик команды.
         */

        if (
                executor
                        instanceof org.bukkit.command.CommandExecutor commandExecutor
        ) {

            command.setExecutor(
                    commandExecutor
            );
        }


        /*
         * Устанавливаем автодополнение.
         */

        if (
                tabComplete
                        && executor
                        instanceof org.bukkit.command.TabCompleter tabCompleter
        ) {

            command.setTabCompleter(
                    tabCompleter
            );
        }
    }


    /**
     * Регистрирует все слушатели событий.
     */
    private void registerListeners() {

        /*
         * Вход игрока и возрождение.
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new ConnectionListener(this),
                        this
                );


        /*
         * Убийства и автоматические наказания.
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new DeathListener(this),
                        this
                );


        /*
         * Ограничение выхода из камеры.
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new MoveListener(this),
                        this
                );


        /*
         * Запрет действий заключённого.
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new RestrictionListener(this),
                        this
                );
    }


    /**
     * Возвращает менеджер тюрьмы.
     */
    public JailManager getJailManager() {

        return jailManager;
    }
}

package com.jail;

import com.jail.command.GetCoordsCommand;
import com.jail.command.JailTimeCommand;
import com.jail.command.PrisonCommand;

import com.jail.listener.ConnectionListener;
import com.jail.listener.DeathListener;
import com.jail.listener.EntityJailListener;
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
 * - запуск таймеров;
 * - загрузку и сохранение заключённых;
 * - управление заключёнными сущностями.
 */
public final class JailPlugin
        extends JavaPlugin {


    /**
     * Основной менеджер тюрьмы игроков.
     */
    private JailManager jailManager;


    /**
     * Менеджер заключённых сущностей.
     */
    private EntityJailManager entityJailManager;


    /**
     * Таймер заключения игроков.
     */
    private JailTimer jailTimer;


    /**
     * Таймер заключения сущностей.
     */
    private EntityJailTimer entityJailTimer;


    /**
     * Listener ручного заключения сущностей.
     */
    private EntityJailListener entityJailListener;


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
         * Создаём основной менеджер тюрьмы.
         */

        jailManager =
                new JailManager(
                        this
                );


        /*
         * Загружаем заключённых игроков.
         */

        jailManager.loadPrisoners();


        /*
         * Создаём менеджер сущностей.
         */

        entityJailManager =
                new EntityJailManager(
                        this
                );


        /*
         * Загружаем заключённых сущностей.
         */

        entityJailManager.loadEntities();


        /*
         * Создаём listener сущностей.
         */

        entityJailListener =
                new EntityJailListener(
                        this,
                        entityJailManager
                );


        /*
         * Регистрируем команды.
         */

        registerCommands();


        /*
         * Регистрируем события.
         */

        registerListeners();


        /*
         * Запускаем таймер игроков.
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
         * Запускаем отдельный таймер сущностей.
         */

        entityJailTimer =
                new EntityJailTimer(
                        entityJailManager
                );


        entityJailTimer.runTaskTimer(
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
                "Загружено заключённых игроков: " +
                        jailManager
                                .getAllPrisoners()
                                .size()
        );

        getLogger().info(
                "Загружено заключённых сущностей: " +
                        entityJailManager
                                .getAllEntities()
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
         * Останавливаем таймер игроков.
         */

        if (jailTimer != null) {

            jailTimer.cancel();

            jailTimer = null;
        }


        /*
         * Останавливаем таймер сущностей.
         */

        if (entityJailTimer != null) {

            entityJailTimer.cancel();

            entityJailTimer = null;
        }


        /*
         * Сохраняем игроков.
         */

        if (jailManager != null) {

            jailManager.savePrisoners();
        }


        /*
         * Сохраняем сущности.
         */

        if (entityJailManager != null) {

            entityJailManager.saveEntities();
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
     * @param name название команды
     * @param executor обработчик команды
     * @param tabComplete нужен ли TabCompleter
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
         * Если команда отсутствует
         * в plugin.yml — останавливаем запуск.
         */

        if (command == null) {

            throw new IllegalStateException(
                    "Команда '" +
                            name +
                            "' не найдена в plugin.yml."
            );
        }


        /*
         * Устанавливаем обработчик.
         */

        if (
                executor
                        instanceof
                        org.bukkit.command.CommandExecutor
                        commandExecutor
        ) {

            command.setExecutor(
                    commandExecutor
            );
        }


        /*
         * Устанавливаем TabCompleter.
         */

        if (
                tabComplete
                        &&
                executor
                                instanceof
                                org.bukkit.command.TabCompleter
                                tabCompleter
        ) {

            command.setTabCompleter(
                    tabCompleter
            );
        }
    }


    /**
     * Регистрирует обработчики событий.
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
         * Ограничения для заключённых игроков.
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new RestrictionListener(this),
                        this
                );


        /*
         * Ручное заключение сущностей.
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        entityJailListener,
                        this
                );
    }


    /**
     * Возвращает менеджер игроков.
     */
    public JailManager getJailManager() {

        return jailManager;
    }


    /**
     * Возвращает менеджер сущностей.
     */
    public EntityJailManager getEntityJailManager() {

        return entityJailManager;
    }


    /**
     * Возвращает listener сущностей.
     */
    public EntityJailListener getEntityJailListener() {

        return entityJailListener;
    }
}

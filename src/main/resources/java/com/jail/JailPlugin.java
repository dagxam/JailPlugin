package com.jail;

import com.jail.command.GetCoordsCommand;
import com.jail.command.JailTimeCommand;
import com.jail.command.PrisonCommand;
import com.jail.listener.ConnectionListener;
import com.jail.listener.DeathListener;
import com.jail.listener.MoveListener;
import com.jail.listener.RestrictionListener;
import org.bukkit.plugin.java.JavaPlugin;

public class JailPlugin extends JavaPlugin {

    private JailManager jailManager;

    @Override
    public void onEnable() {
        // Сохраняем конфиг по умолчанию (если не существует)
        saveDefaultConfig();

        // Инициализация менеджера тюрьмы
        jailManager = new JailManager(this);
        jailManager.loadPrisoners();

        // Регистрация слушателей событий
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);

        // Регистрация команд
        PrisonCommand prisonCmd = new PrisonCommand(this);
        getCommand("prison").setExecutor(prisonCmd);
        getCommand("prison").setTabCompleter(prisonCmd);
        getCommand("jailtime").setExecutor(new JailTimeCommand(this));
        getCommand("getcoords").setExecutor(new GetCoordsCommand());

        // Запуск таймера (каждую секунду = 20 тиков)
        new JailTimer(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("Плагин тюрьмы успешно загружен!");
    }

    @Override
    public void onDisable() {
        if (jailManager != null) {
            jailManager.savePrisoners();
        }
        getLogger().info("Плагин тюрьмы выключен.");
    }

    public JailManager getJailManager() {
        return jailManager;
    }
}

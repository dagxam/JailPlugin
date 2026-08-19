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

public final class JailPlugin extends JavaPlugin {

    private JailManager jailManager;
    private JailTimer jailTimer;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        jailManager = new JailManager(this);

        jailManager.loadPrisoners();

        registerCommands();

        registerListeners();

        jailTimer = new JailTimer(this);

        jailTimer.runTaskTimer(
                this,
                20L,
                20L
        );

        getLogger().info(
                "JailPlugin 2.0.0 запущен."
        );

        getLogger().info(
                "Paper / Minecraft 26.2"
        );

        getLogger().info(
                "Камер загружено: "
                        + jailManager.getCellCount()
        );
    }

    @Override
    public void onDisable() {

        if (jailManager != null) {
            jailManager.savePrisoners();
        }

        if (jailTimer != null) {
            jailTimer.cancel();
        }
    }

    private void registerCommands() {

        register(
                "prison",
                new PrisonCommand(this),
                true
        );

        register(
                "jailtime",
                new JailTimeCommand(this),
                false
        );

        register(
                "getcoords",
                new GetCoordsCommand(),
                false
        );
    }

    private void register(
            String name,
            Object executor,
            boolean tabComplete
    ) {

        PluginCommand command = getCommand(name);

        if (command == null) {

            throw new IllegalStateException(
                    "Команда не объявлена в plugin.yml: "
                            + name
            );
        }

        if (executor instanceof org.bukkit.command.CommandExecutor ce) {

            command.setExecutor(ce);
        }

        if (
                tabComplete
                        && executor instanceof org.bukkit.command.TabCompleter tc
        ) {

            command.setTabCompleter(tc);
        }
    }

    private void registerListeners() {

        getServer()
                .getPluginManager()
                .registerEvents(
                        new ConnectionListener(this),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new DeathListener(this),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new MoveListener(this),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new RestrictionListener(this),
                        this
                );
    }

    public JailManager getJailManager() {

        return jailManager;
    }
}

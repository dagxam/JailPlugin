package com.jail;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JailManager {

    private final JailPlugin plugin;
    private final File dataFile;

    // Данные заключённых: UUID -> оставшееся время (секунды)
    private final Map<UUID, Integer> prisonerTimes = new HashMap<>();
    // Данные заключённых: UUID -> локация камеры
    private final Map<UUID, Location> prisonerCells = new HashMap<>();

    // Кэш из конфига
    private final List<Location> cellLocations = new ArrayList<>();
    private Location releaseLocation;
    private double cellRadius;
    private List<String> allowedCommands = new ArrayList<>();
    private boolean broadcastArrests;

    public JailManager(JailPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "prisoners.yml");
        reloadSettings();
    }

    // ─── Загрузка настроек из config.yml ───

    public void reloadSettings() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Загрузка камер
        cellLocations.clear();
        if (config.isConfigurationSection("cells")) {
            for (String key : config.getConfigurationSection("cells").getKeys(false)) {
                String path = "cells." + key;
                String worldName = config.getString(path + ".world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Мир '" + worldName + "' не найден для камеры " + key);
                    continue;
                }
                cellLocations.add(new Location(world,
                        config.getDouble(path + ".x"),
                        config.getDouble(path + ".y"),
                        config.getDouble(path + ".z")));
            }
        }

        if (cellLocations.isEmpty()) {
            plugin.getLogger().warning("Не настроено ни одной камеры! Проверьте config.yml");
        }

        // Точка освобождения
        String relWorldName = config.getString("release.world", "world");
        World relWorld = Bukkit.getWorld(relWorldName);
        if (relWorld != null) {
            releaseLocation = new Location(relWorld,
                    config.getDouble("release.x"),
                    config.getDouble("release.y"),
                    config.getDouble("release.z"));
        } else {
            plugin.getLogger().warning("Мир '" + relWorldName + "' не найден для точки освобождения!");
            releaseLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        cellRadius = config.getDouble("cell-radius", 3.0);
        broadcastArrests = config.getBoolean("broadcast-arrests", true);

        // Разрешённые команды (в нижнем регистре)
        allowedCommands = new ArrayList<>();
        for (String cmd : config.getStringList("allowed-commands")) {
            allowedCommands.add(cmd.toLowerCase());
        }
    }

    // ─── Загрузка/сохранение данных заключённых ───

    public void loadPrisoners() {
        if (!dataFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.isConfigurationSection("prisoners")) return;

        for (String uuidStr : data.getConfigurationSection("prisoners").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "prisoners." + uuidStr;

                int time = data.getInt(path + ".time", 0);
                if (time <= 0) continue;

                String worldName = data.getString(path + ".world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Location cellLoc = new Location(world,
                        data.getDouble(path + ".x"),
                        data.getDouble(path + ".y"),
                        data.getDouble(path + ".z"));

                prisonerTimes.put(uuid, time);
                prisonerCells.put(uuid, cellLoc);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректный UUID в данных: " + uuidStr);
            }
        }

        plugin.getLogger().info("Загружено заключённых: " + prisonerTimes.size());
    }

    public void savePrisoners() {
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, Integer> entry : prisonerTimes.entrySet()) {
            UUID uuid = entry.getKey();
            Location cell = prisonerCells.get(uuid);
            if (cell == null || cell.getWorld() == null) continue;

            String path = "prisoners." + uuid.toString();
            data.set(path + ".time", entry.getValue());
            data.set(path + ".world", cell.getWorld().getName());
            data.set(path + ".x", cell.getX());
            data.set(path + ".y", cell.getY());
            data.set(path + ".z", cell.getZ());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить данные заключённых!");
            e.printStackTrace();
        }
    }

    // ─── Основные методы тюрьмы ───

    /**
     * Посадить игрока в тюрьму.
     * Если игрок уже в тюрьме — время ПРИБАВЛЯЕТСЯ, камера выбирается заново.
     */
    public void jailPlayer(Player player, int seconds, String reason) {
        UUID uuid = player.getUniqueId();

        // Добавляем время (если уже сидит — прибавляем)
        int currentTime = prisonerTimes.getOrDefault(uuid, 0);
        prisonerTimes.put(uuid, currentTime + seconds);

        // Выбираем случайную камеру
        Location cell = getRandomCell();
        prisonerCells.put(uuid, cell);

        // Телепортация с задержкой 2 тика (как в оригинале)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(cell);
            }
        }, 2L);

        // Сообщения игроку
        String timeFormatted = formatTimeWords(seconds);
        player.sendMessage("");
        player.sendMessage(colorize(getMessage("arrest-header")));
        player.sendMessage(colorize(getMessage("arrest-reason")
                .replace("%reason%", reason)));
        player.sendMessage(colorize(getMessage("arrest-time")
                .replace("%time%", timeFormatted)));
        player.sendMessage("");

        // Оповещение в чат
        if (broadcastArrests) {
            Bukkit.broadcastMessage(colorize(getMessage("broadcast-arrest")
                    .replace("%player%", player.getName())
                    .replace("%reason%", reason)));
        }

        savePrisoners();
    }

    /**
     * Освободить игрока из тюрьмы.
     */
    public void releasePlayer(UUID uuid) {
        prisonerTimes.remove(uuid);
        prisonerCells.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.teleport(releaseLocation);

            player.sendMessage("");
            player.sendMessage(colorize(getMessage("release-chat")));
            player.sendMessage("");

            player.sendTitle(
                    colorize(getMessage("release-title")),
                    colorize(getMessage("release-subtitle")),
                    10, 50, 10);
        }

        savePrisoners();
    }

    /**
     * Тик таймера — вызывается каждую секунду.
     */
    public void tickTimers() {
        // Создаём копию чтобы избежать ConcurrentModification
        List<UUID> toRelease = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!prisonerTimes.containsKey(uuid)) continue;

            int time = prisonerTimes.get(uuid);

            if (time > 0) {
                time--;
                prisonerTimes.put(uuid, time);

                // Экшн-бар с таймером
                int mins = time / 60;
                int secs = time % 60;
                String msg = colorize(getMessage("actionbar-timer")
                        .replace("%mins%", String.valueOf(mins))
                        .replace("%secs%", String.valueOf(secs)));
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(msg));
            } else {
                toRelease.add(uuid);
            }
        }

        // Освобождаем вне цикла
        for (UUID uuid : toRelease) {
            releasePlayer(uuid);
        }
    }

    // ─── Проверки ───

    public boolean isJailed(UUID uuid) {
        return prisonerTimes.containsKey(uuid);
    }

    public int getTimeRemaining(UUID uuid) {
        return prisonerTimes.getOrDefault(uuid, 0);
    }

    public Location getCellLocation(UUID uuid) {
        return prisonerCells.get(uuid);
    }

    public Map<UUID, Integer> getAllPrisoners() {
        return Collections.unmodifiableMap(prisonerTimes);
    }

    public double getCellRadius() {
        return cellRadius;
    }

    public List<String> getAllowedCommands() {
        return allowedCommands;
    }

    public Location getReleaseLocation() {
        return releaseLocation;
    }

    // ─── Получение значений из конфига ───

    public int getSentenceTime(String key) {
        return plugin.getConfig().getInt("sentences." + key,
                plugin.getConfig().getInt("sentences.default", 600));
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, key);
    }

    // ─── Вспомогательные методы ───

    public Location getRandomCell() {
        if (cellLocations.isEmpty()) {
            plugin.getLogger().warning("Нет доступных камер!");
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        Random random = new Random();
        return cellLocations.get(random.nextInt(cellLocations.size())).clone();
    }

    /**
     * Перевод цветовых кодов (&a, &b и т.д.)
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Форматирование времени с правильным склонением.
     * Например: "6 минут", "3 минуты", "1 минута"
     */
    public String formatTimeWords(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;

        if (mins > 0 && secs == 0) {
            return mins + " " + minuteWord(mins);
        } else if (mins > 0) {
            return mins + " " + minuteWord(mins) + " " + secs + " " + secondWord(secs);
        } else {
            return secs + " " + secondWord(secs);
        }
    }

    /**
     * Русское склонение слова «минута»
     */
    private String minuteWord(int n) {
        int abs = Math.abs(n) % 100;
        int last = abs % 10;
        if (abs >= 11 && abs <= 19) return "минут";
        if (last == 1) return "минута";
        if (last >= 2 && last <= 4) return "минуты";
        return "минут";
    }

    /**
     * Русское склонение слова «секунда»
     */
    private String secondWord(int n) {
        int abs = Math.abs(n) % 100;
        int last = abs % 10;
        if (abs >= 11 && abs <= 19) return "секунд";
        if (last == 1) return "секунда";
        if (last >= 2 && last <= 4) return "секунды";
        return "секунд";
    }
}

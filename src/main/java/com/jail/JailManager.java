package com.jail;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Основной менеджер системы тюрьмы.
 */
public final class JailManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JailPlugin plugin;
    private final File dataFile;

    private final Map<UUID, Integer> prisonerTimes = new HashMap<>();
    private final Map<UUID, Location> prisonerCells = new HashMap<>();
    private final Map<String, Location> cells = new LinkedHashMap<>();

    private Location releaseLocation;
    private double cellRadius;
    private List<String> allowedCommands = List.of();
    private boolean broadcastArrests;

    private final Random random = new Random();

    public JailManager(JailPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "prisoners.yml");
        reloadSettings();
    }

    /** Загрузка настроек. */
    public void reloadSettings() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        cells.clear();

        if (config.isConfigurationSection("cells")) {
            for (String id : config.getConfigurationSection("cells").getKeys(false)) {
                Location location = readLocation(config, "cells." + id);
                if (location != null) {
                    cells.put(id, location);
                }
            }
        }

        releaseLocation = readLocation(config, "release");

        if (releaseLocation == null && !Bukkit.getWorlds().isEmpty()) {
            releaseLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        cellRadius = Math.max(0.5, config.getDouble("cell-radius", 3.0));
        broadcastArrests = config.getBoolean("broadcast-arrests", true);

        allowedCommands = config.getStringList("allowed-commands")
                .stream()
                .map(command -> command.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private Location readLocation(FileConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning(
                    "Мир '" + worldName + "' не найден для настройки: " + path
            );
            return null;
        }

        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw", 0.0),
                (float) config.getDouble(path + ".pitch", 0.0)
        );
    }

    public void loadPrisoners() {
        prisonerTimes.clear();
        prisonerCells.clear();

        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.isConfigurationSection("prisoners")) {
            return;
        }

        for (String uuidText : data.getConfigurationSection("prisoners").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                String path = "prisoners." + uuidText;
                int time = data.getInt(path + ".time", 0);

                if (time <= 0) {
                    continue;
                }

                String cellId = data.getString(path + ".cell");
                Location cell = cellId == null ? null : cells.get(cellId);

                if (cell == null) {
                    String worldName = data.getString(path + ".world", "world");
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        cell = new Location(
                                world,
                                data.getDouble(path + ".x"),
                                data.getDouble(path + ".y"),
                                data.getDouble(path + ".z"),
                                (float) data.getDouble(path + ".yaw", 0.0),
                                (float) data.getDouble(path + ".pitch", 0.0)
                        );
                    }
                }

                if (cell != null) {
                    prisonerTimes.put(uuid, time);
                    prisonerCells.put(uuid, cell);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Некорректный UUID заключённого: " + uuidText
                );
            }
        }
    }

    public void savePrisoners() {
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, Integer> entry : prisonerTimes.entrySet()) {
            Location cell = prisonerCells.get(entry.getKey());
            if (cell == null || cell.getWorld() == null) {
                continue;
            }

            String path = "prisoners." + entry.getKey();
            data.set(path + ".time", entry.getValue());

            String cellId = findCellId(cell);
            if (cellId != null) {
                data.set(path + ".cell", cellId);
            } else {
                data.set(path + ".world", cell.getWorld().getName());
                data.set(path + ".x", cell.getX());
                data.set(path + ".y", cell.getY());
                data.set(path + ".z", cell.getZ());
                data.set(path + ".yaw", cell.getYaw());
                data.set(path + ".pitch", cell.getPitch());
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Не удалось сохранить файл prisoners.yml: " + exception.getMessage()
            );
        }
    }

    private String findCellId(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        for (Map.Entry<String, Location> entry : cells.entrySet()) {
            Location cell = entry.getValue();
            if (cell != null
                    && cell.getWorld() != null
                    && cell.getWorld().equals(location.getWorld())
                    && cell.distanceSquared(location) < 0.01) {
                return entry.getKey();
            }
        }

        return null;
    }

    public void jailPlayer(Player player, int seconds, String reason) {
        if (player == null || cells.isEmpty()) {
            if (player != null) {
                player.sendMessage(component(getMessage("no-cells")));
                player.sendMessage(component(getMessage("no-cells-hint")));
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        int newTime = prisonerTimes.getOrDefault(uuid, 0) + Math.max(1, seconds);
        Location cell = getRandomCell();

        if (cell == null) {
            return;
        }

        prisonerTimes.put(uuid, newTime);
        prisonerCells.put(uuid, cell);
        player.teleport(cell);

        player.sendMessage("");
        player.sendMessage(component(getMessage("arrest-header")));
        player.sendMessage(component(
                getMessage("arrest-reason").replace("%reason%", reason)
        ));
        player.sendMessage(component(
                getMessage("arrest-time").replace("%time%", formatTimeWords(newTime))
        ));

        String cellName = findCellId(cell);
        if (cellName != null) {
            player.sendMessage(component(
                    getMessage("arrest-cell").replace("%cell%", cellName)
            ));
        }

        player.sendMessage("");

        if (broadcastArrests) {
            Bukkit.broadcast(component(
                    getMessage("broadcast-arrest")
                            .replace("%player%", player.getName())
                            .replace("%reason%", reason)
            ));
        }

        savePrisoners();
    }

    public void releasePlayer(UUID uuid) {
        prisonerTimes.remove(uuid);
        prisonerCells.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && releaseLocation != null) {
            player.teleport(releaseLocation);
            player.sendMessage("");
            player.sendMessage(component(getMessage("release-chat")));
            player.sendMessage("");
            player.showTitle(Title.title(
                    component(getMessage("release-title")),
                    component(getMessage("release-subtitle")),
                    Title.Times.times(
                            Duration.ofMillis(300),
                            Duration.ofMillis(1500),
                            Duration.ofMillis(300)
                    )
            ));
        }

        savePrisoners();
    }

    /**
     * Обновляет таймер только для зарегистрированных заключённых,
     * которые находятся онлайн. Это избавляет от полного обхода
     * всех игроков сервера на каждом тике таймера.
     */
    public void tickTimers() {
        if (prisonerTimes.isEmpty()) {
            return;
        }

        List<UUID> toRelease = new ArrayList<>();
        boolean changed = false;

        for (Map.Entry<UUID, Integer> entry : prisonerTimes.entrySet()) {
            UUID uuid = entry.getKey();
            int time = entry.getValue() - 1;

            if (time <= 0) {
                toRelease.add(uuid);
                continue;
            }

            entry.setValue(time);
            changed = true;

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendActionBar(component(
                        getMessage("actionbar-timer")
                                .replace("%time%", formatTime(time))
                ));
            }
        }

        for (UUID uuid : toRelease) {
            releasePlayer(uuid);
        }

        if (changed && toRelease.isEmpty()) {
            savePrisoners();
        }
    }

    public boolean isJailed(UUID uuid) {
        return prisonerTimes.containsKey(uuid);
    }

    public int getTimeRemaining(UUID uuid) {
        return prisonerTimes.getOrDefault(uuid, 0);
    }

    public Location getCellLocation(UUID uuid) {
        Location location = prisonerCells.get(uuid);
        return location == null ? null : location.clone();
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

    public Location getRandomCell() {
        if (cells.isEmpty()) {
            return null;
        }

        int index = random.nextInt(cells.size());
        int current = 0;
        for (Location location : cells.values()) {
            if (current++ == index) {
                return location.clone();
            }
        }

        return null;
    }

    public int getCellCount() {
        return cells.size();
    }

    public Set<String> getCellIds() {
        return Collections.unmodifiableSet(cells.keySet());
    }

    /** Добавляет или обновляет камеру и сразу сохраняет её в config.yml. */
    public void setCell(String id, Location location) {
        if (id == null || id.isBlank() || location == null || location.getWorld() == null) {
            return;
        }

        String normalizedId = id.trim();
        Location stored = location.clone();
        cells.put(normalizedId, stored);

        FileConfiguration config = plugin.getConfig();
        String path = "cells." + normalizedId;
        writeLocation(config, path, stored);
        plugin.saveConfig();
    }

    /** Удаляет камеру из памяти и config.yml. */
    public boolean removeCell(String id) {
        if (id == null) {
            return false;
        }

        Location removed = cells.remove(id);
        if (removed == null) {
            return false;
        }

        plugin.getConfig().set("cells." + id, null);
        plugin.saveConfig();
        return true;
    }

    /** Устанавливает точку освобождения и сразу сохраняет её в config.yml. */
    public void setRelease(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        releaseLocation = location.clone();
        writeLocation(plugin.getConfig(), "release", releaseLocation);
        plugin.saveConfig();
    }

    private void writeLocation(FileConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    public Location getReleaseLocation() {
        return releaseLocation == null ? null : releaseLocation.clone();
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString(
                "messages." + key,
                "&cСообщение не найдено: &f" + key
        );
    }

    public int getSentenceTime(String key) {
        return Math.max(1, plugin.getConfig().getInt("sentences." + key, 60));
    }

    public static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public String formatTime(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int hours = safeSeconds / 3600;
        int minutes = (safeSeconds % 3600) / 60;
        int secs = safeSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs);
        }

        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
    }

    public String formatTimeWords(int seconds) {
        return formatTime(seconds);
    }

    public int getCellRadiusSquaredInt() {
        return (int) Math.ceil(cellRadius * cellRadius);
    }
}

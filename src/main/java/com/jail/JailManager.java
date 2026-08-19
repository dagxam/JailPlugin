package com.jail;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;


/**
 * Основной менеджер системы тюрьмы.
 *
 * Отвечает за:
 *
 * - заключение игроков;
 * - освобождение игроков;
 * - хранение оставшегося времени;
 * - хранение камер;
 * - сохранение данных;
 * - загрузку данных;
 * - работу таймера;
 * - форматирование времени;
 * - получение сообщений из config.yml.
 */
public final class JailManager {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    /**
     * Конфигурация плагина.
     */
    private FileConfiguration config;


    /**
     * Файл с заключёнными.
     */
    private File prisonersFile;


    /**
     * Конфигурация заключённых.
     */
    private FileConfiguration prisonersConfig;


    /**
     * Оставшееся время заключённых.
     *
     * UUID игрока -> секунды.
     */
    private final Map<UUID, Integer> prisonerTimes =
            new HashMap<>();


    /**
     * Причины заключения.
     *
     * UUID игрока -> причина.
     */
    private final Map<UUID, String> prisonerReasons =
            new HashMap<>();


    /**
     * Камеры тюрьмы.
     *
     * Название камеры -> Location.
     */
    private final Map<String, Location> cells =
            new HashMap<>();


    /**
     * Камера, назначенная заключённому.
     *
     * UUID игрока -> название камеры.
     */
    private final Map<UUID, String> prisonerCells =
            new HashMap<>();


    /**
     * Генератор случайных чисел.
     */
    private final Random random =
            new Random();


    /**
     * MiniMessage для сообщений.
     */
    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();


    /**
     * Создаёт менеджер тюрьмы.
     *
     * @param plugin главный класс плагина
     */
    public JailManager(
            JailPlugin plugin
    ) {

        this.plugin = plugin;

        reloadSettings();
    }


    /**
     * Перезагружает настройки плагина.
     */
    public void reloadSettings() {

        plugin.reloadConfig();

        config =
                plugin.getConfig();


        loadCells();

        loadReleaseLocation();
    }


    /**
     * Загружает камеры из config.yml.
     */
    private void loadCells() {

        cells.clear();


        ConfigurationSection section =
                config.getConfigurationSection(
                        "cells"
                );


        if (section == null) {

            return;
        }


        for (
                String cellId :
                section.getKeys(false)
        ) {

            String path =
                    "cells." + cellId;


            Location location =
                    readLocation(
                            path
                    );


            if (location != null) {

                cells.put(
                        cellId,
                        location
                );
            }
        }
    }


    /**
     * Точка освобождения.
     */
    private Location releaseLocation;


    /**
     * Загружает точку освобождения.
     */
    private void loadReleaseLocation() {

        releaseLocation =
                readLocation(
                        "release"
                );
    }


    /**
     * Читает Location из config.yml.
     *
     * @param path путь в конфигурации
     * @return Location либо null
     */
    private Location readLocation(
            String path
    ) {

        String worldName =
                config.getString(
                        path + ".world"
                );


        if (
                worldName == null
                        ||
                worldName.isBlank()
        ) {

            return null;
        }


        if (
                Bukkit.getWorld(
                        worldName
                ) == null
        ) {

            return null;
        }


        double x =
                config.getDouble(
                        path + ".x"
                );


        double y =
                config.getDouble(
                        path + ".y"
                );


        double z =
                config.getDouble(
                        path + ".z"
                );


        float yaw =
                (float) config.getDouble(
                        path + ".yaw"
                );


        float pitch =
                (float) config.getDouble(
                        path + ".pitch"
                );


        return new Location(
                Bukkit.getWorld(
                        worldName
                ),
                x,
                y,
                z,
                yaw,
                pitch
        );
    }


    /**
     * Сохраняет Location в config.yml.
     *
     * @param path путь в конфигурации
     * @param location координаты
     */
    private void writeLocation(
            String path,
            Location location
    ) {

        if (
                location == null
                        ||
                location.getWorld() == null
        ) {

            return;
        }


        config.set(
                path + ".world",
                location
                        .getWorld()
                        .getName()
        );


        config.set(
                path + ".x",
                location.getX()
        );


        config.set(
                path + ".y",
                location.getY()
        );


        config.set(
                path + ".z",
                location.getZ()
        );


        config.set(
                path + ".yaw",
                location.getYaw()
        );


        config.set(
                path + ".pitch",
                location.getPitch()
        );
    }


    /**
     * Загружает заключённых из prisoners.yml.
     */
    public void loadPrisoners() {

        prisonerTimes.clear();

        prisonerReasons.clear();

        prisonerCells.clear();


        prisonersFile =
                new File(
                        plugin.getDataFolder(),
                        "prisoners.yml"
                );


        if (
                !prisonersFile.exists()
        ) {

            try {

                if (
                        !prisonersFile.createNewFile()
                ) {

                    plugin.getLogger().warning(
                            "Не удалось создать prisoners.yml."
                    );

                    return;
                }

            } catch (
                    IOException exception
            ) {

                plugin.getLogger().severe(
                        "Ошибка создания prisoners.yml: "
                                +
                                exception.getMessage()
                );

                return;
            }
        }


        prisonersConfig =
                YamlConfiguration.loadConfiguration(
                        prisonersFile
                );


        ConfigurationSection section =
                prisonersConfig.getConfigurationSection(
                        "prisoners"
                );


        if (section == null) {

            return;
        }


        for (
                String uuidString :
                section.getKeys(false)
        ) {

            try {

                UUID uuid =
                        UUID.fromString(
                                uuidString
                        );


                String path =
                        "prisoners." + uuidString;


                int time =
                        prisonersConfig.getInt(
                                path + ".time",
                                0
                        );


                if (time <= 0) {

                    continue;
                }


                String reason =
                        prisonersConfig.getString(
                                path + ".reason",
                                "неизвестная причина"
                        );


                String cell =
                        prisonersConfig.getString(
                                path + ".cell"
                        );


                prisonerTimes.put(
                        uuid,
                        time
                );


                prisonerReasons.put(
                        uuid,
                        reason
                );


                if (
                        cell != null
                                &&
                        cells.containsKey(
                                cell
                        )
                ) {

                    prisonerCells.put(
                            uuid,
                            cell
                    );
                }

            } catch (
                    IllegalArgumentException exception
            ) {

                plugin.getLogger().warning(
                        "Некорректный UUID в prisoners.yml: "
                                +
                                uuidString
                );
            }
        }
    }


    /**
     * Сохраняет заключённых в prisoners.yml.
     */
    public void savePrisoners() {

        if (
                prisonersConfig == null
        ) {

            prisonersFile =
                    new File(
                            plugin.getDataFolder(),
                            "prisoners.yml"
                    );


            prisonersConfig =
                    YamlConfiguration.loadConfiguration(
                            prisonersFile
                    );
        }


        prisonersConfig.set(
                "prisoners",
                null
        );


        for (
                Map.Entry<UUID, Integer> entry :
                prisonerTimes.entrySet()
        ) {

            UUID uuid =
                    entry.getKey();


            int time =
                    entry.getValue();


            if (time <= 0) {

                continue;
            }


            String path =
                    "prisoners."
                            +
                            uuid;


            prisonersConfig.set(
                    path + ".time",
                    time
            );


            prisonersConfig.set(
                    path + ".reason",
                    prisonerReasons.getOrDefault(
                            uuid,
                            "неизвестная причина"
                    )
            );


            String cell =
                    prisonerCells.get(
                            uuid
                    );


            if (cell != null) {

                prisonersConfig.set(
                        path + ".cell",
                        cell
                );
            }
        }


        try {

            prisonersConfig.save(
                    prisonersFile
            );

        } catch (
                IOException exception
        ) {

            plugin.getLogger().severe(
                    "Не удалось сохранить prisoners.yml: "
                            +
                            exception.getMessage()
            );
        }
    }


    /**
     * Заключает игрока в тюрьму.
     *
     * Если игрок уже находится в тюрьме,
     * новый срок добавляется к существующему.
     *
     * @param player игрок
     * @param seconds новый срок в секундах
     * @param reason причина
     */
    public void jailPlayer(
            Player player,
            int seconds,
            String reason
    ) {

        if (
                player == null
        ) {

            return;
        }


        UUID uuid =
                player.getUniqueId();


        /*
         * Новый срок добавляется
         * к уже существующему.
         */

        int newTime =
                prisonerTimes.getOrDefault(
                        uuid,
                        0
                )
                +
                Math.max(
                        1,
                        seconds
                );


        prisonerTimes.put(
                uuid,
                newTime
        );


        prisonerReasons.put(
                uuid,
                reason
        );


        /*
         * Если у игрока ещё нет камеры,
         * назначаем случайную.
         */

        if (
                !prisonerCells.containsKey(
                        uuid
                )
        ) {

            Location randomCell =
                    getRandomCell();


            if (randomCell != null) {

                String cellId =
                        findCellId(
                                randomCell
                        );


                if (cellId != null) {

                    prisonerCells.put(
                            uuid,
                            cellId
                    );
                }
            }
        }


        /*
         * Получаем камеру заключённого.
         */

        Location cell =
                getCellLocation(
                        uuid
                );


        /*
         * Телепортируем игрока
         * в камеру.
         */

        if (cell != null) {

            player.teleport(
                    cell
            );
        }


        /*
         * Показываем игроку информацию
         * о заключении.
         *
         * ВАЖНО:
         *
         * Здесь используется newTime,
         * а не seconds.
         *
         * Поэтому при повторном заключении
         * игрок видит полный итоговый срок.
         */

        player.sendMessage("");

        player.sendMessage(

                component(

                        getMessage(
                                "arrest-header"
                        )
                )
        );


        player.sendMessage(

                component(

                        getMessage(
                                "arrest-reason"
                        )
                                .replace(
                                        "%reason%",
                                        reason
                                )
                )
        );


        player.sendMessage(

                component(

                        getMessage(
                                "arrest-time"
                        )
                                .replace(
                                        "%time%",
                                        formatTimeWords(
                                                newTime
                                        )
                                )
                )
        );


        if (cell != null) {

            String cellId =
                    prisonerCells.get(
                            uuid
                    );


            if (cellId != null) {

                player.sendMessage(

                        component(

                                getMessage(
                                        "arrest-cell"
                                )
                                        .replace(
                                                "%cell%",
                                                cellId
                                        )
                        )
                );
            }
        }


        player.sendMessage("");


        /*
         * Если включена трансляция арестов,
         * сообщаем всем игрокам.
         */

        if (
                config.getBoolean(
                        "broadcast-arrests",
                        true
                )
        ) {

            Bukkit.broadcast(

                    component(

                            getMessage(
                                    "broadcast-arrest"
                            )

                                    .replace(
                                            "%player%",
                                            player.getName()
                                    )

                                    .replace(
                                            "%reason%",
                                            reason
                                    )
                    )
            );
        }


        /*
         * Сохраняем данные.
         */

        savePrisoners();
    }


    /**
     * Освобождает игрока.
     *
     * @param uuid UUID игрока
     */
    public void releasePlayer(
            UUID uuid
    ) {

        if (
                uuid == null
        ) {

            return;
        }


        prisonerTimes.remove(
                uuid
        );


        prisonerReasons.remove(
                uuid
        );


        prisonerCells.remove(
                uuid
        );


        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        if (player != null) {

            /*
             * Телепортируем игрока
             * в точку освобождения.
             */

            if (
                    releaseLocation != null
            ) {

                player.teleport(
                        releaseLocation
                );
            }


            player.sendMessage("");

            player.sendMessage(

                    component(

                            getMessage(
                                    "release-chat"
                            )
                    )
            );


            player.sendMessage("");
        }


        savePrisoners();
    }


    /**
     * Проверяет, находится ли игрок
     * в тюрьме.
     *
     * @param uuid UUID игрока
     * @return true, если игрок заключён
     */
    public boolean isJailed(
            UUID uuid
    ) {

        return uuid != null
                &&
                prisonerTimes.containsKey(
                        uuid
                )
                &&
                prisonerTimes.get(
                        uuid
                ) > 0;
    }


    /**
     * Возвращает оставшееся время.
     *
     * @param uuid UUID игрока
     * @return секунды
     */
    public int getTimeRemaining(
            UUID uuid
    ) {

        return prisonerTimes.getOrDefault(
                uuid,
                0
        );
    }


    /**
     * Возвращает причину заключения.
     *
     * @param uuid UUID игрока
     * @return причина
     */
    public String getPrisonerReason(
            UUID uuid
    ) {

        return prisonerReasons.getOrDefault(
                uuid,
                "неизвестная причина"
        );
    }


    /**
     * Возвращает карту всех заключённых.
     *
     * @return UUID -> оставшееся время
     */
    public Map<UUID, Integer> getAllPrisoners() {

        return Collections.unmodifiableMap(
                prisonerTimes
        );
    }


    /**
     * Обновляет таймеры заключённых.
     *
     * Вызывается JailTimer один раз в секунду.
     */
    public void tickTimers() {

        if (
                prisonerTimes.isEmpty()
        ) {

            return;
        }


        List<UUID> released =
                new ArrayList<>();


        for (
                Map.Entry<UUID, Integer> entry :
                new ArrayList<>(
                        prisonerTimes.entrySet()
                )
        ) {

            UUID uuid =
                    entry.getKey();


            int time =
                    entry.getValue();


            time--;


            if (time <= 0) {

                released.add(
                        uuid
                );

                continue;
            }


            prisonerTimes.put(
                    uuid,
                    time
            );


            Player player =
                    Bukkit.getPlayer(
                            uuid
                    );


            if (player != null) {

                player.sendActionBar(

                        component(

                                getMessage(
                                        "actionbar-timer"
                                )

                                        .replace(
                                                "%time%",
                                                formatTime(
                                                        time
                                                )
                                        )
                        )
                );
            }
        }


        /*
         * Освобождаем игроков,
         * у которых срок закончился.
         */

        for (
                UUID uuid :
                released
        ) {

            releasePlayer(
                    uuid
            );
        }


        /*
         * Периодически сохраняем данные.
         */

        savePrisoners();
    }


    /**
     * Возвращает Location камеры,
     * назначенной заключённому.
     *
     * @param uuid UUID игрока
     * @return Location камеры
     */
    public Location getCellLocation(
            UUID uuid
    ) {

        String cellId =
                prisonerCells.get(
                        uuid
                );


        if (cellId == null) {

            return null;
        }


        return cells.get(
                cellId
        );
    }


    /**
     * Возвращает название камеры
     * заключённого.
     *
     * @param uuid UUID игрока
     * @return название камеры
     */
    public String getPrisonerCell(
            UUID uuid
    ) {

        return prisonerCells.get(
                uuid
        );
    }


    /**
     * Возвращает случайную камеру.
     *
     * @return случайная камера либо null
     */
    public Location getRandomCell() {

        if (
                cells.isEmpty()
        ) {

            return null;
        }


        List<Location> locations =
                new ArrayList<>(
                        cells.values()
                );


        return locations.get(
                random.nextInt(
                        locations.size()
                )
        );
    }


    /**
     * Находит ID камеры по Location.
     *
     * @param location Location камеры
     * @return ID камеры либо null
     */
    private String findCellId(
            Location location
    ) {

        if (location == null) {

            return null;
        }


        for (
                Map.Entry<String, Location> entry :
                cells.entrySet()
        ) {

            Location stored =
                    entry.getValue();


            if (
                    sameLocation(
                            stored,
                            location
                    )
            ) {

                return entry.getKey();
            }
        }


        return null;
    }


    /**
     * Сравнивает две Location.
     */
    private boolean sameLocation(
            Location first,
            Location second
    ) {

        if (
                first == null
                        ||
                second == null
        ) {

            return false;
        }


        if (
                first.getWorld() == null
                        ||
                second.getWorld() == null
        ) {

            return false;
        }


        if (
                !first
                        .getWorld()
                        .getName()
                        .equals(
                                second
                                        .getWorld()
                                        .getName()
                        )
        ) {

            return false;
        }


        return Double.compare(
                first.getX(),
                second.getX()
        ) == 0

                &&

                Double.compare(
                        first.getY(),
                        second.getY()
                ) == 0

                &&

                Double.compare(
                        first.getZ(),
                        second.getZ()
                ) == 0;
    }


    /**
     * Создаёт или изменяет камеру.
     *
     * @param id название камеры
     * @param location координаты
     * @return true при успехе
     */
    public boolean setCell(
            String id,
            Location location
    ) {

        if (
                id == null
                        ||
                id.isBlank()
                        ||
                location == null
                        ||
                location.getWorld() == null
        ) {

            return false;
        }


        cells.put(
                id,
                location.clone()
        );


        writeLocation(
                "cells." + id,
                location
        );


        try {

            plugin.saveConfig();

        } catch (
                Exception exception
        ) {

            plugin.getLogger().warning(
                    "Не удалось сохранить камеру "
                            +
                            id
                            +
                            ": "
                            +
                            exception.getMessage()
            );

            return false;
        }


        return true;
    }


    /**
     * Удаляет камеру.
     *
     * @param id название камеры
     * @return true, если камера существовала
     */
    public boolean removeCell(
            String id
    ) {

        if (
                id == null
                        ||
                id.isBlank()
        ) {

            return false;
        }


        if (
                !cells.containsKey(
                        id
                )
        ) {

            return false;
        }


        cells.remove(
                id
        );


        config.set(
                "cells." + id,
                null
        );


        /*
         * Удаляем назначение камеры
         * у заключённых.
         */

        prisonerCells
                .entrySet()
                .removeIf(

                        entry ->

                                id.equals(
                                        entry.getValue()
                                )
                );


        plugin.saveConfig();

        savePrisoners();


        return true;
    }


    /**
     * Возвращает количество камер.
     *
     * @return количество камер
     */
    public int getCellCount() {

        return cells.size();
    }


    /**
     * Возвращает список ID камер.
     *
     * @return список камер
     */
    public List<String> getCellIds() {

        return new ArrayList<>(
                cells.keySet()
        );
    }


    /**
     * Устанавливает точку освобождения.
     *
     * @param location координаты
     */
    public void setRelease(
            Location location
    ) {

        if (
                location == null
        ) {

            return;
        }


        releaseLocation =
                location.clone();


        writeLocation(
                "release",
                location
        );


        plugin.saveConfig();
    }


    /**
     * Возвращает точку освобождения.
     *
     * @return Location либо null
     */
    public Location getReleaseLocation() {

        return releaseLocation;
    }


    /**
     * Возвращает радиус камеры.
     *
     * @return радиус
     */
    public double getCellRadius() {

        return Math.max(
                0.0,
                config.getDouble(
                        "cell-radius",
                        3.0
                )
        );
    }


    /**
     * Возвращает срок из конфигурации.
     *
     * @param type тип наказания
     * @return срок в секундах
     */
    public int getSentenceTime(
            String type
    ) {

        int seconds =
                config.getInt(
                        "sentences." + type,
                        config.getInt(
                                "sentences.default",
                                600
                        )
                );


        return Math.max(
                1,
                seconds
        );
    }


    /**
     * Возвращает разрешённые команды.
     *
     * @return список команд
     */
    public List<String> getAllowedCommands() {

        List<String> commands =
                config.getStringList(
                        "allowed-commands"
                );


        List<String> result =
                new ArrayList<>();


        for (
                String command :
                commands
        ) {

            if (
                    command == null
                            ||
                    command.isBlank()
            ) {

                continue;
            }


            String normalized =
                    command
                            .trim()
                            .toLowerCase();


            if (
                    normalized.startsWith(
                            "/"
                    )
            ) {

                normalized =
                        normalized.substring(
                                1
                        );
            }


            result.add(
                    normalized
            );
        }


        return result;
    }


    /**
     * Получает сообщение из config.yml.
     *
     * @param key ключ сообщения
     * @return сообщение
     */
    public String getMessage(
            String key
    ) {

        return config.getString(
                "messages." + key,
                "&cСообщение не найдено: " + key
        );
    }


    /**
     * Преобразует строку с цветами
     * в Adventure Component.
     *
     * Поддерживает стандартные
     * Minecraft-цвета через &.
     *
     * @param text текст
     * @return Component
     */
    public static Component component(
            String text
    ) {

        if (
                text == null
        ) {

            return Component.empty();
        }


        /*
         * Поддержка старого формата:
         *
         * &a
         * &c
         * &6
         * &l
         * и т.д.
         */

        String mini =
                text
                        .replace(
                                "&0",
                                "<black>"
                        )
                        .replace(
                                "&1",
                                "<dark_blue>"
                        )
                        .replace(
                                "&2",
                                "<dark_green>"
                        )
                        .replace(
                                "&3",
                                "<dark_aqua>"
                        )
                        .replace(
                                "&4",
                                "<dark_red>"
                        )
                        .replace(
                                "&5",
                                "<dark_purple>"
                        )
                        .replace(
                                "&6",
                                "<gold>"
                        )
                        .replace(
                                "&7",
                                "<gray>"
                        )
                        .replace(
                                "&8",
                                "<dark_gray>"
                        )
                        .replace(
                                "&9",
                                "<blue>"
                        )
                        .replace(
                                "&a",
                                "<green>"
                        )
                        .replace(
                                "&b",
                                "<aqua>"
                        )
                        .replace(
                                "&c",
                                "<red>"
                        )
                        .replace(
                                "&d",
                                "<light_purple>"
                        )
                        .replace(
                                "&e",
                                "<yellow>"
                        )
                        .replace(
                                "&f",
                                "<white>"
                        )
                        .replace(
                                "&k",
                                "<obfuscated>"
                        )
                        .replace(
                                "&l",
                                "<bold>"
                        )
                        .replace(
                                "&m",
                                "<strikethrough>"
                        )
                        .replace(
                                "&n",
                                "<underlined>"
                        )
                        .replace(
                                "&o",
                                "<italic>"
                        )
                        .replace(
                                "&r",
                                "<reset>"
                        );


        return MINI_MESSAGE.deserialize(
                mini
        );
    }


    /**
     * Форматирует время в компактном формате.
     *
     * Пример:
     *
     * 3661 -> 1ч 1м 1с
     *
     * @param seconds секунды
     * @return форматированное время
     */
    public String formatTime(
            int seconds
    ) {

        seconds =
                Math.max(
                        0,
                        seconds
                );


        int hours =
                seconds / 3600;


        int minutes =
                (
                        seconds % 3600
                )
                        /
                        60;


        int remainingSeconds =
                seconds % 60;


        if (hours > 0) {

            return hours
                    +
                    "ч "
                    +
                    minutes
                    +
                    "м "
                    +
                    remainingSeconds
                    +
                    "с";
        }


        if (minutes > 0) {

            return minutes
                    +
                    "м "
                    +
                    remainingSeconds
                    +
                    "с";
        }


        return remainingSeconds
                +
                "с";
    }


    /**
     * Форматирует время естественным русским языком.
     *
     * Примеры:
     *
     * 1 -> 1 секунда
     * 2 -> 2 секунды
     * 5 -> 5 секунд
     * 61 -> 1 минута 1 секунда
     *
     * @param seconds секунды
     * @return форматированное время
     */
    public String formatTimeWords(
            int seconds
    ) {

        seconds =
                Math.max(
                        0,
                        seconds
                );


        int hours =
                seconds / 3600;


        int minutes =
                (
                        seconds % 3600
                )
                        /
                        60;


        int remainingSeconds =
                seconds % 60;


        List<String> parts =
                new ArrayList<>();


        if (hours > 0) {

            parts.add(
                    hours
                            +
                            " "
                            +
                            plural(
                                    hours,
                                    "час",
                                    "часа",
                                    "часов"
                            )
            );
        }


        if (minutes > 0) {

            parts.add(
                    minutes
                            +
                            " "
                            +
                            plural(
                                    minutes,
                                    "минута",
                                    "минуты",
                                    "минут"
                            )
            );
        }


        if (
                remainingSeconds > 0
                        ||
                parts.isEmpty()
        ) {

            parts.add(
                    remainingSeconds
                            +
                            " "
                            +
                            plural(
                                    remainingSeconds,
                                    "секунда",
                                    "секунды",
                                    "секунд"
                            )
            );
        }


        return String.join(
                " ",
                parts
        );
    }


    /**
     * Выбирает правильную форму слова.
     *
     * @param number число
     * @param one форма для 1
     * @param few форма для 2-4
     * @param many форма для 5-0
     * @return правильная форма
     */
    private String plural(
            int number,
            String one,
            String few,
            String many
    ) {

        int lastTwo =
                number % 100;


        int last =
                number % 10;


        if (
                lastTwo >= 11
                        &&
                lastTwo <= 14
        ) {

            return many;
        }


        if (last == 1) {

            return one;
        }


        if (
                last >= 2
                        &&
                last <= 4
        ) {

            return few;
        }


        return many;
    }

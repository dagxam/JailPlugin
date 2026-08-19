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
 *
 * Отвечает за:
 *
 * - камеры;
 * - заключённых;
 * - сроки наказания;
 * - сохранение заключённых;
 * - освобождение;
 * - таймер;
 * - точку освобождения;
 * - сообщения игрокам.
 */
public final class JailManager {

    /**
     * Преобразователь старого формата цветов Minecraft:
     *
     * &c
     * &a
     * &7
     * &l
     *
     * в Adventure Component.
     */
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();


    private final JailPlugin plugin;

    /**
     * Файл с текущими заключёнными.
     */
    private final File dataFile;


    /**
     * Срок каждого заключённого в секундах.
     */
    private final Map<UUID, Integer> prisonerTimes =
            new HashMap<>();


    /**
     * Камера, в которой находится заключённый.
     */
    private final Map<UUID, Location> prisonerCells =
            new HashMap<>();


    /**
     * Все камеры тюрьмы.
     *
     * Например:
     *
     * cell-1
     * cell-2
     * cell-3
     */
    private final Map<String, Location> cells =
            new LinkedHashMap<>();


    /**
     * Точка освобождения.
     */
    private Location releaseLocation;


    /**
     * Радиус, за который заключённому нельзя выходить.
     */
    private double cellRadius;


    /**
     * Команды, разрешённые заключённым.
     */
    private List<String> allowedCommands =
            new ArrayList<>();


    /**
     * Показывать ли всему серверу сообщение об аресте.
     */
    private boolean broadcastArrests;


    /**
     * Генератор случайных камер.
     */
    private final Random random =
            new Random();


    public JailManager(
            JailPlugin plugin
    ) {

        this.plugin = plugin;

        this.dataFile = new File(
                plugin.getDataFolder(),
                "prisoners.yml"
        );

        reloadSettings();
    }


    /**
     * Загружает настройки из config.yml.
     */
    public void reloadSettings() {

        plugin.reloadConfig();

        FileConfiguration config =
                plugin.getConfig();


        /*
         * Загружаем камеры.
         */

        cells.clear();


        if (
                config.isConfigurationSection(
                        "cells"
                )
        ) {

            for (
                    String id :
                    config
                            .getConfigurationSection(
                                    "cells"
                            )
                            .getKeys(false)
            ) {

                String path =
                        "cells." + id;


                Location location =
                        readLocation(
                                config,
                                path
                        );


                if (location != null) {

                    cells.put(
                            id,
                            location
                    );
                }
            }
        }


        /*
         * Загружаем точку освобождения.
         */

        releaseLocation =
                readLocation(
                        config,
                        "release"
                );


        if (releaseLocation == null) {

            World world =
                    Bukkit.getWorlds().isEmpty()
                            ? null
                            : Bukkit.getWorlds().get(0);


            if (world != null) {

                releaseLocation =
                        world.getSpawnLocation();
            }
        }


        /*
         * Радиус камеры.
         */

        cellRadius =
                Math.max(
                        0.5,
                        config.getDouble(
                                "cell-radius",
                                3.0
                        )
                );


        /*
         * Сообщение всему серверу об аресте.
         */

        broadcastArrests =
                config.getBoolean(
                        "broadcast-arrests",
                        true
                );


        /*
         * Разрешённые команды.
         */

        allowedCommands =
                new ArrayList<>();


        for (
                String command :
                config.getStringList(
                        "allowed-commands"
                )
        ) {

            allowedCommands.add(
                    command
                            .toLowerCase(
                                    Locale.ROOT
                            )
            );
        }
    }


    /**
     * Читает Location из YAML.
     */
    private Location readLocation(
            FileConfiguration config,
            String path
    ) {

        String worldName =
                config.getString(
                        path + ".world"
                );


        if (worldName == null) {

            return null;
        }


        World world =
                Bukkit.getWorld(
                        worldName
                );


        if (world == null) {

            plugin.getLogger().warning(
                    "Мир '" +
                            worldName +
                            "' не найден для настройки: " +
                            path
            );

            return null;
        }


        return new Location(

                world,

                config.getDouble(
                        path + ".x"
                ),

                config.getDouble(
                        path + ".y"
                ),

                config.getDouble(
                        path + ".z"
                ),

                (float) config.getDouble(
                        path + ".yaw",
                        0.0
                ),

                (float) config.getDouble(
                        path + ".pitch",
                        0.0
                )
        );
    }


    /**
     * Загружает заключённых из prisoners.yml.
     */
    public void loadPrisoners() {

        prisonerTimes.clear();

        prisonerCells.clear();


        if (!dataFile.exists()) {

            return;
        }


        FileConfiguration data =
                YamlConfiguration
                        .loadConfiguration(
                                dataFile
                        );


        if (
                !data.isConfigurationSection(
                        "prisoners"
                )
        ) {

            return;
        }


        for (
                String uuidText :
                data
                        .getConfigurationSection(
                                "prisoners"
                        )
                        .getKeys(false)
        ) {

            try {

                UUID uuid =
                        UUID.fromString(
                                uuidText
                        );


                String path =
                        "prisoners." +
                                uuidText;


                int time =
                        data.getInt(
                                path + ".time",
                                0
                        );


                if (time <= 0) {

                    continue;
                }


                String cellId =
                        data.getString(
                                path + ".cell"
                        );


                Location cell = null;


                /*
                 * Сначала пытаемся найти камеру
                 * по её названию.
                 */

                if (cellId != null) {

                    cell =
                            cells.get(
                                    cellId
                            );
                }


                /*
                 * Если камера больше не существует,
                 * пробуем восстановить координаты.
                 */

                if (cell == null) {

                    String worldName =
                            data.getString(
                                    path + ".world",
                                    "world"
                            );


                    World world =
                            Bukkit.getWorld(
                                    worldName
                            );


                    if (world != null) {

                        cell =
                                new Location(

                                        world,

                                        data.getDouble(
                                                path + ".x"
                                        ),

                                        data.getDouble(
                                                path + ".y"
                                        ),

                                        data.getDouble(
                                                path + ".z"
                                        ),

                                        (float) data.getDouble(
                                                path + ".yaw",
                                                0.0
                                        ),

                                        (float) data.getDouble(
                                                path + ".pitch",
                                                0.0
                                        )
                                );
                    }
                }


                if (cell != null) {

                    prisonerTimes.put(
                            uuid,
                            time
                    );


                    prisonerCells.put(
                            uuid,
                            cell
                    );
                }


            } catch (Exception exception) {

                plugin.getLogger().warning(
                        "Не удалось загрузить заключённого: " +
                                uuidText
                );
            }
        }
    }


    /**
     * Сохраняет всех заключённых.
     */
    public void savePrisoners() {

        FileConfiguration data =
                new YamlConfiguration();


        for (
                UUID uuid :
                prisonerTimes.keySet()
        ) {

            Location cell =
                    prisonerCells.get(
                            uuid
                    );


            if (
                    cell == null
                            || cell.getWorld() == null
            ) {

                continue;
            }


            String path =
                    "prisoners." +
                            uuid;


            /*
             * Сохраняем оставшееся время.
             */

            data.set(
                    path + ".time",
                    prisonerTimes.get(
                            uuid
                    )
            );


            /*
             * Если камера известна по имени,
             * сохраняем её название.
             */

            String cellId =
                    findCellId(
                            cell
                    );


            if (cellId != null) {

                data.set(
                        path + ".cell",
                        cellId
                );

            } else {

                /*
                 * Если камера была удалена,
                 * сохраняем её координаты.
                 */

                data.set(
                        path + ".world",
                        cell.getWorld().getName()
                );


                data.set(
                        path + ".x",
                        cell.getX()
                );


                data.set(
                        path + ".y",
                        cell.getY()
                );


                data.set(
                        path + ".z",
                        cell.getZ()
                );


                data.set(
                        path + ".yaw",
                        cell.getYaw()
                );


                data.set(
                        path + ".pitch",
                        cell.getPitch()
                );
            }
        }


        try {

            data.save(
                    dataFile
            );

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Не удалось сохранить файл prisoners.yml."
            );

            exception.printStackTrace();
        }
    }


    /**
     * Находит название камеры по Location.
     */
    private String findCellId(
            Location location
    ) {

        for (
                Map.Entry<String, Location> entry :
                cells.entrySet()
        ) {

            Location cell =
                    entry.getValue();


            if (
                    cell.getWorld().equals(
                            location.getWorld()
                    )

                            &&

                    cell.distanceSquared(
                            location
                    ) < 0.01
            ) {

                return entry.getKey();
            }
        }


        return null;
    }


    /**
     * Заключает игрока в тюрьму.
     *
     * @param player игрок
     * @param seconds срок в секундах
     * @param reason причина заключения
     */
    public void jailPlayer(
            Player player,
            int seconds,
            String reason
    ) {

        /*
         * Без камер заключить игрока невозможно.
         */

        if (cells.isEmpty()) {

            player.sendMessage(
                    component(
                            getMessage(
                                    "no-cells"
                            )
                    )
            );


            player.sendMessage(
                    component(
                            getMessage(
                                    "no-cells-hint"
                            )
                    )
            );


            return;
        }


        UUID uuid =
                player.getUniqueId();


        /*
         * Если игрок уже сидит,
         * добавляем новый срок к старому.
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


        /*
         * Выбираем случайную камеру.
         */

        Location cell =
                getRandomCell();


        if (cell == null) {

            return;
        }


        prisonerTimes.put(
                uuid,
                newTime
        );


        prisonerCells.put(
                uuid,
                cell
        );


        /*
         * Телепортируем игрока.
         */

        player.teleport(
                cell
        );


        /*
         * Сообщение игроку.
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


        String cellName =
                findCellId(
                        cell
                );


        if (cellName != null) {

            player.sendMessage(
                    component(
                            getMessage(
                                    "arrest-cell"
                            )
                                    .replace(
                                            "%cell%",
                                            cellName
                                    )
                    )
            );
        }


        player.sendMessage("");


        /*
         * Сообщение всему серверу.
         */

        if (broadcastArrests) {

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


        savePrisoners();
    }


    /**
     * Освобождает игрока.
     */
    public void releasePlayer(
            UUID uuid
    ) {

        prisonerTimes.remove(
                uuid
        );


        prisonerCells.remove(
                uuid
        );


        Player player =
                Bukkit.getPlayer(
                        uuid
                );


        if (
                player != null
                        && player.isOnline()
                        && releaseLocation != null
        ) {

            player.teleport(
                    releaseLocation
            );


            player.sendMessage("");


            player.sendMessage(
                    component(
                            getMessage(
                                    "release-chat"
                            )
                    )
            );


            player.sendMessage("");


            /*
             * Большой заголовок на экране.
             */

            player.showTitle(

                    Title.title(

                            component(
                                    getMessage(
                                            "release-title"
                                    )
                            ),

                            component(
                                    getMessage(
                                            "release-subtitle"
                                    )
                            ),

                            Title.Times.times(

                                    Duration.ofMillis(
                                            300
                                    ),

                                    Duration.ofMillis(
                                            1500
                                    ),

                                    Duration.ofMillis(
                                            300
                                    )
                            )
                    )
            );
        }


        savePrisoners();
    }


    /**
     * Обновляет таймер заключённых.
     *
     * Вызывается каждую секунду.
     */
    public void tickTimers() {

        List<UUID> toRelease =
                new ArrayList<>();


        for (
                Player player :
                Bukkit.getOnlinePlayers()
        ) {

            UUID uuid =
                    player.getUniqueId();


            Integer current =
                    prisonerTimes.get(
                            uuid
                    );


            /*
             * Игрок не заключён.
             */

            if (current == null) {

                continue;
            }


            /*
             * Уменьшаем срок на одну секунду.
             */

            int time =
                    current - 1;


            /*
             * Срок закончился.
             */

            if (time <= 0) {

                toRelease.add(
                        uuid
                );


                continue;
            }


            prisonerTimes.put(
                    uuid,
                    time
            );


            /*
             * Показываем красивый таймер.
             */

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


        /*
         * Освобождаем игроков,
         * у которых закончился срок.
         */

        for (
                UUID uuid :
                toRelease
        ) {

            releasePlayer(
                    uuid
            );
        }
    }


    /**
     * Проверяет, находится ли игрок в тюрьме.
     */
    public boolean isJailed(
            UUID uuid
    ) {

        return prisonerTimes.containsKey(
                uuid
        );
    }


    /**
     * Возвращает оставшееся время.
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
     * Возвращает камеру заключённого.
     */
    public Location getCellLocation(
            UUID uuid
    ) {

        Location location =
                prisonerCells.get(
                        uuid
                );


        return location == null
                ? null
                : location.clone();
    }


    /**
     * Возвращает всех заключённых.
     */
    public Map<UUID, Integer> getAllPrisoners() {

        return Collections.unmodifiableMap(
                prisonerTimes
        );
    }


    /**
     * Радиус камеры.
     */
    public double getCellRadius() {

        return cellRadius;
    }


    /**
     * Разрешённые команды.
     */
    public List<String> getAllowedCommands() {

        return Collections.unmodifiableList(
                allowedCommands
        );
    }


    /**
     * Возвращает точку освобождения.
     */
    public Location getReleaseLocation() {

        return releaseLocation == null
                ? null
                : releaseLocation.clone();
    }


    /**
     * Количество камер.
     */
    public int getCellCount() {

        return cells.size();
    }


    /**
     * Названия всех камер.
     */
    public Set<String> getCellIds() {

        return Collections.unmodifiableSet(
                cells.keySet()
        );
    }


    /**
     * Возвращает случайную камеру.
     */
    public Location getRandomCell() {

        if (cells.isEmpty()) {

            return null;
        }


        List<Location> list =
                new ArrayList<>(
                        cells.values()
                );


        return list.get(
                random.nextInt(
                        list.size()
                )
        ).clone();
    }


    /**
     * Создаёт или изменяет камеру.
     */
    public boolean setCell(
            String id,
            Location location
    ) {

        if (
                id == null
                        || id.isBlank()
                        || location == null
                        || location.getWorld() == null
        ) {

            return false;
        }


        String path =
                "cells." + id;


        plugin.getConfig().set(
                path + ".world",
                location.getWorld().getName()
        );


        plugin.getConfig().set(
                path + ".x",
                location.getX()
        );


        plugin.getConfig().set(
                path + ".y",
                location.getY()
        );


        plugin.getConfig().set(
                path + ".z",
                location.getZ()
        );


        plugin.getConfig().set(
                path + ".yaw",
                location.getYaw()
        );


        plugin.getConfig().set(
                path + ".pitch",
                location.getPitch()
        );


        plugin.saveConfig();


        reloadSettings();


        return true;
    }


    /**
     * Удаляет камеру.
     */
    public boolean removeCell(
            String id
    ) {

        if (
                !cells.containsKey(
                        id
                )
        ) {

            return false;
        }


        plugin.getConfig().set(
                "cells." + id,
                null
        );


        plugin.saveConfig();


        reloadSettings();


        return true;
    }


    /**
     * Устанавливает точку освобождения.
     */
    public void setRelease(
            Location location
    ) {

        plugin.getConfig().set(
                "release.world",
                location.getWorld().getName()
        );


        plugin.getConfig().set(
                "release.x",
                location.getX()
        );


        plugin.getConfig().set(
                "release.y",
                location.getY()
        );


        plugin.getConfig().set(
                "release.z",
                location.getZ()
        );


        plugin.getConfig().set(
                "release.yaw",
                location.getYaw()
        );


        plugin.getConfig().set(
                "release.pitch",
                location.getPitch()
        );


        plugin.saveConfig();


        reloadSettings();
    }


    /**
     * Возвращает срок наказания.
     */
    public int getSentenceTime(
            String key
    ) {

        return plugin
                .getConfig()
                .getInt(

                        "sentences." + key,

                        plugin
                                .getConfig()
                                .getInt(
                                        "sentences.default",
                                        600
                                )
                );
    }


    /**
     * Получает сообщение из config.yml.
     */
    public String getMessage(
            String key
    ) {

        return plugin
                .getConfig()
                .getString(
                        "messages." + key,
                        key
                );
    }


    /**
     * Форматирует время:
     *
     * 1 секунда
     * 2 секунды
     * 5 секунд
     *
     * 1 минута
     * 2 минуты
     * 5 минут
     *
     * 1 минута 20 секунд
     */
    public String formatTime(
            int totalSeconds
    ) {

        int minutes =
                totalSeconds / 60;


        int seconds =
                totalSeconds % 60;


        if (minutes > 0 && seconds > 0) {

            return minutes +
                    " " +
                    minuteWord(
                            minutes
                    ) +

                    " " +

                    seconds +
                    " " +
                    secondWord(
                            seconds
                    );
        }


        if (minutes > 0) {

            return minutes +
                    " " +
                    minuteWord(
                            minutes
                    );
        }


        return seconds +
                " " +
                secondWord(
                        seconds
                );
    }


    /**
     * Старый метод оставлен для совместимости.
     */
    public String formatTimeWords(
            int seconds
    ) {

        return formatTime(
                seconds
        );
    }


    /**
     * Склонение слова "минута".
     */
    private String minuteWord(
            int number
    ) {

        int value =
                Math.abs(
                        number
                ) % 100;


        int last =
                value % 10;


        if (
                value >= 11
                        && value <= 19
        ) {

            return "минут";
        }


        if (last == 1) {

            return "минута";
        }


        if (
                last >= 2
                        && last <= 4
        ) {

            return "минуты";
        }


        return "минут";
    }


    /**
     * Склонение слова "секунда".
     */
    private String secondWord(
            int number
    ) {

        int value =
                Math.abs(
                        number
                ) % 100;


        int last =
                value % 10;


        if (
                value >= 11
                        && value <= 19
        ) {

            return "секунд";
        }


        if (last == 1) {

            return "секунда";
        }


        if (
                last >= 2
                        && last <= 4
        ) {

            return "секунды";
        }


        return "секунд";
    }


    /**
     * Преобразует &c, &a и другие
     * цветовые коды в Component.
     */
    public static Component component(
            String text
    ) {

        return LEGACY.deserialize(
                text == null
                        ? ""
                        : text
        );
    }


    /**
     * Преобразует текст в Legacy-формат.
     *
     * Оставлено для совместимости.
     */
    public static String colorize(
            String text
    ) {

        if (text == null) {

            return "";
        }


        return LEGACY.serialize(
                LEGACY.deserialize(
                        text
                )
        );
    }
}

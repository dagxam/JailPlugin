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

public final class JailManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JailPlugin plugin;

    private final File dataFile;

    private final Map<UUID, Integer> prisonerTimes =
            new HashMap<>();

    private final Map<UUID, Location> prisonerCells =
            new HashMap<>();

    private final Map<String, Location> cells =
            new LinkedHashMap<>();

    private Location releaseLocation;

    private double cellRadius;

    private List<String> allowedCommands =
            new ArrayList<>();

    private boolean broadcastArrests;


    public JailManager(JailPlugin plugin) {

        this.plugin = plugin;

        this.dataFile = new File(
                plugin.getDataFolder(),
                "prisoners.yml"
        );

        reloadSettings();
    }


    public void reloadSettings() {

        plugin.reloadConfig();

        FileConfiguration config =
                plugin.getConfig();


        cells.clear();


        if (
                config.isConfigurationSection(
                        "cells"
                )
        ) {

            for (
                    String id :
                    config
                            .getConfigurationSection("cells")
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


        cellRadius =
                Math.max(
                        0.5,
                        config.getDouble(
                                "cell-radius",
                                3.0
                        )
                );


        broadcastArrests =
                config.getBoolean(
                        "broadcast-arrests",
                        true
                );


        allowedCommands =
                new ArrayList<>();

        for (
                String command :
                config.getStringList(
                        "allowed-commands"
                )
        ) {

            allowedCommands.add(
                    command.toLowerCase(
                            Locale.ROOT
                    )
            );
        }
    }


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
                Bukkit.getWorld(worldName);

        if (world == null) {

            plugin.getLogger().warning(
                    "Мир '" +
                            worldName +
                            "' не найден для " +
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
                        "prisoners." + uuidText;


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


                if (cellId != null) {

                    cell =
                            cells.get(
                                    cellId
                            );
                }


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
                        "Не удалось загрузить заключённого: "
                                + uuidText
                );
            }
        }
    }


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
                    "prisoners." + uuid;


            data.set(
                    path + ".time",
                    prisonerTimes.get(uuid)
            );


            String cellId =
                    findCellId(cell);


            if (cellId != null) {

                data.set(
                        path + ".cell",
                        cellId
                );

            } else {

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
            }
        }


        try {

            data.save(
                    dataFile
            );

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Не удалось сохранить prisoners.yml"
            );

            exception.printStackTrace();
        }
    }


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
                            && cell.distanceSquared(
                            location
                    ) < 0.01
            ) {

                return entry.getKey();
            }
        }

        return null;
    }


    public void jailPlayer(
            Player player,
            int seconds,
            String reason
    ) {

        if (cells.isEmpty()) {

            player.sendMessage(
                    colorize(
                            getMessage(
                                    "no-cells"
                            )
                    )
            );

            return;
        }


        UUID uuid =
                player.getUniqueId();


        int newTime =
                prisonerTimes.getOrDefault(
                        uuid,
                        0
                )
                        + Math.max(
                        1,
                        seconds
                );


        Location cell =
                getRandomCell();


        prisonerTimes.put(
                uuid,
                newTime
        );


        prisonerCells.put(
                uuid,
                cell
        );


        player.teleport(
                cell
        );


        player.sendMessage("");

        player.sendMessage(
                colorize(
                        getMessage(
                                "arrest-header"
                        )
                )
        );

        player.sendMessage(
                colorize(
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
                colorize(
                        getMessage(
                                "arrest-time"
                        )
                                .replace(
                                        "%time%",
                                        formatTimeWords(
                                                seconds
                                        )
                                )
                )
        );

        player.sendMessage("");


        if (broadcastArrests) {

            Bukkit.broadcastMessage(
                    colorize(
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
                    colorize(
                            getMessage(
                                    "release-chat"
                            )
                    )
            );

            player.sendMessage("");


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


            if (current == null) {

                continue;
            }


            int time =
                    current - 1;


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


            player.sendActionBar(

                    component(

                            getMessage(
                                    "actionbar-timer"
                            )

                                    .replace(
                                            "%mins%",
                                            String.valueOf(
                                                    time / 60
                                            )
                                    )

                                    .replace(
                                            "%secs%",
                                            String.valueOf(
                                                    time % 60
                                            )
                                    )
                    )
            );
        }


        for (
                UUID uuid :
                toRelease
        ) {

            releasePlayer(
                    uuid
            );
        }
    }


    public boolean isJailed(
            UUID uuid
    ) {

        return prisonerTimes.containsKey(
                uuid
        );
    }


    public int getTimeRemaining(
            UUID uuid
    ) {

        return prisonerTimes.getOrDefault(
                uuid,
                0
        );
    }


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


    public Map<UUID, Integer> getAllPrisoners() {

        return Collections.unmodifiableMap(
                prisonerTimes
        );
    }


    public double getCellRadius() {

        return cellRadius;
    }


    public List<String> getAllowedCommands() {

        return Collections.unmodifiableList(
                allowedCommands
        );
    }


    public Location getReleaseLocation() {

        return releaseLocation == null
                ? null
                : releaseLocation.clone();
    }


    public int getCellCount() {

        return cells.size();
    }


    public Set<String> getCellIds() {

        return Collections.unmodifiableSet(
                cells.keySet()
        );
    }


    public Location getRandomCell() {

        if (cells.isEmpty()) {

            return null;
        }


        List<Location> list =
                new ArrayList<>(
                        cells.values()
                );


        return list.get(
                new Random().nextInt(
                        list.size()
                )
        ).clone();
    }


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


    public boolean removeCell(
            String id
    ) {

        if (!cells.containsKey(id)) {

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


    public int getSentenceTime(
            String key
    ) {

        return plugin.getConfig().getInt(

                "sentences." + key,

                plugin.getConfig().getInt(
                        "sentences.default",
                        600
                )
        );
    }


    public String getMessage(
            String key
    ) {

        return plugin.getConfig().getString(
                "messages." + key,
                key
        );
    }


    public String formatTimeWords(
            int seconds
    ) {

        int mins =
                seconds / 60;

        int secs =
                seconds % 60;


        if (
                mins > 0
                        && secs == 0
        ) {

            return mins +
                    " " +
                    minuteWord(mins);
        }


        if (mins > 0) {

            return mins +
                    " " +
                    minuteWord(mins) +
                    " " +
                    secs +
                    " " +
                    secondWord(secs);
        }


        return secs +
                " " +
                secondWord(secs);
    }


    private String minuteWord(
            int n
    ) {

        int x =
                Math.abs(n) % 100;

        int last =
                x % 10;


        if (
                x >= 11
                        && x <= 19
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


    private String secondWord(
            int n
    ) {

        int x =
                Math.abs(n) % 100;

        int last =
                x % 10;


        if (
                x >= 11
                        && x <= 19
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


    public static Component component(
            String text
    ) {

        return LEGACY.deserialize(
                text == null
                        ? ""
                        : text
        );
    }
}

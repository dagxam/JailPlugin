package com.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Отдельный менеджер тюремного заключения сущностей.
 *
 * ВАЖНО:
 *
 * Игроки здесь НЕ хранятся.
 *
 * Существующая система игроков продолжает
 * работать через JailManager.
 *
 * Этот класс отвечает только за сущности,
 * отличные от Player.
 */
public final class EntityJailManager {


    /**
     * Главный класс плагина.
     */
    private final JailPlugin plugin;


    /**
     * Файл с заключёнными сущностями.
     */
    private final File dataFile;


    /**
     * Оставшееся время сущностей.
     *
     * UUID сущности -> секунды.
     */
    private final Map<UUID, Integer> entityTimes =
            new HashMap<>();


    /**
     * Камеры сущностей.
     *
     * UUID сущности -> Location камеры.
     */
    private final Map<UUID, Location> entityCells =
            new HashMap<>();


    /**
     * Создаёт менеджер сущностей.
     *
     * @param plugin главный класс плагина
     */
    public EntityJailManager(
            JailPlugin plugin
    ) {

        this.plugin = plugin;


        this.dataFile =
                new File(
                        plugin.getDataFolder(),
                        "entity-prisoners.yml"
                );
    }


    /**
     * Загружает заключённых сущностей
     * из entity-prisoners.yml.
     *
     * Если сущность больше не существует,
     * её запись будет удалена.
     */
    public void loadEntities() {

        entityTimes.clear();

        entityCells.clear();


        if (
                !dataFile.exists()
        ) {

            return;
        }


        FileConfiguration data =
                YamlConfiguration.loadConfiguration(
                        dataFile
                );


        if (
                !data.isConfigurationSection(
                        "entities"
                )
        ) {

            return;
        }


        List<UUID> missingEntities =
                new ArrayList<>();


        for (
                String uuidText :
                data
                        .getConfigurationSection(
                                "entities"
                        )
                        .getKeys(false)
        ) {

            UUID uuid;


            try {

                uuid =
                        UUID.fromString(
                                uuidText
                        );

            } catch (
                    IllegalArgumentException exception
            ) {

                plugin.getLogger().warning(
                        "Некорректный UUID сущности в entity-prisoners.yml: "
                                +
                                uuidText
                );

                continue;
            }


            String path =
                    "entities." +
                            uuidText;


            int time =
                    data.getInt(
                            path + ".time",
                            0
                    );


            if (
                    time <= 0
            ) {

                continue;
            }


            Location cell =
                    readLocation(
                            data,
                            path
                    );


            if (
                    cell == null
            ) {

                plugin.getLogger().warning(
                        "Не удалось восстановить камеру сущности "
                                +
                                uuidText
                );

                continue;
            }


            /*
             * Проверяем, существует ли сама сущность.
             */

            Entity entity =
                    findEntity(
                            uuid
                    );


            if (
                    entity == null
            ) {

                missingEntities.add(
                        uuid
                );

                continue;
            }


            /*
             * Игроки никогда не должны попадать
             * в этот менеджер.
             */

            if (
                    entity instanceof org.bukkit.entity.Player
            ) {

                missingEntities.add(
                        uuid
                );

                continue;
            }


            entityTimes.put(
                    uuid,
                    time
            );


            entityCells.put(
                    uuid,
                    cell
            );
        }


        /*
         * Удаляем старые записи.
         */

        for (
                UUID uuid :
                missingEntities
        ) {

            entityTimes.remove(
                    uuid
            );

            entityCells.remove(
                    uuid
            );
        }


        if (
                !missingEntities.isEmpty()
        ) {

            saveEntities();
        }
    }


    /**
     * Сохраняет заключённых сущностей.
     */
    public void saveEntities() {

        FileConfiguration data =
                new YamlConfiguration();


        for (
                Map.Entry<UUID, Integer> entry :
                entityTimes.entrySet()
        ) {

            UUID uuid =
                    entry.getKey();


            int time =
                    entry.getValue();


            Location cell =
                    entityCells.get(
                            uuid
                    );


            if (
                    time <= 0
                            ||
                    cell == null
                            ||
                    cell.getWorld() == null
            ) {

                continue;
            }


            String path =
                    "entities." +
                            uuid;


            data.set(
                    path + ".time",
                    time
            );


            data.set(
                    path + ".world",
                    cell
                            .getWorld()
                            .getName()
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


        try {

            data.save(
                    dataFile
            );

        } catch (
                IOException exception
        ) {

            plugin.getLogger().severe(
                    "Не удалось сохранить entity-prisoners.yml: "
                            +
                            exception.getMessage()
            );
        }
    }


    /**
     * Заключает сущность.
     *
     * @param entity сущность
     * @param seconds срок в секундах
     * @param cell камера
     * @return true при успешном заключении
     */
    public boolean jailEntity(
            Entity entity,
            int seconds,
            Location cell
    ) {

        if (
                entity == null
                        ||
                entity.isDead()
                        ||
                cell == null
                        ||
                cell.getWorld() == null
        ) {

            return false;
        }


        /*
         * Игроков через этот менеджер
         * заключать нельзя.
         */

        if (
                entity instanceof org.bukkit.entity.Player
        ) {

            return false;
        }


        UUID uuid =
                entity.getUniqueId();


        /*
         * Если сущность уже заключена,
         * добавляем новый срок к старому.
         */

        int newTime =
                entityTimes.getOrDefault(
                        uuid,
                        0
                )
                +
                Math.max(
                        1,
                        seconds
                );


        entityTimes.put(
                uuid,
                newTime
        );


        entityCells.put(
                uuid,
                cell.clone()
        );


        /*
         * Сразу отправляем сущность
         * в камеру.
         */

        entity.teleport(
                cell
        );


        /*
         * Сохраняем данные.
         */

        saveEntities();


        return true;
    }


    /**
     * Освобождает сущность.
     *
     * Если сущность ещё существует,
     * она возвращается из камеры
     * в точку освобождения.
     *
     * @param uuid UUID сущности
     */
    public void releaseEntity(
            UUID uuid
    ) {

        if (
                uuid == null
        ) {

            return;
        }


        entityTimes.remove(
                uuid
        );


        entityCells.remove(
                uuid
        );


        Entity entity =
                findEntity(
                        uuid
                );


        if (
                entity != null
                        &&
                !entity.isDead()
        ) {

            Location release =
                    plugin
                            .getJailManager()
                            .getReleaseLocation();


            if (
                    release != null
            ) {

                entity.teleport(
                        release
                );
            }
        }


        saveEntities();
    }


    /**
     * Уменьшает сроки заключённых сущностей.
     *
     * Вызывается раз в секунду.
     */
    public void tickTimers() {

        if (
                entityTimes.isEmpty()
        ) {

            return;
        }


        List<UUID> toRelease =
                new ArrayList<>();


        List<UUID> toRemove =
                new ArrayList<>();


        for (
                Map.Entry<UUID, Integer> entry :
                new ArrayList<>(
                        entityTimes.entrySet()
                )
        ) {

            UUID uuid =
                    entry.getKey();


            int time =
                    entry.getValue();


            Entity entity =
                    findEntity(
                            uuid
                    );


            /*
             * Сущность исчезла.
             */

            if (
                    entity == null
                            ||
                    entity.isDead()
            ) {

                toRemove.add(
                        uuid
                );

                continue;
            }


            /*
             * Игрок каким-то образом
             * оказался в entity manager.
             *
             * Удаляем такую запись.
             */

            if (
                    entity instanceof org.bukkit.entity.Player
            ) {

                toRemove.add(
                        uuid
                );

                continue;
            }


            time--;


            if (
                    time <= 0
            ) {

                toRelease.add(
                        uuid
                );

                continue;
            }


            entityTimes.put(
                    uuid,
                    time
            );


            /*
             * Проверяем, не вышла ли сущность
             * за пределы камеры.
             */

            Location cell =
                    entityCells.get(
                            uuid
                    );


            if (
                    cell != null
                            &&
                    entity.getWorld()
                            .equals(
                                    cell.getWorld()
                            )
            ) {

                double radius =
                        plugin
                                .getJailManager()
                                .getCellRadius();


                if (
                        entity
                                .getLocation()
                                .distanceSquared(
                                        cell
                                )
                                >
                                radius * radius
                ) {

                    entity.teleport(
                            cell
                    );
                }
            }
        }


        /*
         * Освобождаем тех,
         * у кого закончился срок.
         */

        for (
                UUID uuid :
                toRelease
        ) {

            releaseEntity(
                    uuid
            );
        }


        /*
         * Удаляем сущности,
         * которые исчезли.
         */

        for (
                UUID uuid :
                toRemove
        ) {

            entityTimes.remove(
                    uuid
            );

            entityCells.remove(
                    uuid
            );
        }


        saveEntities();
    }


    /**
     * Проверяет, находится ли сущность
     * в тюрьме.
     *
     * @param uuid UUID сущности
     * @return true, если заключена
     */
    public boolean isJailed(
            UUID uuid
    ) {

        return uuid != null
                &&
                entityTimes.containsKey(
                        uuid
                )
                &&
                entityTimes.get(
                        uuid
                ) > 0;
    }


    /**
     * Возвращает оставшееся время.
     *
     * @param uuid UUID сущности
     * @return секунды
     */
    public int getTimeRemaining(
            UUID uuid
    ) {

        return entityTimes.getOrDefault(
                uuid,
                0
        );
    }


    /**
     * Возвращает камеру сущности.
     *
     * @param uuid UUID сущности
     * @return Location камеры
     */
    public Location getCellLocation(
            UUID uuid
    ) {

        Location location =
                entityCells.get(
                        uuid
                );


        return location == null
                ? null
                : location.clone();
    }


    /**
     * Возвращает всех заключённых сущностей.
     *
     * @return карта UUID -> время
     */
    public Map<UUID, Integer> getAllEntities() {

        return Map.copyOf(
                entityTimes
        );
    }


    /**
     * Находит сущность по UUID
     * во всех загруженных мирах.
     *
     * @param uuid UUID сущности
     * @return сущность или null
     */
    private Entity findEntity(
            UUID uuid
    ) {

        if (
                uuid == null
        ) {

            return null;
        }


        for (
                World world :
                Bukkit.getWorlds()
        ) {

            Entity entity =
                    world.getEntity(
                            uuid
                    );


            if (
                    entity != null
            ) {

                return entity;
            }
        }


        return null;
    }


    /**
     * Читает координаты камеры
     * из файла.
     */
    private Location readLocation(
            FileConfiguration data,
            String path
    ) {

        String worldName =
                data.getString(
                        path + ".world"
                );


        if (
                worldName == null
        ) {

            return null;
        }


        World world =
                Bukkit.getWorld(
                        worldName
                );


        if (
                world == null
        ) {

            return null;
        }


        return new Location(

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

package com.jail;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Отдельный менеджер заключённых сущностей.
 *
 * Игроки здесь не хранятся.
 * Их продолжает обслуживать JailManager.
 *
 * Срок хранится как абсолютное время окончания,
 * поэтому перезапуск сервера не обнуляет и не замораживает срок.
 */
public final class EntityJailManager {

    private final JailPlugin plugin;
    private final File dataFile;

    private final Map<UUID, Long> expiresAt = new HashMap<>();
    private final Map<UUID, Location> entityCells = new HashMap<>();

    public EntityJailManager(JailPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(
                plugin.getDataFolder(),
                "entity-prisoners.yml"
        );
    }

    public void loadEntities() {
        expiresAt.clear();
        entityCells.clear();

        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration data =
                YamlConfiguration.loadConfiguration(dataFile);

        if (!data.isConfigurationSection("entities")) {
            return;
        }

        for (String uuidText :
                data.getConfigurationSection("entities").getKeys(false)) {

            UUID uuid;

            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Некорректный UUID сущности: " + uuidText
                );
                continue;
            }

            String path = "entities." + uuidText;

            long expiry = data.getLong(
                    path + ".expires-at",
                    0L
            );

            /*
             * Совместимость со старым форматом,
             * где хранилось количество секунд.
             */
            if (expiry <= 0L) {
                int oldSeconds = data.getInt(
                        path + ".time",
                        0
                );

                if (oldSeconds > 0) {
                    expiry = System.currentTimeMillis()
                            + oldSeconds * 1000L;
                }
            }

            Location cell = readLocation(data, path);

            if (expiry <= 0L || cell == null) {
                continue;
            }

            /*
             * Не удаляем запись, если сущность сейчас не загружена.
             * После загрузки мира/чанка UUID снова будет найден.
             */
            expiresAt.put(uuid, expiry);
            entityCells.put(uuid, cell);
        }

        saveEntities();
    }

    public void saveEntities() {
        FileConfiguration data = new YamlConfiguration();

        long now = System.currentTimeMillis();

        for (UUID uuid : new ArrayList<>(expiresAt.keySet())) {
            long expiry = expiresAt.getOrDefault(uuid, 0L);
            Location cell = entityCells.get(uuid);

            if (expiry <= now || cell == null || cell.getWorld() == null) {
                continue;
            }

            String path = "entities." + uuid;

            data.set(path + ".expires-at", expiry);
            data.set(path + ".world", cell.getWorld().getName());
            data.set(path + ".x", cell.getX());
            data.set(path + ".y", cell.getY());
            data.set(path + ".z", cell.getZ());
            data.set(path + ".yaw", cell.getYaw());
            data.set(path + ".pitch", cell.getPitch());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Не удалось сохранить entity-prisoners.yml: "
                            + exception.getMessage()
            );
        }
    }

    public boolean jailEntity(
            Entity entity,
            int seconds,
            Location cell
    ) {
        if (entity == null
                || entity.isDead()
                || !(entity instanceof LivingEntity)
                || entity instanceof org.bukkit.entity.Player
                || cell == null
                || cell.getWorld() == null
                || seconds <= 0) {
            return false;
        }

        UUID uuid = entity.getUniqueId();

        long now = System.currentTimeMillis();
        long oldExpiry = expiresAt.getOrDefault(uuid, now);
        long base = Math.max(now, oldExpiry);

        expiresAt.put(
                uuid,
                base + seconds * 1000L
        );

        entityCells.put(
                uuid,
                cell.clone()
        );

        /*
         * Не даём обычному мобу исчезнуть из-за
         * удаления на расстоянии от игроков.
         */
        entity.setPersistent(true);

        if (entity instanceof Mob mob) {
            mob.setRemoveWhenFarAway(false);
        }

        entity.teleport(cell);

        saveEntities();

        return true;
    }

    public void releaseEntity(UUID uuid) {
        if (uuid == null) {
            return;
        }

        expiresAt.remove(uuid);
        entityCells.remove(uuid);

        Entity entity = findEntity(uuid);

        if (entity != null && !entity.isDead()) {
            Location release =
                    plugin.getJailManager().getReleaseLocation();

            if (release != null) {
                entity.teleport(release);
            }
        }

        saveEntities();
    }

    public void tickTimers() {
        if (expiresAt.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<UUID> expiredAndLoaded = new ArrayList<>();

        for (UUID uuid : new ArrayList<>(expiresAt.keySet())) {
            long expiry = expiresAt.getOrDefault(uuid, 0L);

            Entity entity = findEntity(uuid);

            /*
             * Сущность может находиться в выгруженном чанке.
             * В таком случае запись сохраняем.
             */
            if (entity == null) {
                continue;
            }

            if (entity.isDead()
                    || entity instanceof org.bukkit.entity.Player) {
                expiresAt.remove(uuid);
                entityCells.remove(uuid);
                continue;
            }

            if (expiry <= now) {
                expiredAndLoaded.add(uuid);
                continue;
            }

            Location cell = entityCells.get(uuid);

            if (cell != null
                    && cell.getWorld() != null
                    && entity.getWorld().equals(cell.getWorld())) {

                double radius =
                        plugin.getJailManager().getCellRadius();

                if (entity.getLocation().distanceSquared(cell)
                        > radius * radius) {

                    entity.teleport(cell);
                }
            }
        }

        for (UUID uuid : expiredAndLoaded) {
            releaseEntity(uuid);
        }

        saveEntities();
    }

    public boolean isJailed(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        Long expiry = expiresAt.get(uuid);

        return expiry != null
                && expiry > System.currentTimeMillis();
    }

    public int getTimeRemaining(UUID uuid) {
        if (uuid == null) {
            return 0;
        }

        Long expiry = expiresAt.get(uuid);

        if (expiry == null) {
            return 0;
        }

        long remaining =
                Math.max(
                        0L,
                        expiry - System.currentTimeMillis()
                );

        return (int) Math.min(
                Integer.MAX_VALUE,
                (remaining + 999L) / 1000L
        );
    }

    public Location getCellLocation(UUID uuid) {
        Location location = entityCells.get(uuid);
        return location == null ? null : location.clone();
    }

    public Map<UUID, Integer> getAllEntities() {
        Map<UUID, Integer> result = new HashMap<>();

        for (UUID uuid : expiresAt.keySet()) {
            int time = getTimeRemaining(uuid);

            if (time > 0) {
                result.put(uuid, time);
            }
        }

        return Map.copyOf(result);
    }

    private Entity findEntity(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        Entity entity = Bukkit.getEntity(uuid);

        if (entity != null) {
            return entity;
        }

        for (World world : Bukkit.getWorlds()) {
            entity = world.getEntity(uuid);

            if (entity != null) {
                return entity;
            }
        }

        return null;
    }

    private Location readLocation(
            FileConfiguration data,
            String path
    ) {
        String worldName = data.getString(path + ".world");

        if (worldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            return null;
        }

        return new Location(
                world,
                data.getDouble(path + ".x"),
                data.getDouble(path + ".y"),
                data.getDouble(path + ".z"),
                (float) data.getDouble(path + ".yaw", 0.0),
                (float) data.getDouble(path + ".pitch", 0.0)
        );
    }
}

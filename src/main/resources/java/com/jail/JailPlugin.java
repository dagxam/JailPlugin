package com.jail;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JailPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private Location jailLocation;
    private List<String> jailedPlayers;

    @Override
    public void onEnable() {
        // Создаем конфиг, если его нет
        saveDefaultConfig();
        loadJailData();

        // Регистрируем события и команды
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("setjail").setExecutor(this);
        getCommand("jail").setExecutor(this);
        getCommand("unjail").setExecutor(this);

        getLogger().info("JailPlugin успешно запущен!");
    }

    @Override
    public void onDisable() {
        saveJailData();
        getLogger().info("JailPlugin выключен. Данные сохранены.");
    }

    // Загрузка данных из конфигурации
    private void loadJailData() {
        jailedPlayers = getConfig().getStringList("jailed_players");
        if (jailedPlayers == null) {
            jailedPlayers = new ArrayList<>();
        }

        if (getConfig().contains("jail_location.world")) {
            World world = Bukkit.getWorld(getConfig().getString("jail_location.world"));
            double x = getConfig().getDouble("jail_location.x");
            double y = getConfig().getDouble("jail_location.y");
            double z = getConfig().getDouble("jail_location.z");
            float yaw = (float) getConfig().getDouble("jail_location.yaw");
            float pitch = (float) getConfig().getDouble("jail_location.pitch");
            
            if (world != null) {
                jailLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    // Сохранение данных в конфигурацию
    private void saveJailData() {
        getConfig().set("jailed_players", jailedPlayers);
        
        if (jailLocation != null) {
            getConfig().set("jail_location.world", jailLocation.getWorld().getName());
            getConfig().set("jail_location.x", jailLocation.getX());
            getConfig().set("jail_location.y", jailLocation.getY());
            getConfig().set("jail_location.z", jailLocation.getZ());
            getConfig().set("jail_location.yaw", jailLocation.getYaw());
            getConfig().set("jail_location.pitch", jailLocation.getPitch());
        }
        
        saveConfig();
    }

    // Обработка команд
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Команда: /setjail (Установить точку тюрьмы)
        if (command.getName().equalsIgnoreCase("setjail")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Эту команду может использовать только игрок!");
                return true;
            }
            if (!sender.hasPermission("jail.admin")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для этого!");
                return true;
            }

            Player p = (Player) sender;
            jailLocation = p.getLocation();
            saveJailData();
            p.sendMessage(ChatColor.GREEN + "Точка тюрьмы успешно установлена!");
            return true;
        }

        // Команда: /jail <игрок> (Посадить в тюрьму)
        if (command.getName().equalsIgnoreCase("jail")) {
            if (!sender.hasPermission("jail.admin")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Использование: /jail <игрок>");
                return true;
            }
            if (jailLocation == null) {
                sender.sendMessage(ChatColor.RED + "Точка тюрьмы не установлена! Используйте /setjail");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден онлайн!");
                return true;
            }

            String targetUUID = target.getUniqueId().toString();
            if (!jailedPlayers.contains(targetUUID)) {
                jailedPlayers.add(targetUUID);
                target.teleport(jailLocation);
                saveJailData();
                target.sendMessage(ChatColor.RED + "Вы были отправлены в тюрьму!");
                sender.sendMessage(ChatColor.GREEN + "Игрок " + target.getName() + " отправлен в тюрьму.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Игрок уже в тюрьме!");
            }
            return true;
        }

        // Команда: /unjail <игрок> (Освободить)
        if (command.getName().equalsIgnoreCase("unjail")) {
            if (!sender.hasPermission("jail.admin")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Использование: /unjail <игрок>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            String targetUUID = (target != null) ? target.getUniqueId().toString() : null;

            // Если игрок оффлайн, пытаемся найти по имени и убрать (упрощенный вариант)
            boolean removed = false;
            if (targetUUID != null && jailedPlayers.contains(targetUUID)) {
                jailedPlayers.remove(targetUUID);
                target.teleport(target.getWorld().getSpawnLocation());
                target.sendMessage(ChatColor.GREEN + "Вы были освобождены из тюрьмы!");
                removed = true;
            }
            
            if (removed) {
                saveJailData();
                sender.sendMessage(ChatColor.GREEN + "Игрок освобожден.");
            } else {
                sender.sendMessage(ChatColor.RED + "Этот игрок не в тюрьме или оффлайн.");
            }
            return true;
        }

        return false;
    }

    // --- БЛОК СОБЫТИЙ (Ограничения для заключенных) ---

    // Проверка, в тюрьме ли игрок
    private boolean isJailed(Player p) {
        return jailedPlayers.contains(p.getUniqueId().toString());
    }

    // Запрещаем ломать блоки
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (isJailed(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Вам запрещено ломать блоки в тюрьме!");
        }
    }

    // Запрещаем ставить блоки
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isJailed(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Вам запрещено ставить блоки в тюрьме!");
        }
    }

    // Запрещаем бить других игроков
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (isJailed(p)) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "Вам запрещено драться в тюрьме!");
            }
        }
    }

    // Запрещаем писать команды (кроме /msg или других нужных)
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (isJailed(e.getPlayer())) {
            // Разрешаем только команду /msg, остальные блокируем
            if (!e.getMessage().toLowerCase().startsWith("/msg ")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "В тюрьме нельзя использовать команды!");
            }
        }
    }
}

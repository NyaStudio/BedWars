package cn.nekopixel.bedwars.setup;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.commands.HelpCommand;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Map implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Set<String> validTeams = Set.of("red", "blue", "green", "yellow", "aqua", "white", "pink", "gray");
    private FileConfiguration mapConfig;
    private File mapFile;
    
    private final java.util.Map<Player, Location> pos1Map = new HashMap<>();
    private final java.util.Map<Player, Location> pos2Map = new HashMap<>();

    public Map(Main plugin) {
        this.plugin = plugin;
        loadMapConfig();
    }

    private void loadMapConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        mapFile = new File(plugin.getDataFolder(), "map.yml");
        if (!mapFile.exists()) {
            plugin.saveResource("map.yml", false);
        }
        mapConfig = YamlConfiguration.loadConfiguration(mapFile);
    }

    private void saveMapConfig() {
        try {
            mapConfig.save(mapFile);
            plugin.getLogger().info("地图配置文件已保存");
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 map.yml 文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadMapConfig() {
        try {
            loadMapConfig();
            validateConfig();
            plugin.getLogger().info("地图配置文件已重新加载");
        } catch (Exception e) {
            plugin.getLogger().severe("重新加载地图配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateConfig() {
        if (!mapConfig.contains("spawnpoints")) {
            plugin.getLogger().warning("配置文件中缺少 spawnpoints 配置项");
        }
        if (!mapConfig.contains("npcs")) {
            plugin.getLogger().warning("配置文件中缺少 npcs 配置项");
        }
        if (!mapConfig.contains("spawners")) {
            plugin.getLogger().warning("配置文件中缺少 spawners 配置项");
        }
        if (!mapConfig.contains("join")) {
            plugin.getLogger().warning("配置文件中缺少 join 配置项");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "这个命令只能玩家执行。");
            return true;
        }

        if (args.length < 1) {
            HelpCommand.sendMainHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> {
                HelpCommand.sendMainHelp(sender);
            }

            case "setjoin" -> {
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw setjoin [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 1);
                mapConfig.set("join", loc.serialize());
                saveMapConfig();
                sender.sendMessage(ChatColor.GREEN + "已设置加入时位置");
            }

            case "setbed" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw setbed <team> [x] [y] [z]");
                    return true;
                }
                String team = args[1].toLowerCase();
                if (!validTeams.contains(team)) {
                    sender.sendMessage(ChatColor.RED + "无效的队伍颜色！可用颜色: " + String.join(", ", validTeams));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                Block block = loc.getBlock();
                if (block.getType() != Material.RED_BED && block.getType() != Material.WHITE_BED) {
                    sender.sendMessage(ChatColor.RED + "错误：指定位置必须是床方块");
                    return true;
                }
                mapConfig.set("beds." + team, loc.serialize());
                saveMapConfig();
                sender.sendMessage(ChatColor.GREEN + "已设置 " + ChatColor.YELLOW + team + ChatColor.GREEN + " 队的床位置");
            }

            case "setspawn" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw setspawn <team> [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                String team = args[1].toLowerCase();
                if (!validTeams.contains(team)) {
                    sender.sendMessage(ChatColor.RED + "无效的队伍颜色！可用颜色: " + String.join(", ", validTeams));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                mapConfig.set("spawnpoints." + team, loc.serialize());
                saveMapConfig();
                sender.sendMessage(ChatColor.GREEN + "已设置 " + ChatColor.YELLOW + team + ChatColor.GREEN + " 队出生点");
            }

            case "setnpc" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw setnpc <shop|upgrade> [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                String type = args[1].toLowerCase();
                Location loc = getLocationFromArgs(p, args, 2);

                if (type.equals("shop")) {
                    List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs.shop");
                    list.add(loc.serialize());
                    mapConfig.set("npcs.shop", list);
                    saveMapConfig();
                    sender.sendMessage(ChatColor.GREEN + "已添加一个商店NPC位置");
                } else if (type.equals("upgrade")) {
                    List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs.upgrade");
                    list.add(loc.serialize());
                    mapConfig.set("npcs.upgrade", list);
                    saveMapConfig();
                    sender.sendMessage(ChatColor.GREEN + "已添加一个升级NPC位置");
                } else {
                    sender.sendMessage(ChatColor.RED + "无效的NPC类型！可用类型: shop, upgrade");
                }
            }

            case "setspawner" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw setspawner <iron|gold|diamond|emerald> [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                String type = args[1].toLowerCase();
                if (!type.equals("iron") && !type.equals("gold") && !type.equals("diamond") && !type.equals("emerald")) {
                    sender.sendMessage(ChatColor.RED + "无效的资源类型！可用类型: iron, gold, diamond, emerald");
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                List<java.util.Map<?, ?>> list = mapConfig.getMapList("spawners." + type);
                list.add(loc.serialize());
                mapConfig.set("spawners." + type, list);
                saveMapConfig();
                sender.sendMessage(ChatColor.GREEN + "已添加一个 " + ChatColor.YELLOW + type + ChatColor.GREEN + " 生成点");
            }

            case "pos1" -> {
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw pos1 [x] [y] [z]");
                    return true;
                }
                Location pos1 = getLocationFromArgs(p, args, 1);
                pos1Map.put(p, pos1);
                sender.sendMessage(ChatColor.GREEN + "已设置第一个坐标点: " + 
                    String.format("(%.1f, %.1f, %.1f)", pos1.getX(), pos1.getY(), pos1.getZ()));
            }

            case "pos2" -> {
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw pos2 [x] [y] [z]");
                    return true;
                }
                Location pos2 = getLocationFromArgs(p, args, 1);
                pos2Map.put(p, pos2);
                sender.sendMessage(ChatColor.GREEN + "已设置第二个坐标点: " + 
                    String.format("(%.1f, %.1f, %.1f)", pos2.getX(), pos2.getY(), pos2.getZ()));
            }

            case "addprotect" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw addprotect <name>");
                    return true;
                }
                handleAddProtectArea(p, args);
            }

            case "removeprotect" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw removeprotect <name>");
                    return true;
                }
                handleRemoveProtectArea(p, args[1]);
            }

            case "listprotect" -> {
                handleListProtectAreas(p);
            }

            case "save" -> {
                saveMapConfig();
                sender.sendMessage(ChatColor.GREEN + "配置文件已保存！");
            }
        }

        return true;
    }

    private void handleAddProtectArea(Player player, String[] args) {
        Location pos1 = pos1Map.get(player);
        Location pos2 = pos2Map.get(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "请先设置两个坐标点 (pos1 和 pos2)");
            return;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(ChatColor.RED + "两个坐标点必须在同一个世界");
            return;
        }

        String areaName = args[1];

        try {
            var areasSection = mapConfig.getConfigurationSection("protection");
            if (areasSection == null) {
                areasSection = mapConfig.createSection("protection");
            }

            var areaSection = areasSection.createSection(areaName);
            areaSection.set("world", pos1.getWorld().getName());
            
            var pos1Section = areaSection.createSection("pos1");
            pos1Section.set("x", Math.floor(pos1.getX()));
            pos1Section.set("y", Math.floor(pos1.getY()));
            pos1Section.set("z", Math.floor(pos1.getZ()));
            
            var pos2Section = areaSection.createSection("pos2");
            pos2Section.set("x", Math.floor(pos2.getX()));
            pos2Section.set("y", Math.floor(pos2.getY()));
            pos2Section.set("z", Math.floor(pos2.getZ()));
            
            saveMapConfig();
            
            if (Plugin.getInstance().getMapManager() != null) {
                Plugin.getInstance().getMapManager().loadProtectedAreas();
            }
            
            player.sendMessage(ChatColor.GREEN + "成功添加保护区域: " + areaName);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "添加保护区域失败: " + e.getMessage());
        }
    }

    private void handleRemoveProtectArea(Player player, String areaName) {
        try {
            var areasSection = mapConfig.getConfigurationSection("protection");
            if (areasSection != null && areasSection.contains(areaName)) {
                areasSection.set(areaName, null);
                saveMapConfig();
                
                if (Plugin.getInstance().getMapManager() != null) {
                    Plugin.getInstance().getMapManager().loadProtectedAreas();
                }
                
                player.sendMessage(ChatColor.GREEN + "成功移除保护区域: " + areaName);
            } else {
                player.sendMessage(ChatColor.RED + "保护区域不存在");
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "移除保护区域失败: " + e.getMessage());
        }
    }

    private void handleListProtectAreas(Player player) {
        var areasSection = mapConfig.getConfigurationSection("protection");
        if (areasSection == null || areasSection.getKeys(false).isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "当前没有配置任何保护区域");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== 保护区域列表 ===");
        for (String areaName : areasSection.getKeys(false)) {
            var areaSection = areasSection.getConfigurationSection(areaName);
            if (areaSection != null) {
                var pos1Section = areaSection.getConfigurationSection("pos1");
                var pos2Section = areaSection.getConfigurationSection("pos2");
                
                if (pos1Section != null && pos2Section != null) {
                    double x1 = pos1Section.getDouble("x");
                    double y1 = pos1Section.getDouble("y");
                    double z1 = pos1Section.getDouble("z");
                    double x2 = pos2Section.getDouble("x");
                    double y2 = pos2Section.getDouble("y");
                    double z2 = pos2Section.getDouble("z");
                    
                    player.sendMessage(String.format("§e%s: §7(%.1f, %.1f, %.1f) 到 (%.1f, %.1f, %.1f)",
                        areaName,
                        Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                        Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
                    ));
                }
            }
        }
    }

    public FileConfiguration getMapConfig() {
        return mapConfig;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("help");
            completions.add("setjoin");
            completions.add("setbed");
            completions.add("setspawn");
            completions.add("setnpc");
            completions.add("setspawner");
            completions.add("pos1");
            completions.add("pos2");
            completions.add("addprotect");
            completions.add("removeprotect");
            completions.add("listprotect");
            completions.add("save");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setbed", "setspawn" -> completions.addAll(validTeams);
                case "setnpc" -> {
                    completions.add("shop");
                    completions.add("upgrade");
                }
                case "setspawner" -> {
                    completions.add("iron");
                    completions.add("gold");
                    completions.add("diamond");
                    completions.add("emerald");
                }
                case "addprotect" -> {
                }
                case "removeprotect" -> {
                    var areasSection = mapConfig.getConfigurationSection("protection");
                    if (areasSection != null) {
                        completions.addAll(areasSection.getKeys(false));
                    }
                }
            }
        }
        
        return completions;
    }

    private Location getLocationFromArgs(Player player, String[] args, int startIndex) {
        if (args.length <= startIndex) {
            Location loc = player.getLocation();
            return new Location(loc.getWorld(),
                Math.floor(loc.getX()),
                Math.floor(loc.getY()),
                Math.floor(loc.getZ()),
                loc.getYaw(),
                loc.getPitch()
            );
        }

        try {
            double x = Math.floor(Double.parseDouble(args[startIndex]));
            double y = Math.floor(Double.parseDouble(args[startIndex + 1]));
            double z = Math.floor(Double.parseDouble(args[startIndex + 2]));
            float yaw = args.length > startIndex + 3 ? Float.parseFloat(args[startIndex + 3]) : player.getLocation().getYaw();
            float pitch = args.length > startIndex + 4 ? Float.parseFloat(args[startIndex + 4]) : player.getLocation().getPitch();

            return new Location(player.getWorld(), x, y, z, yaw, pitch);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Location loc = player.getLocation();
            return new Location(loc.getWorld(),
                Math.floor(loc.getX()),
                Math.floor(loc.getY()),
                Math.floor(loc.getZ()),
                loc.getYaw(),
                loc.getPitch()
            );
        }
    }
}

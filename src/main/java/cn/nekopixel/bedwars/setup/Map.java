package cn.nekopixel.bedwars.setup;

import cn.nekopixel.bedwars.Main;
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

    public void reloadMapConfig() {
        loadMapConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "这个命令只能玩家执行。");
            return true;
        }

        if (args.length < 1) {
            return false;
        }

        switch (args[0].toLowerCase()) {
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

            case "save" -> {
                saveMapConfig();
                sender.sendMessage(ChatColor.GREEN + "配置文件已保存！");
            }
        }

        return true;
    }

    private void saveMapConfig() {
        try {
            mapConfig.save(mapFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 map.yml 文件: " + e.getMessage());
        }
    }

    public FileConfiguration getMapConfig() {
        return mapConfig;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("setjoin");
            completions.add("setbed");
            completions.add("setspawn");
            completions.add("setnpc");
            completions.add("setspawner");
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
            }
        }
        
        return completions;
    }

    private Location getLocationFromArgs(Player player, String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return player.getLocation();
        }

        try {
            double x = Double.parseDouble(args[startIndex]);
            double y = Double.parseDouble(args[startIndex + 1]);
            double z = Double.parseDouble(args[startIndex + 2]);
            float yaw = args.length > startIndex + 3 ? Float.parseFloat(args[startIndex + 3]) : player.getLocation().getYaw();
            float pitch = args.length > startIndex + 4 ? Float.parseFloat(args[startIndex + 4]) : player.getLocation().getPitch();

            return new Location(player.getWorld(), x, y, z, yaw, pitch);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return player.getLocation();
        }
    }
}

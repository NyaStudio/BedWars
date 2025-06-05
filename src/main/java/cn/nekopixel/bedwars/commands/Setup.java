package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class Setup implements CommandExecutor {
    private final Main plugin;
    private final Set<String> validTeams = Set.of("red", "blue", "green", "yellow", "aqua", "white", "pink", "gray");

    public Setup(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("这个命令只能玩家执行。");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setspawn" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /bw setspawn <team> [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                String team = args[1].toLowerCase();
                if (!validTeams.contains(team)) {
                    sender.sendMessage("§c无效的队伍颜色！可用颜色: " + String.join(", ", validTeams));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                plugin.getConfig().set("spawnpoints." + team, loc.serialize());
                sender.sendMessage("§a已设置 §e" + team + " §a队出生点！");
            }

            case "setnpc" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /bw setnpc <shop|upgrade> [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                String type = args[1].toLowerCase();
                Location loc = getLocationFromArgs(p, args, 2);
                plugin.getConfig().set("npcs." + type, loc.serialize());
                sender.sendMessage("§a已设置 §e" + type + " §aNPC 位置！");
            }

            case "setspawner" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /bw setspawner <type> [x] [y] [z] [yaw] [pitch]");
                    return true;
                }
                String type = args[1].toLowerCase();
                Location loc = getLocationFromArgs(p, args, 2);
                List<Map<?, ?>> list = plugin.getConfig().getMapList("spawners." + type);
                list.add(loc.serialize());
                plugin.getConfig().set("spawners." + type, list);
                sender.sendMessage("§a已添加一个 §e" + type + " §a资源生成点！");
            }

            case "saveconfig" -> {
                plugin.saveConfig();
                sender.sendMessage("§a配置文件已保存！");
            }

            default -> {
                sender.sendMessage("§c未知命令参数！");
                sendHelp(sender);
            }
        }

        return true;
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("""
                §6[Bedwars 地图配置]
                §e/bw setspawn <team> [x] [y] [z] [yaw] [pitch] §7- 设置队伍出生点
                §e/bw setnpc <shop|upgrade> [x] [y] [z] [yaw] [pitch] §7- 设置商店/升级NPC
                §e/bw setspawner <type> [x] [y] [z] [yaw] [pitch] §7- 设置资源生成点
                §e/bw saveconfig §7- 保存配置文件
                """);
    }
}

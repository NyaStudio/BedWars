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
                    sender.sendMessage("§c用法: /bw setspawn <team>");
                    return true;
                }
                String team = args[1].toLowerCase();
                Location loc = p.getLocation();
                plugin.getConfig().set("spawnpoints." + team, loc.serialize());
                sender.sendMessage("§a已设置 §e" + team + " §a队出生点！");
            }

            case "setnpc" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /bw setnpc <shop|upgrade>");
                    return true;
                }
                String type = args[1].toLowerCase();
                Location loc = p.getLocation();
                plugin.getConfig().set("npcs." + type, loc.serialize());
                sender.sendMessage("§a已设置 §e" + type + " §aNPC 位置！");
            }

            case "setspawner" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /bw setspawner <iron|gold|diamond|emerald>");
                    return true;
                }
                String type = args[1].toLowerCase();
                Location loc = p.getLocation();
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("""
                §6[Bedwars 地图配置]
                §e/bw setspawn <team> §7- 设置队伍出生点
                §e/bw setnpc <shop|upgrade> §7- 设置商店/升级NPC
                §e/bw setspawner <type> §7- 设置资源生成点
                §e/bw saveconfig §7- 保存配置文件
                """);
    }
}

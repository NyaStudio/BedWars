package cn.nekopixel.bedwars.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand {
    public static void sendMainHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Bedwars 命令帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/bw switch <status> " + ChatColor.GRAY + "- 切换游戏状态");
        sender.sendMessage(ChatColor.YELLOW + "/bw reload " + ChatColor.GRAY + "- 重新加载配置文件");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== 地图配置命令 ===");
        sender.sendMessage(ChatColor.YELLOW + "/bw setjoin [x] [y] [z] [yaw] [pitch] " + ChatColor.GRAY + "- 设置加入时位置");
        sender.sendMessage(ChatColor.YELLOW + "/bw setbed <team> [x] [y] [z] " + ChatColor.GRAY + "- 设置队伍床位置");
        sender.sendMessage(ChatColor.YELLOW + "/bw setspawn <team> [x] [y] [z] [yaw] [pitch] " + ChatColor.GRAY + "- 设置队伍出生点");
        sender.sendMessage(ChatColor.YELLOW + "/bw setnpc <shop|upgrade> [x] [y] [z] [yaw] [pitch] " + ChatColor.GRAY + "- 设置商店/升级NPC");
        sender.sendMessage(ChatColor.YELLOW + "/bw setspawner <iron|gold|diamond|emerald> [x] [y] [z] [yaw] [pitch] " + ChatColor.GRAY + "- 设置资源生成点");
        sender.sendMessage(ChatColor.YELLOW + "/bw save " + ChatColor.GRAY + "- 保存配置文件");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "=== 保护区域命令 ===");
        sender.sendMessage(ChatColor.YELLOW + "/bw pos1 [x] [y] [z]" + ChatColor.WHITE + " - 设置第一个坐标点");
        sender.sendMessage(ChatColor.YELLOW + "/bw pos2 [x] [y] [z]" + ChatColor.WHITE + " - 设置第二个坐标点");
        sender.sendMessage(ChatColor.YELLOW + "/bw addprotect <name>" + ChatColor.WHITE + " - 添加保护区域");
        sender.sendMessage(ChatColor.YELLOW + "/bw removeprotect <name>" + ChatColor.WHITE + " - 移除保护区域");
        sender.sendMessage(ChatColor.YELLOW + "/bw listprotect" + ChatColor.WHITE + " - 列出所有保护区域");
    }
}
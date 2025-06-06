package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SwitchStatus {
    public static boolean onCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /bw switch <status>");
            sender.sendMessage(ChatColor.YELLOW + "可用状态: " + GameStatus.getNamesAsString());
            return true;
        }

        try {
            GameStatus newStatus = GameStatus.valueOf(args[1].toUpperCase());
            GameManager.getInstance().setStatus(newStatus);
            sender.sendMessage(ChatColor.GREEN + "游戏状态已切换为: " + ChatColor.YELLOW + newStatus.name());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "无效的游戏状态！");
            sender.sendMessage(ChatColor.YELLOW + "可用状态: " + GameStatus.getNamesAsString());
        }
        return true;
    }

    public static List<String> onTabComplete(String[] args) {
        if (args.length == 2) {
            return GameStatus.getNames();
        }
        return null;
    }
} 
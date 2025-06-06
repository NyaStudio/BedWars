package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameStatus implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;

    public GameStatus() {
        this.gameManager = GameManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有使用此命令的权限");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("switch")) {
            sender.sendMessage(ChatColor.RED + "用法: /bw switch <status>");
            return true;
        }

        try {
            cn.nekopixel.bedwars.game.GameStatus newStatus = cn.nekopixel.bedwars.game.GameStatus.valueOf(args[1].toUpperCase());
            gameManager.setStatus(newStatus);
            sender.sendMessage(ChatColor.GREEN + "成功将状态更改为: " + newStatus.name());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "无效的状态！可用状态: " +
                    Arrays.stream(cn.nekopixel.bedwars.game.GameStatus.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("switch");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("switch")) {
            completions.addAll(Arrays.stream(cn.nekopixel.bedwars.game.GameStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.toList()));
        }
        
        return completions;
    }
} 
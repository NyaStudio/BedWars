package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShoutCommand implements CommandExecutor {
    private final Main plugin;

    public ShoutCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "用法: " + ChatColor.YELLOW + "/shout <msg>");
            return true;
        }

        int gameMode = plugin.getConfig().getInt("game.mode", 2);
        if (gameMode == 1) {
            player.sendMessage(ChatColor.RED + "在单挑模式下无法使用此命令！");
            return true;
        }

        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            player.sendMessage(ChatColor.YELLOW + "当前无法使用此命令");
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String message = messageBuilder.toString().trim();

        String formattedMessage = Plugin.getInstance().getChatManager().formatShoutMessage(player, message);
        Bukkit.broadcastMessage(formattedMessage);

        return true;
    }
} 
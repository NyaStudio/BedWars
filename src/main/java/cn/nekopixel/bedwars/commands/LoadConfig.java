package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Loader;
import cn.nekopixel.bedwars.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoadConfig implements CommandExecutor {
    private final Main plugin;

    public LoadConfig(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有使用此命令的权限");
            return true;
        }

        try {
            Loader.reloadAll(plugin);
            sender.sendMessage(ChatColor.GREEN + "所有配置文件已重新加载");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "配置文件加载失败: " + e.getMessage());
        }

        return true;
    }
}

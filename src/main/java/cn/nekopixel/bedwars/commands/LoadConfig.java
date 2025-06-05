package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
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
            sender.sendMessage("§c你没有使用此命令的权限");
            return true;
        }

        try {
            plugin.reloadConfig();
            sender.sendMessage("§a配置文件已重新加载");
        } catch (Exception e) {
            sender.sendMessage("§c配置文件加载失败: " + e.getMessage());
        }

        return true;
    }
}

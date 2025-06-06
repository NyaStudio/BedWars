package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Map setup;
    private final LoadConfig loadConfig;
    private final Set<String> validCommands = Set.of("help", "reload", "switch", "setjoin", "setbed", "setspawn", "setnpc", "setspawner", "save");

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        this.setup = new Map(plugin);
        this.loadConfig = new LoadConfig(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            Help.sendMainHelp(sender);
            return true;
        }

        String command = args[0].toLowerCase();
        if (!validCommands.contains(command)) {
            sender.sendMessage(ChatColor.RED + "未知命令！使用 " + ChatColor.YELLOW + "/bw help " + ChatColor.RED + "查看帮助。");
            return true;
        }

        switch (command) {
            case "reload":
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                return loadConfig.onCommand(sender, cmd, label, newArgs);
            case "switch":
                return setup.onCommand(sender, cmd, label, args);
            default:
                return setup.onCommand(sender, cmd, label, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(validCommands);
        } else if (args.length > 1) {
            if (args[0].equalsIgnoreCase("switch")) {
                return setup.onTabComplete(sender, command, alias, args);
            }
        }
        
        return completions;
    }
} 
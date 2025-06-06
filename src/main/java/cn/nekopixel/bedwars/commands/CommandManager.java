package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Map setup;
    private final LoadConfig loadConfig;
    private final GameStatus gameStatusCommand;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        this.setup = new Map(plugin);
        this.loadConfig = new LoadConfig(plugin);
        this.gameStatusCommand = new GameStatus();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§a=== BedWars 命令帮助 ===");
            sender.sendMessage("§f/bw switch <status> §7- 切换游戏状态");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                return loadConfig.onCommand(sender, cmd, label, newArgs);
            case "switch":
                return gameStatusCommand.onCommand(sender, cmd, label, args);
            default:
                return setup.onCommand(sender, cmd, label, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reload");
            completions.add("switch");
        } else if (args.length > 1) {
            if (args[0].equalsIgnoreCase("switch")) {
                return gameStatusCommand.onTabComplete(sender, command, alias, args);
            }
        }
        
        return completions;
    }
} 
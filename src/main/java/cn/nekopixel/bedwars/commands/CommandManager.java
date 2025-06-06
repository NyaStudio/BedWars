package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandManager implements CommandExecutor {
    private final Main plugin;
    private final Map setup;
    private final LoadConfig loadConfig;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        this.setup = new Map(plugin);
        this.loadConfig = new LoadConfig(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            setup.sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                return loadConfig.onCommand(sender, cmd, label, newArgs);
            }
            default -> {
                return setup.onCommand(sender, cmd, label, args);
            }
        }
    }
} 
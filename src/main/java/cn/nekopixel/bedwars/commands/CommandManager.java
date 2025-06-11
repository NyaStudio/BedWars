package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Map setup;
    private final LoadConfig loadConfig;
    private final Set<String> validCommands = Set.of("help", "reload", "switch", "setjoin", "setbed", "setspawn", "setnpc", "setspawner", "save", "upgrade");

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        this.setup = new Map(plugin);
        this.loadConfig = new LoadConfig(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            HelpCommand.sendMainHelp(sender);
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
                return SwitchStatus.onCommand(sender, args);
            case "upgrade":
                if (!sender.hasPermission("bedwars.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有使用此命令的权限");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法: /bw upgrade <diamond|emerald>");
                    return true;
                }
                String type = args[1].toLowerCase();
                if (!type.equals("diamond") && !type.equals("emerald")) {
                    sender.sendMessage(ChatColor.RED + "无效的资源类型！可用类型: diamond, emerald");
                    return true;
                }
                if (type.equals("diamond")) {
                    plugin.getGameManager().getSpawnerManager().getDiamondSpawner().upgrade();
                    sender.sendMessage(ChatColor.GREEN + "钻石生成点已升级");
                } else {
                    plugin.getGameManager().getSpawnerManager().getEmeraldSpawner().upgrade();
                    sender.sendMessage(ChatColor.GREEN + "绿宝石生成点已升级");
                }
                return true;
            default:
                return setup.onCommand(sender, cmd, label, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(validCommands);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("switch")) {
                completions.addAll(GameStatus.getNames());
            } else if (args[0].equalsIgnoreCase("upgrade")) {
                completions.add("diamond");
                completions.add("emerald");
            }
        } else if (args.length > 1) {
            if (args[0].equalsIgnoreCase("switch")) {
                return SwitchStatus.onTabComplete(args);
            }
        }
        
        return completions;
    }
} 
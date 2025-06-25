package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionCommand {
    
    private static final Properties gitProperties = new Properties();
    
    static {
        try (InputStream is = VersionCommand.class.getClassLoader().getResourceAsStream("git.properties")) {
            if (is != null) {
                gitProperties.load(is);
            }
        } catch (IOException e) {}
    }
    
    public static void showVersion(CommandSender sender, Main plugin) {
        String version = plugin.getDescription().getVersion();
        String gitHash = getGitHash();
        String gitBranch = getGitBranch();
        
        sender.sendMessage(ChatColor.GOLD + "=== BedWars @ " + ChatColor.translateAlternateColorCodes('&', "&cN&6e&ek&ao&bP&9i&5x&de&cl") + " " + ChatColor.GOLD + "===");
        sender.sendMessage(ChatColor.WHITE + "v" + version + ChatColor.GREEN + " By " + ChatColor.AQUA + String.join(", ", plugin.getDescription().getAuthors()));
        if (gitHash != null && !gitHash.isEmpty() && !gitHash.contains("${")) {
            sender.sendMessage("Branch: " +
                    (gitBranch != null && !gitBranch.isEmpty() && !gitBranch.contains("${")
                            ? ChatColor.WHITE + gitBranch + ChatColor.GRAY + " (" + gitHash + ")"
                            : ChatColor.WHITE + gitHash
                    ));
        }
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + ChatColor.translateAlternateColorCodes('&', "&cN&6e&ek&ao&bP&9i&5x&de&cl") + " 社区动力！");
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + "https://www.nekopixel.cn/");
        sender.sendMessage(ChatColor.GOLD + "==========================");
    }
    
    private static String getGitHash() {
        String hash = gitProperties.getProperty("git.commit.id.abbrev");
        return (hash != null && !hash.contains("${")) ? hash : null;
    }
    
    private static String getGitBranch() {
        String branch = gitProperties.getProperty("git.branch");
        return (branch != null && !branch.contains("${")) ? branch : null;
    }
} 
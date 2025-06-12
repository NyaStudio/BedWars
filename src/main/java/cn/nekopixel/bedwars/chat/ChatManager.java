package cn.nekopixel.bedwars.chat;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ChatManager {
    private final Main plugin;
    private final TeamManager teamManager;
    private final Map<String, String> teamColors;
    private final Map<String, String> teamNames;
    private String chatFormat;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getGameManager().getTeamManager();
        this.teamColors = new HashMap<>();
        this.teamNames = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "chatting.yml");
        if (!configFile.exists()) {
            plugin.saveResource("chatting.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        chatFormat = ChatColor.translateAlternateColorCodes('&', config.getString("chat.format", "[%team_color%%team_name%&r] &7%player%&8: &f%message%"));

        if (config.contains("chat.team_colors")) {
            for (String key : config.getConfigurationSection("chat.team_colors").getKeys(false)) {
                String color = config.getString("chat.team_colors." + key);
                teamColors.put(key.toLowerCase(), ChatColor.translateAlternateColorCodes('&', color));
            }
        }

        if (config.contains("chat.team_names")) {
            for (String key : config.getConfigurationSection("chat.team_names").getKeys(false)) {
                String name = config.getString("chat.team_names." + key);
                teamNames.put(key.toLowerCase(), name);
            }
        }
    }

    public String formatMessage(Player player, String message) {
        String team = teamManager.getPlayerTeam(player);
        if (team == null) team = "spectator";

        String teamColor = teamColors.getOrDefault(team.toLowerCase(), "&7");
        String teamName = teamNames.getOrDefault(team.toLowerCase(), "未知队伍");

        return chatFormat
                .replace("%team_color%", teamColor)
                .replace("%team_name%", teamName)
                .replace("%player%", player.getName())
                .replace("%message%", message);
    }

    public void reloadConfig() {
        teamColors.clear();
        teamNames.clear();
        loadConfig();
    }
} 
package cn.nekopixel.bedwars.tab;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TabListManager {
    private final Main plugin;
    private final TeamManager teamManager;
    private final Map<String, String> teamColors;
    private final Map<String, String> teamNames;
    private String tabFormat;
    private int updateInterval;
    private BukkitRunnable updateTask;

    public TabListManager(Main plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getGameManager().getTeamManager();
        this.teamColors = new HashMap<>();
        this.teamNames = new HashMap<>();
        loadConfig();
        startUpdateTask();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "chatting.yml");
        if (!configFile.exists()) {
            plugin.saveResource("chatting.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        tabFormat = ChatColor.translateAlternateColorCodes('&', config.getString("tablist.format", "%team_color%%team_name%&r &7%player%"));
        updateInterval = config.getInt("tablist.update_interval", 20);

        if (config.contains("tablist.team_colors")) {
            for (String key : config.getConfigurationSection("tablist.team_colors").getKeys(false)) {
                String color = config.getString("tablist.team_colors." + key);
                teamColors.put(key.toLowerCase(), ChatColor.translateAlternateColorCodes('&', color));
            }
        }

        if (config.contains("tablist.team_names")) {
            for (String key : config.getConfigurationSection("tablist.team_names").getKeys(false)) {
                String name = config.getString("tablist.team_names." + key);
                teamNames.put(key.toLowerCase(), name);
            }
        }
    }

    private void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPlayers();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, updateInterval);
    }

    private void updateAllPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        if (!plugin.getGameManager().isStatus(GameStatus.INGAME)) {
            player.setPlayerListName(player.getName());
            return;
        }

        String team = teamManager.getPlayerTeam(player);
        if (team == null) team = "spectator";

        String teamColor = teamColors.getOrDefault(team.toLowerCase(), "&7");
        String teamName = teamNames.getOrDefault(team.toLowerCase(), "未知队伍");

        String formattedName = tabFormat
                .replace("%team_color%", teamColor)
                .replace("%team_name%", teamName)
                .replace("%player%", player.getName());

        player.setPlayerListName(formattedName);
    }

    public void reloadConfig() {
        teamColors.clear();
        teamNames.clear();
        loadConfig();
    }
} 
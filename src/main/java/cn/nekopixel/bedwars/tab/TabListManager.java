package cn.nekopixel.bedwars.tab;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class TabListManager {
    private final Main plugin;
    private final TeamManager teamManager;
    private final Map<String, String> teamColors;
    private final Map<String, String> teamNames;
    private String tabFormat;
    private int updateInterval;
    private BukkitRunnable updateTask;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Set<UUID> temporarySpectators = new HashSet<>();

    public TabListManager(Main plugin) {
        this.plugin = plugin;
        this.teamManager = Plugin.getInstance().getGameManager().getTeamManager();
        this.teamColors = new HashMap<>();
        this.teamNames = new HashMap<>();
        loadConfig();
        setupScoreboard();
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

    private void setupScoreboard() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        // healthObjective = board.getObjective("HealthTab");

        // if (healthObjective == null) {
        //     healthObjective = board.registerNewObjective("HealthTab", "dummy", ChatColor.YELLOW + "");
        // }
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
        if (!Plugin.getInstance().getGameManager().isStatus(GameStatus.INGAME)) {
            player.setPlayerListName(player.getName());
            removeHealthDisplay(player);
            return;
        }

        setupHealthDisplay(player);

        String team;
        if (temporarySpectators.contains(player.getUniqueId())) {
            team = "spectator";
        } else {
            team = teamManager.getPlayerTeam(player);
            if (team == null) team = "spectator";
        }

        String teamColor = teamColors.getOrDefault(team.toLowerCase(), "&7");
        String teamName = teamNames.getOrDefault(team.toLowerCase(), "未知队伍");

        String formattedName = tabFormat
                .replace("%team_color%", teamColor)
                .replace("%team_name%", teamName)
                .replace("%player%", player.getName());

        player.setPlayerListName(formattedName);

        updateHealthForAllPlayers(player);
    }

    private void setupHealthDisplay(Player player) {
        Scoreboard playerBoard = player.getScoreboard();
        
        if (playerBoard == null || playerBoard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            playerBoard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), playerBoard);
            player.setScoreboard(playerBoard);
        } else {
            playerScoreboards.put(player.getUniqueId(), playerBoard);
        }
        
        Objective healthObjective = playerBoard.getObjective("PlayerHealth");
        if (healthObjective == null) {
            healthObjective = playerBoard.registerNewObjective("PlayerHealth", "dummy", ChatColor.RED + "❤");
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            int health = (int) Math.round(onlinePlayer.getHealth());
            healthObjective.getScore(onlinePlayer.getName()).setScore(health);
        }
    }
    
    private void updateHealthForAllPlayers(Player updatedPlayer) {
        int health = (int) Math.round(updatedPlayer.getHealth());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = player.getScoreboard();
            if (board != null && board != Bukkit.getScoreboardManager().getMainScoreboard()) {
                Objective healthObjective = board.getObjective("PlayerHealth");
                if (healthObjective != null) {
                    healthObjective.getScore(updatedPlayer.getName()).setScore(health);
                }
            }
        }
    }
    
    private void removeHealthDisplay(Player player) {
        Scoreboard playerBoard = player.getScoreboard();
        
        if (playerBoard != null && playerBoard != Bukkit.getScoreboardManager().getMainScoreboard()) {
            Objective healthObjective = playerBoard.getObjective("PlayerHealth");
            if (healthObjective != null) {
                healthObjective.unregister();
            }
            if (playerScoreboards.containsKey(player.getUniqueId())) {
                playerScoreboards.remove(player.getUniqueId());
                if (playerBoard.getObjectives().isEmpty()) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        }
    }
    
    public void cleanupPlayer(Player player) {
        removeHealthDisplay(player);
        temporarySpectators.remove(player.getUniqueId());
    }

    public void reloadConfig() {
        teamColors.clear();
        teamNames.clear();
        loadConfig();
    }
    
    public void setTemporarySpectator(Player player, boolean isSpectator) {
        if (isSpectator) {
            temporarySpectators.add(player.getUniqueId());
        } else {
            temporarySpectators.remove(player.getUniqueId());
        }
        updatePlayer(player);
    }
    
    public void clearTemporarySpectators() {
        temporarySpectators.clear();
    }
    
    public String getTeamName(String team) {
        return teamNames.getOrDefault(team.toLowerCase(), "未知");
    }
}

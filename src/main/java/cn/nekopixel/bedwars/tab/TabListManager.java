package cn.nekopixel.bedwars.tab;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.player.PlayerStats;
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
import java.util.List;
import java.util.ArrayList;

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
    private boolean enabled;
    private List<String> headerLines;
    private List<String> footerLines;

    public TabListManager(Main plugin) {
        this.plugin = plugin;
        this.teamManager = Plugin.getInstance().getGameManager().getTeamManager();
        this.teamColors = new HashMap<>();
        this.teamNames = new HashMap<>();
        this.headerLines = new ArrayList<>();
        this.footerLines = new ArrayList<>();
        loadConfig();
        setupScoreboard();
        if (enabled) {
            startUpdateTask();
        }
    }

    private void loadConfig() {
        File tabConfigFile = new File(plugin.getDataFolder(), "tablist.yml");
        if (!tabConfigFile.exists()) {
            plugin.saveResource("tablist.yml", false);
        }

        FileConfiguration tabConfig = YamlConfiguration.loadConfiguration(tabConfigFile);
        enabled = tabConfig.getBoolean("tablist.enabled", true);
        updateInterval = tabConfig.getInt("tablist.update_interval", 20);

        headerLines.clear();
        if (tabConfig.contains("tablist.header")) {
            for (String line : tabConfig.getStringList("tablist.header")) {
                headerLines.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }

        footerLines.clear();
        if (tabConfig.contains("tablist.footer")) {
            for (String line : tabConfig.getStringList("tablist.footer")) {
                footerLines.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }

        File chattingConfigFile = new File(plugin.getDataFolder(), "chatting.yml");
        if (!chattingConfigFile.exists()) {
            plugin.saveResource("chatting.yml", false);
        }

        FileConfiguration chattingConfig = YamlConfiguration.loadConfiguration(chattingConfigFile);
        
        tabFormat = ChatColor.translateAlternateColorCodes('&', chattingConfig.getString("tablist.format", "%team_color%%team_name%&r &7%player%"));
        
        teamColors.clear();
        if (chattingConfig.contains("tablist.team_colors")) {
            for (String key : chattingConfig.getConfigurationSection("tablist.team_colors").getKeys(false)) {
                String color = chattingConfig.getString("tablist.team_colors." + key);
                teamColors.put(key.toLowerCase(), ChatColor.translateAlternateColorCodes('&', color));
            }
        }

        teamNames.clear();
        if (chattingConfig.contains("tablist.team_names")) {
            for (String key : chattingConfig.getConfigurationSection("tablist.team_names").getKeys(false)) {
                String name = chattingConfig.getString("tablist.team_names." + key);
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
            updateTabHeaderFooter(player);
        }
    }

    public void updatePlayer(Player player) {
        if (!enabled) return;
        
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

    private void updateTabHeaderFooter(Player player) {
        if (!enabled) return;
        
        String header = String.join("\n", headerLines);
        
        PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
        boolean isWaiting = Plugin.getInstance().getGameManager().isStatus(GameStatus.WAITING);
        
        List<String> processedFooter = new ArrayList<>();
        for (String line : footerLines) {
            boolean hasStatsPlaceholder = line.contains("%kills%") ||
                                        line.contains("%final_kills%") || 
                                        line.contains("%beds_broken%");
            
            if (isWaiting && hasStatsPlaceholder) {
                continue;
            }
            
            String processed = line
                    .replace("%kills%", String.valueOf(stats.getKills()))
                    .replace("%final_kills%", String.valueOf(stats.getFinalKills()))
                    .replace("%beds_broken%", String.valueOf(stats.getBedsBroken()));
            processedFooter.add(processed);
        }
        
        String footer = String.join("\n", processedFooter);
        
        player.setPlayerListHeaderFooter(header, footer);
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
        headerLines.clear();
        footerLines.clear();
        loadConfig();
        if (enabled && updateTask == null) {
            startUpdateTask();
        } else if (!enabled && updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
    
    public void setTemporarySpectator(Player player, boolean isSpectator) {
        if (isSpectator) {
            temporarySpectators.add(player.getUniqueId());
        } else {
            temporarySpectators.remove(player.getUniqueId());
        }
        updatePlayer(player);
        updateTabHeaderFooter(player);
    }
    
    public void clearTemporarySpectators() {
        temporarySpectators.clear();
    }
    
    public String getTeamName(String team) {
        return teamNames.getOrDefault(team.toLowerCase(), "未知");
    }
    
    public void onPlayerJoin(Player player) {
        if (enabled) {
            updatePlayer(player);
            updateTabHeaderFooter(player);
        }
    }
    
    public void onGameStatusChange() {
        if (enabled) {
            updateAllPlayers();
        }
    }
}

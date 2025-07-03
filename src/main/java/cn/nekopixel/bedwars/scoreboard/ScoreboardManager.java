package cn.nekopixel.bedwars.scoreboard;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.QueueManager;
import cn.nekopixel.bedwars.map.MapManager;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final Main plugin;
    private final Map<UUID, ScoreboardAPI> scoreboards = new ConcurrentHashMap<>();
    private FileConfiguration config;
    private QueueManager queueManager;
    private BukkitTask updateTask;
    
    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        
        GameManager gameManager = Plugin.getInstance().getGameManager();
        if (gameManager != null) {
            this.queueManager = gameManager.getQueueManager();
        }
        loadConfig();
        startUpdateTask();
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void createScoreboard(Player player) {
        ScoreboardAPI api = new ScoreboardAPI(player);
        scoreboards.put(player.getUniqueId(), api);
        updateScoreboard(player);
    }
    
    public void removeScoreboard(Player player) {
        ScoreboardAPI api = scoreboards.remove(player.getUniqueId());
        if (api != null) {
            api.delete();
        }
    }
    
    private void updateScoreboard(Player player) {
        ScoreboardAPI api = scoreboards.get(player.getUniqueId());
        if (api == null) return;
        
        GameStatus currentStatus = Plugin.getInstance().getGameManager().getCurrentStatus();
        
        if (currentStatus == GameStatus.WAITING) {
            updateWaitingScoreboard(player, api);
        }
    }
    
    private void updateWaitingScoreboard(Player player, ScoreboardAPI api) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("scoreboard.waiting.title", "&e起床战争"));
        api.setTitle(title);
        
        List<String> lines = new ArrayList<>();
        List<String> configLines = config.getStringList("scoreboard.waiting.lines");
        
        for (String line : configLines) {
            lines.add(replacePlaceholders(line));
        }
        
        for (int i = 0; i < lines.size(); i++) {
            api.setLine(i, lines.get(i));
        }
    }
    
    private String replacePlaceholders(String line) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
            config.getString("scoreboard.date_format", "MM/dd/yy"));
        line = line.replace("%date%", dateFormat.format(new Date()));
        line = line.replace("%server_id%", config.getString("scoreboard.server_id", "S1"));
        String mapName = "未知";
        if (Plugin.getInstance().getMapSetup() != null && 
            Plugin.getInstance().getMapSetup().getMapConfig() != null) {
            mapName = Plugin.getInstance().getMapSetup().getMapConfig()
                .getString("map.display_name", "未知");
        }
        line = line.replace("%map_name%", mapName);
        
        int currentPlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = getMaxPlayers();
        line = line.replace("%players%", String.valueOf(currentPlayers));
        line = line.replace("%max_players%", String.valueOf(maxPlayers));
        
        if (line.contains("%countdown_line%")) {
            line = line.replace("%countdown_line%", getCountdownLine());
        }
        
        String mode = plugin.getConfig().getString("game.mode", "4s");
        String modeDisplay = config.getString("scoreboard.mode_display." + mode, mode);
        line = line.replace("%mode_display%", modeDisplay);
        
        line = line.replace("%version%", plugin.getDescription().getVersion());
        
        return ChatColor.translateAlternateColorCodes('&', line);
    }
    
    private String getCountdownLine() {
        if (queueManager != null && queueManager.isCountingDown()) {
            String startingFormat = config.getString("scoreboard.waiting.countdown.starting", 
                "&f即将开始: &a%seconds%秒");
            return startingFormat.replace("%seconds%", String.valueOf(queueManager.getSeconds()));
        } else {
            return config.getString("scoreboard.waiting.countdown.waiting", "&f等待更多玩家...");
        }
    }
    
    private int getMaxPlayers() {
        String mode = plugin.getConfig().getString("game.mode", "4s");
        switch (mode.toLowerCase()) {
            case "solo":
                return 8;
            case "double":
            case "4s":
            case "3s":
                return 16;
            case "4v4":
                return 8;
            default:
                return 16;
        }
    }
    
    public QueueManager getQueueManager() {
        return queueManager;
    }
    
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        for (ScoreboardAPI api : scoreboards.values()) {
            api.delete();
        }
        scoreboards.clear();
        if (queueManager != null) {
            queueManager.stop();
        }
    }
}
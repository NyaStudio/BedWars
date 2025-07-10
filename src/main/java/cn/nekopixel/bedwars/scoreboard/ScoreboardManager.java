package cn.nekopixel.bedwars.scoreboard;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.QueueManager;
import cn.nekopixel.bedwars.game.EventManager;
import cn.nekopixel.bedwars.game.BedManager;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.player.PlayerStats;
import cn.nekopixel.bedwars.chat.ChatManager;
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
    private BukkitTask animationTask;
    
    private final List<String> titleAnimation = new ArrayList<>();
    private int animationIndex = 0;
    private boolean animationEnabled = false;
    private long animationInterval = 250L;
    
    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        
        GameManager gameManager = Plugin.getInstance().getGameManager();
        if (gameManager != null) {
            this.queueManager = gameManager.getQueueManager();
        }
        loadConfig();
        initTitleAnimation();
        startUpdateTask();
        startAnimationTask();
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        initTitleAnimation();
    }
    
    private void initTitleAnimation() {
        titleAnimation.clear();
        animationEnabled = config.getBoolean("scoreboard.waiting.title_animation.enabled", false);
        animationInterval = config.getLong("scoreboard.waiting.title_animation.interval_ticks", 250L);
        
        if (animationEnabled) {
            List<String> frames = config.getStringList("scoreboard.waiting.title_animation.frames");
            if (!frames.isEmpty()) {
                titleAnimation.addAll(frames);
            } else {
                animationEnabled = false;
            }
        }
        
        animationIndex = 0;
    }
    
    private String getAnimatedTitle() {
        if (titleAnimation.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', 
                config.getString("scoreboard.waiting.title", "&e&l起床战争"));
        }
        String frame = titleAnimation.get(animationIndex);
        return ChatColor.translateAlternateColorCodes('&', frame);
    }
    
    private void startAnimationTask() {
        long interval = animationEnabled ? animationInterval : 10L;
        
        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Plugin.getInstance().getGameManager().getCurrentStatus() == GameStatus.WAITING) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        ScoreboardAPI api = scoreboards.get(player.getUniqueId());
                        if (api != null) {
                            String title;
                            if (animationEnabled && !titleAnimation.isEmpty()) {
                                title = getAnimatedTitle();
                            } else {
                                title = ChatColor.translateAlternateColorCodes('&', 
                                    config.getString("scoreboard.waiting.title", "&e&l起床战争"));
                            }
                            api.setTitle(title);
                        }
                    }
                    
                    if (animationEnabled && !titleAnimation.isEmpty()) {
                        animationIndex = (animationIndex + 1) % titleAnimation.size();
                    }
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }
    
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    public void createScoreboard(Player player) {
        ScoreboardAPI api = new ScoreboardAPI(player);
        scoreboards.put(player.getUniqueId(), api);
        
        if (Plugin.getInstance().getGameManager().getCurrentStatus() == GameStatus.WAITING) {
            if (animationEnabled && !titleAnimation.isEmpty()) {
                api.setTitle(getAnimatedTitle());
            } else {
                api.setTitle(ChatColor.translateAlternateColorCodes('&', 
                    config.getString("scoreboard.waiting.title", "&e&l起床战争")));
            }
        } else {
            api.setTitle(ChatColor.translateAlternateColorCodes('&',
                config.getString("scoreboard.ingame.title", "&e&l起床战争")));
        }
        
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
        } else if (currentStatus == GameStatus.INGAME) {
            updateInGameScoreboard(player, api);
        }
    }
    
    private void updateWaitingScoreboard(Player player, ScoreboardAPI api) {

        List<String> lines = new ArrayList<>();
        List<String> configLines = config.getStringList("scoreboard.waiting.lines");
        
        for (String line : configLines) {
            lines.add(replacePlaceholders(line));
        }
        
        for (int i = 0; i < lines.size(); i++) {
            api.setLine(i, lines.get(i));
        }
    }
    
    private void updateInGameScoreboard(Player player, ScoreboardAPI api) {
        String title = ChatColor.translateAlternateColorCodes('&',
            config.getString("scoreboard.ingame.title", "&e&l起床战争"));
        api.setTitle(title);
        
        List<String> lines = new ArrayList<>();
        List<String> configLines = config.getStringList("scoreboard.ingame.lines");
        
        for (String line : configLines) {
            if (line.equals("%teams%")) {
                lines.addAll(getTeamLines(player));
            } else if (line.equals("%stats%")) {
                List<String> statsLines = getStatsLines(player);
                if (!statsLines.isEmpty()) {
                    lines.addAll(statsLines);
                }
            } else {
                lines.add(replaceInGamePlaceholders(line, player));
            }
        }
        
        for (int i = 0; i < lines.size() && i < 15; i++) {
            api.setLine(i, lines.get(i));
        }
        
        for (int i = lines.size(); i < 15; i++) {
            api.removeLine(i);
        }
    }
    
    private List<String> getTeamLines(Player player) {
        List<String> teamLines = new ArrayList<>();
        
        GameManager gameManager = Plugin.getInstance().getGameManager();
        TeamManager teamManager = gameManager.getTeamManager();
        BedManager bedManager = gameManager.getBedManager();
        ChatManager chatManager = Plugin.getInstance().getChatManager();
        
        Set<String> teams = teamManager.getConfigTeams();
        
        for (String teamKey : teams) {
            String teamColor = chatManager.getTeamColor(teamKey);
            String teamName = chatManager.getTeamName(teamKey);
            String tablistTeamName = Plugin.getInstance().getTabListManager().getTeamName(teamKey);
            String colorCode = ChatColor.translateAlternateColorCodes('&', teamColor);
            
            String status;
            if (!bedManager.hasBed(teamKey)) {
                int aliveCount = teamManager.getAlivePlayersInTeam(teamKey).size();
                if (aliveCount > 0) {
                    status = ChatColor.GREEN + String.valueOf(aliveCount);
                } else {
                    status = ChatColor.RED + "✘";
                }
            } else {
                status = ChatColor.GREEN + "✔";
            }
            
            String line = colorCode + tablistTeamName + " " + ChatColor.WHITE + teamName + ": " + status;
            
            if (teamManager.getPlayerTeam(player) != null &&
                teamManager.getPlayerTeam(player).equalsIgnoreCase(teamKey)) {
                line += ChatColor.GRAY + " YOU";
            }
            
            teamLines.add(line);
        }
        
        return teamLines;
    }
    
    private List<String> getStatsLines(Player player) {
        List<String> statsLines = new ArrayList<>();
        
        boolean statsEnabled = config.getBoolean("scoreboard.ingame.stats.enabled", true);
        if (!statsEnabled) {
            return statsLines;
        }
        
        GameManager gameManager = Plugin.getInstance().getGameManager();
        TeamManager teamManager = gameManager.getTeamManager();
        Set<String> teams = teamManager.getConfigTeams();
        int maxTeams = config.getInt("scoreboard.ingame.stats.max_teams", 4);
        
        if (teams.size() <= maxTeams) {
            PlayerStats stats = PlayerStats.getStats(player.getUniqueId());
            List<String> configStatsLines = config.getStringList("scoreboard.ingame.stats.lines");
            
            for (String line : configStatsLines) {
                line = line.replace("%kills%", String.valueOf(stats.getKills()));
                line = line.replace("%final_kills%", String.valueOf(stats.getFinalKills()));
                line = line.replace("%beds_broken%", String.valueOf(stats.getBedsBroken()));
                statsLines.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        
        return statsLines;
    }
    
    private String replaceInGamePlaceholders(String line, Player player) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
            config.getString("scoreboard.date_format", "MM/dd/yy"));
        line = line.replace("%date%", dateFormat.format(new Date()));
        line = line.replace("%server_id%", config.getString("scoreboard.server_id", "S1"));
        
        GameManager gameManager = Plugin.getInstance().getGameManager();
        EventManager eventManager = gameManager.getEventManager();
        if (eventManager != null) {
            EventManager.NextEvent nextEvent = eventManager.getNextEvent();
            line = line.replace("%event_name%", nextEvent.getName());
            line = line.replace("%event_time%", nextEvent.getFormattedTime());
        } else {
            line = line.replace("%event_name%", "等待事件");
            line = line.replace("%event_time%", "N/A");
        }
        
        line = line.replace("%version%", plugin.getDescription().getVersion());
        
        return ChatColor.translateAlternateColorCodes('&', line);
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
        QueueManager currentQueueManager = null;
        GameManager gameManager = Plugin.getInstance().getGameManager();
        if (gameManager != null) {
            currentQueueManager = gameManager.getQueueManager();
        }
        
        if (currentQueueManager != null && currentQueueManager.isCountingDown()) {
            String startingFormat = config.getString("scoreboard.waiting.countdown.starting", 
                "&f即将开始: &a%seconds%秒");
            return startingFormat.replace("%seconds%", String.valueOf(currentQueueManager.getSeconds()));
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
        if (animationTask != null) {
            animationTask.cancel();
        }
        for (ScoreboardAPI api : scoreboards.values()) {
            api.delete();
        }
        scoreboards.clear();
        if (queueManager != null) {
            queueManager.stop();
        }
    }
    
    public void reloadConfig() {
        loadConfig();
        
        if (animationTask != null) {
            animationTask.cancel();
        }
        startAnimationTask();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }
    
    public void forceUpdateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }
}
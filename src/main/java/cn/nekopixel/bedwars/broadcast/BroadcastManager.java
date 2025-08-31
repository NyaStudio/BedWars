package cn.nekopixel.bedwars.broadcast;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.BedManager;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BroadcastManager {
    private final Main plugin;
    private static BroadcastManager instance;
    private FileConfiguration broadcastConfig;
    
    private BroadcastManager(Main plugin) {
        this.plugin = plugin;
        loadBroadcastConfig();
    }
    
    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new BroadcastManager(plugin);
        }
    }
    
    public static BroadcastManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BroadcastManager 未初始化!");
        }
        return instance;
    }
    
    private void loadBroadcastConfig() {
        File configFile = new File(plugin.getDataFolder(), "broadcast.yml");
        if (!configFile.exists()) {
            plugin.saveResource("broadcast.yml", false);
        }
        broadcastConfig = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void reloadConfig() {
        loadBroadcastConfig();
    }

    public void playerJoinWaiting(String coloredName, int currentPlayers, int maxPlayers) {
        String joinMessage = LanguageManager.getInstance().getMessage("broadcast.join_waiting",
            "player", coloredName, "current", String.valueOf(currentPlayers), "max", String.valueOf(maxPlayers));
        Bukkit.broadcastMessage(joinMessage);
    }

    public void playerQuitWaiting(String coloredName) {
        String quitMessage = LanguageManager.getInstance().getMessage("broadcast.quit_waiting", "player", coloredName);
        Bukkit.broadcastMessage(quitMessage);
    }

    public void playerReconnect(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        String message = LanguageManager.getInstance().getMessage("broadcast.reconnect", "player", playerName);
        Bukkit.broadcastMessage(message);
    }

    public void playerJoinInGame(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        String message = LanguageManager.getInstance().getMessage("broadcast.join_ingame", "player", playerName);
        Bukkit.broadcastMessage(message);
    }

    public void playerDisconnect(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        String message = LanguageManager.getInstance().getMessage("broadcast.disconnect", "player", playerName);
        Bukkit.broadcastMessage(message);
    }

    public void playerFellIntoVoid(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        String message = LanguageManager.getInstance().getMessage("broadcast.fell_void", "player", playerName);
        Bukkit.broadcastMessage(message);
    }
    
    public void playerKilledIntoVoid(Player victim, String victimTeam, Player killer, String killerTeam, boolean isFinalKill) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        
        String victimColor = bedManager.getTeamChatColor(victimTeam);
        String killerColor = bedManager.getTeamChatColor(killerTeam);
        String victimName = victimColor + victim.getName();
        String killerName = killerColor + killer.getName();
        
        List<String> messages = broadcastConfig.getStringList("void_kill_messages");
        
        if (messages.isEmpty()) {
            String defaultMessage = LanguageManager.getInstance().getMessage("broadcast.killed_void", "victim", victimName, "killer", killerName);
            if (isFinalKill) {
                defaultMessage += LanguageManager.getInstance().getMessage("broadcast.final_kill");
            }
            Bukkit.broadcastMessage(defaultMessage);
            return;
        }
        
        String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        message = message.replace("{victim}", victimName);
        message = message.replace("{killer}", killerName);
        
        if (isFinalKill) {
            message += LanguageManager.getInstance().getMessage("broadcast.final_kill");
        }
        
        Bukkit.broadcastMessage(message);
    }
    
    public void playerKilled(Player victim, String victimTeam, Player killer, String killerTeam, boolean isFinalKill) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        
        String victimColor = bedManager.getTeamChatColor(victimTeam);
        String killerColor = bedManager.getTeamChatColor(killerTeam);
        String victimName = victimColor + victim.getName();
        String killerName = killerColor + killer.getName();
        
        List<String> messages = broadcastConfig.getStringList("kill_messages");
        
        if (messages.isEmpty()) {
            String defaultMessage = LanguageManager.getInstance().getMessage("broadcast.killed", "victim", victimName, "killer", killerName);
            if (isFinalKill) {
                defaultMessage += LanguageManager.getInstance().getMessage("broadcast.final_kill");
            }
            Bukkit.broadcastMessage(defaultMessage);
            return;
        }
        
        String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        message = message.replace("{victim}", victimName);
        message = message.replace("{killer}", killerName);
        
        if (isFinalKill) {
            message += LanguageManager.getInstance().getMessage("broadcast.final_kill");
        }
        
        Bukkit.broadcastMessage(message);
    }
    
    // game

    public void bedDestroyed(String teamColor, String teamName, String destroyerColor, String destroyerName) {
        List<String> messages = broadcastConfig.getStringList("bed_destroy_messages");
        
        if (messages.isEmpty()) {
            Bukkit.broadcastMessage("");
            String team = teamColor + teamName;
            String destroyer = destroyerColor + destroyerName;
            String message = LanguageManager.getInstance().getMessage("broadcast.bed_destroyed", "team", team, "destroyer", destroyer);
            Bukkit.broadcastMessage(message);
            Bukkit.broadcastMessage("");
            return;
        }
        
        String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        message = message.replace("{team}", teamColor + teamName);
        message = message.replace("{destroyer}", destroyerColor + destroyerName);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f§l床被破坏了 > " + message);
        Bukkit.broadcastMessage("");
    }

    public void teamEliminated(String teamColor, String teamName) {
        Bukkit.broadcastMessage("");
        String team = teamColor + teamName;
        String message = LanguageManager.getInstance().getMessage("broadcast.team_eliminated", "team", team);
        Bukkit.broadcastMessage(message);
        Bukkit.broadcastMessage("");
    }

    public void gameVictory(String teamColor, String teamName) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(LanguageManager.getInstance().getMessage("broadcast.game_victory"));
        Bukkit.broadcastMessage("");
        String team = teamColor + teamName;
        String message = LanguageManager.getInstance().getMessage("broadcast.game_winner", "team", team);
        Bukkit.broadcastMessage(message);
        Bukkit.broadcastMessage("");
    }

    public void gameDraw() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(LanguageManager.getInstance().getMessage("broadcast.game_draw"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(LanguageManager.getInstance().getMessage("broadcast.game_draw_message"));
        Bukkit.broadcastMessage("");
    }

    public void diamondUpgrade(String romanLevel) {
        String message = LanguageManager.getInstance().getMessage("broadcast.diamond_upgrade", "level", romanLevel);
        Bukkit.broadcastMessage(message);
    }

    public void emeraldUpgrade(String romanLevel) {
        String message = LanguageManager.getInstance().getMessage("broadcast.emerald_upgrade", "level", romanLevel);
        Bukkit.broadcastMessage(message);
    }

    public void allBedsDestroyed() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(LanguageManager.getInstance().getMessage("broadcast.all_beds_destroyed"));
        Bukkit.broadcastMessage("");
    }
    
    // chat

    public void chatMessage(String formattedMessage) {
        Bukkit.broadcastMessage(formattedMessage);
    }

    public void shoutMessage(String formattedMessage) {
        Bukkit.broadcastMessage(formattedMessage);
    }
    
    // tools

    public void sendEmptyLine() {
        Bukkit.broadcastMessage("");
    }

    public void sendCustomMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
}
package cn.nekopixel.bedwars.broadcast;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.BedManager;
import cn.nekopixel.bedwars.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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
        String joinMessage = coloredName + " §e加入了游戏 (§b" + currentPlayers + "§e/§b" + maxPlayers + "§e)！";
        Bukkit.broadcastMessage(joinMessage);
    }

    public void playerQuitWaiting(String coloredName) {
        String quitMessage = coloredName + " §e离开了游戏！";
        Bukkit.broadcastMessage(quitMessage);
    }

    public void playerReconnect(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        Bukkit.broadcastMessage(playerName + " §f重新连接");
    }

    public void playerJoinInGame(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        Bukkit.broadcastMessage(playerName + " §e加入游戏！");
    }

    public void playerDisconnect(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        Bukkit.broadcastMessage(playerName + " §f断开连接");
    }
    
    public void playerFellIntoVoid(Player player, String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(team);
        String playerName = teamColor + player.getName();
        Bukkit.broadcastMessage(playerName + " §7失足跌入虚空。");
    }
    
    public void playerKilledIntoVoid(Player victim, String victimTeam, Player killer, String killerTeam, boolean isFinalKill) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        
        String victimColor = bedManager.getTeamChatColor(victimTeam);
        String killerColor = bedManager.getTeamChatColor(killerTeam);
        String victimName = victimColor + victim.getName();
        String killerName = killerColor + killer.getName();
        
        List<String> messages = broadcastConfig.getStringList("void_kill_messages");
        
        if (messages.isEmpty()) {
            String defaultMessage = victimName + " §7被 " + killerName + " §7丢下虚空。";
            if (isFinalKill) {
                defaultMessage += " §b§l最终击杀！";
            }
            Bukkit.broadcastMessage(defaultMessage);
            return;
        }
        
        String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        message = message.replace("{victim}", victimName);
        message = message.replace("{killer}", killerName);
        
        if (isFinalKill) {
            message += " §b§l最终击杀！";
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
            String defaultMessage = victimName + " §7被 " + killerName + " §7击杀！";
            if (isFinalKill) {
                defaultMessage += " §b§l最终击杀！";
            }
            Bukkit.broadcastMessage(defaultMessage);
            return;
        }
        
        String message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        message = message.replace("{victim}", victimName);
        message = message.replace("{killer}", killerName);
        
        if (isFinalKill) {
            message += " §b§l最终击杀！";
        }
        
        Bukkit.broadcastMessage(message);
    }
    
    // game

    public void bedDestroyed(String teamColor, String teamName, String destroyerColor, String destroyerName) {
        List<String> messages = broadcastConfig.getStringList("bed_destroy_messages");
        
        if (messages.isEmpty()) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§f§l床被破坏了 > " + teamColor + teamName + " §7的床被 " + destroyerColor + destroyerName + " §7破坏！");
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
        Bukkit.broadcastMessage("§f§l团灭 > " + teamColor + teamName + " §7已被淘汰！");
        Bukkit.broadcastMessage("");
    }

    public void gameVictory(String teamColor, String teamName) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e游戏结束！");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f获胜队伍 - " + teamColor + teamName);
        Bukkit.broadcastMessage("");
    }

    public void gameDraw() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l游戏结束！");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e本局游戏平局！");
        Bukkit.broadcastMessage("");
    }

    public void diamondUpgrade(String romanLevel) {
        Bukkit.broadcastMessage("§b钻石生成点§e已经升至§c" + romanLevel + "§e级。");
    }

    public void emeraldUpgrade(String romanLevel) {
        Bukkit.broadcastMessage("§2绿宝石生成点§e已经升至§c" + romanLevel + "§e级。");
    }

    public void allBedsDestroyed() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f§l床自毁 > §7所有的床均已被破坏！");
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
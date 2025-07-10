package cn.nekopixel.bedwars.broadcast;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.BedManager;
import cn.nekopixel.bedwars.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastManager {
    private final Main plugin;
    private static BroadcastManager instance;
    
    private BroadcastManager(Main plugin) {
        this.plugin = plugin;
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

    // player

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
    
    // game

    public void bedDestroyed(String teamColor, String teamName, String destroyerColor, String destroyerName) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f床被破坏了 > " + teamColor + teamName + " §7的床被 " + destroyerColor + destroyerName + " §7拆烂！");
        Bukkit.broadcastMessage("");
    }

    public void teamEliminated(String teamColor, String teamName) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f团灭 > " + teamColor + teamName + " §7已被淘汰！");
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
        Bukkit.broadcastMessage("§b钻石§e生成点已经升至§c" + romanLevel + "§e级。");
    }

    public void emeraldUpgrade(String romanLevel) {
        Bukkit.broadcastMessage("§2绿宝石§e生成点已经升至§c" + romanLevel + "§e级。");
    }

    public void allBedsDestroyed() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f床自毁 §7> §f所有的床均已被破坏！");
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
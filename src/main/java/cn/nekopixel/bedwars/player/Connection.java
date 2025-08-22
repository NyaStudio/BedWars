package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Connection {
    private static Connection instance;
    private final Main plugin;
    private final Set<UUID> authorizedPlayers;
    
    private Connection(Main plugin) {
        this.plugin = plugin;
        this.authorizedPlayers = new HashSet<>();
    }
    
    public static Connection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Connection 未初始化");
        }
        return instance;
    }
    
    public static void initialize(Main plugin) {
        if (instance != null) {
            throw new IllegalStateException("Connection 已经初始化过了");
        }
        instance = new Connection(plugin);
    }

    public boolean canPlayerJoin(PlayerLoginEvent event) {
        GameStatus currentStatus = GameManager.getInstance().getCurrentStatus();
        
        if (currentStatus == GameStatus.WAITING) {
            return canJoinWaiting(event);
        } else if (currentStatus == GameStatus.INGAME) {
            return canJoinInGame(event);
        }

        return false;
    }

    private boolean canJoinWaiting(PlayerLoginEvent event) {
        int currentPlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = getMaxPlayersForCurrentMode();
        
        if (currentPlayers >= maxPlayers) {
            denyLogin(event, 
                ChatColor.RED + "人满了喵！");
            return false;
        }

        return true;
    }

    private boolean canJoinInGame(PlayerLoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        if (!authorizedPlayers.contains(playerId)) {
            denyLogin(event,
                ChatColor.RED + "游戏已经开始了喵！");
            return false;
        }
        
        return true;
    }

    private int getMaxPlayersForCurrentMode() {
        String mode = plugin.getConfig().getString("game.mode", "4s").toLowerCase();
        return plugin.getConfig().getInt("game.max_players." + mode, 16);
    }

    private void denyLogin(PlayerLoginEvent event, String message) {
        event.disallow(PlayerLoginEvent.Result.KICK_FULL, message);
    }

    public void recordGamePlayers() {
        authorizedPlayers.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            authorizedPlayers.add(player.getUniqueId());
        }
    }

    public void clearAuthorizedPlayers() {
        authorizedPlayers.clear();
    }
}
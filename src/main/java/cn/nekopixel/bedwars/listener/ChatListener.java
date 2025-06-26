package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.SpectatorManager;
import cn.nekopixel.bedwars.game.PlayerDeathManager;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;

public class ChatListener implements Listener {
    private final Main plugin;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        int gameMode = plugin.getConfig().getInt("game.mode", 2);
        String formattedMessage = Plugin.getInstance().getChatManager().formatMessage(player, message);
        
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            Bukkit.broadcastMessage(formattedMessage);
            return;
        }
        
        if (gameMode == 1) {
            Bukkit.broadcastMessage(formattedMessage);
            return;
        }
        
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        SpectatorManager spectatorManager = GameManager.getInstance().getSpectatorManager();
        PlayerDeathManager deathManager = GameManager.getInstance().getPlayerDeathManager();
        
        String playerTeam = teamManager.getPlayerTeam(player);
        UUID playerId = player.getUniqueId();
        
        boolean isObserverState = playerTeam == null ||
                                  spectatorManager.isSpectator(playerId) || 
                                  deathManager.isRespawning(playerId);
        
        if (isObserverState) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                UUID onlinePlayerId = onlinePlayer.getUniqueId();
                String onlinePlayerTeam = teamManager.getPlayerTeam(onlinePlayer);
                
                boolean isOnlinePlayerObserver = onlinePlayerTeam == null ||
                                                spectatorManager.isSpectator(onlinePlayerId) ||
                                                deathManager.isRespawning(onlinePlayerId);
                
                if (isOnlinePlayerObserver) {
                    onlinePlayer.sendMessage(formattedMessage);
                }
            }
            return;
        }
        
        Set<UUID> teammates = teamManager.getTeamPlayers(playerTeam);
        for (UUID teammateId : teammates) {
            Player teammate = Bukkit.getPlayer(teammateId);
            if (teammate != null && teammate.isOnline()) {
                if (!spectatorManager.isSpectator(teammateId) && !deathManager.isRespawning(teammateId)) {
                    teammate.sendMessage(formattedMessage);
                }
            }
        }
    }
} 
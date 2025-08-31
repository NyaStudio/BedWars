package cn.nekopixel.bedwars.scoreboard;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ScoreboardListener implements Listener {
    private final Main plugin;
    private final ScoreboardManager scoreboardManager;
    
    public ScoreboardListener(Main plugin, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        scoreboardManager.createScoreboard(player);
        
        if (scoreboardManager.getQueueManager() != null) {
            scoreboardManager.getQueueManager().checkAndUpdateCountdown();
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        scoreboardManager.removeScoreboard(player);
        
        if (scoreboardManager.getQueueManager() != null) {
            scoreboardManager.getQueueManager().checkAndUpdateCountdown();
        }
    }
    
    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (scoreboardManager.getQueueManager() != null) {
            scoreboardManager.getQueueManager().stop();
        }
    }
}
package cn.nekopixel.bedwars.scoreboard;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
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
        scoreboardManager.createScoreboard(event.getPlayer());
        GameStatus status = Plugin.getInstance().getGameManager().getCurrentStatus();
        if (status == GameStatus.WAITING) {
            scoreboardManager.getCountdownManager().checkAndUpdateCountdown();
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scoreboardManager.removeScoreboard(event.getPlayer());
        GameStatus status = Plugin.getInstance().getGameManager().getCurrentStatus();
        if (status == GameStatus.WAITING) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                scoreboardManager.getCountdownManager().checkAndUpdateCountdown();
            }, 1L);
        }
    }
    
    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() != GameStatus.WAITING) {
            scoreboardManager.getCountdownManager().stop();
        }
    }
}
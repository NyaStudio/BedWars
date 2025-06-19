package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class HealthBar implements Listener {

    private static final String OBJECTIVE_NAME = "§c❤";
    private final Main plugin;

    public HealthBar(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.INGAME) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                setupHealthBar(player);
                player.setHealth(player.getHealth());
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeHealthBar(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            Player player = event.getPlayer();
            setupHealthBar(player);
            player.setHealth(player.getHealth());
        }
    }

    private void setupHealthBar(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(OBJECTIVE_NAME, "health");
            obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
        player.setScoreboard(scoreboard);
    }

    private void removeHealthBar(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj != null) {
            obj.unregister();
        }
    }
}
package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
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
        
        initializeHealthBars();
    }
    
    private void initializeHealthBars() {
        if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    setupHealthBar(player);
                }
            }, 2L);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeHealthBar(player);
            }
        }
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.INGAME) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    setupHealthBar(player);
                }
            }, 2L);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeHealthBar(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    setupHealthBar(player);
                }
            }, 2L);
        } else {
            removeHealthBar(player);
        }
    }

    private void setupHealthBar(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");
            obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
        
        updateAllHealthDisplays();
    }
    
    private void updateAllHealthDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();
            Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
            if (obj != null) {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    obj.getScore(target.getName()).setScore((int) Math.round(target.getHealth()));
                }
            }
        }
    }

    private void removeHealthBar(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if (obj != null) {
            obj.unregister();
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            Bukkit.getScheduler().runTaskLater(plugin, this::updateAllHealthDisplays, 1L);
        }
    }
    
    @EventHandler
    public void onPlayerRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            Bukkit.getScheduler().runTaskLater(plugin, this::updateAllHealthDisplays, 1L);
        }
    }
}
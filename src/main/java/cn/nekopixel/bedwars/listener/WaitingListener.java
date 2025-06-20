package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class WaitingListener implements Listener {
    private final Main plugin;

    public WaitingListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            Player player = event.getPlayer();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setLevel(0);
            player.setExp(0f);
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            event.setCancelled(true);
        }
    }
} 
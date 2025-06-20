package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.scheduler.BukkitTask;

public class PlayerHealth implements Listener {
    private final Main plugin;
    private BukkitTask regenTask;

    public PlayerHealth(Main plugin) {
        this.plugin = plugin;
        startSlowRegen();
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        if (e.getEntity() instanceof Player) {
            EntityRegainHealthEvent.RegainReason reason = e.getRegainReason();
            if (reason == EntityRegainHealthEvent.RegainReason.SATIATED || reason == EntityRegainHealthEvent.RegainReason.REGEN) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange e) {
        if (e.getNewStatus() == GameStatus.INGAME) {
            startSlowRegen();
        } else if (e.getNewStatus() == GameStatus.ENDING || e.getNewStatus() == GameStatus.RESETTING) {
            stopSlowRegen();
        }
    }

    private void startSlowRegen() {
        stopSlowRegen();

        regenTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
                return;
            }

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getHealth() < player.getMaxHealth()) {
                    double newHealth = Math.min(player.getHealth() + 0.5, player.getMaxHealth());
                    player.setHealth(newHealth);
                }
            }
        }, 20L * 5, 20L * 5);
    }

    private void stopSlowRegen() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
    }
}

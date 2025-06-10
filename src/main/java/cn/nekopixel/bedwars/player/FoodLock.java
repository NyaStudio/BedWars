package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FoodLock {
    private final Main plugin;
    private BukkitRunnable task;
    private static final float LOCKED_FOOD_LEVEL = 20.0f; // 最大饱食度

    public FoodLock(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        lockFoodLevel(player);
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 20L, 20L); // 每秒检查一次
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void lockFoodLevel(Player player) {
        if (player.getFoodLevel() != LOCKED_FOOD_LEVEL) {
            player.setFoodLevel((int) LOCKED_FOOD_LEVEL);
        }
        if (player.getSaturation() != LOCKED_FOOD_LEVEL) {
            player.setSaturation(LOCKED_FOOD_LEVEL);
        }
    }
} 
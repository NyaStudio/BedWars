package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class RemoveItems {
    private final Main plugin;
    private BukkitRunnable task;

    public RemoveItems(Main plugin) {
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
                        removeEmptyBottles(player);
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

    private void removeEmptyBottles(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.GLASS_BOTTLE) {
                player.getInventory().setItem(i, null);
            }
        }
    }
} 
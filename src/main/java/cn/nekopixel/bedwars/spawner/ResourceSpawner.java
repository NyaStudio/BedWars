package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

public abstract class ResourceSpawner {
    protected final Main plugin;
    protected final String type;
    protected final long interval;
    protected BukkitRunnable task;

    public ResourceSpawner(Main plugin, String type, long interval) {
        this.plugin = plugin;
        this.type = type;
        this.interval = interval;
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (GameManager.getInstance().getCurrentStatus() != GameStatus.INGAME) {
                    return;
                }

                List<Map<?, ?>> spawnerLocations = plugin.getConfig().getMapList("spawners." + type);
                for (Map<?, ?> locMap : spawnerLocations) {
                    Location loc = Location.deserialize((Map<String, Object>) locMap);
                    World world = loc.getWorld();
                    if (world != null) {
                        Item item = world.dropItem(loc, getItem());
                        item.setVelocity(new Vector(0, 0, 0));
                        item.teleport(loc.add(0.5, 0.1, 0.5));
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 0L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    protected abstract org.bukkit.inventory.ItemStack getItem();
} 
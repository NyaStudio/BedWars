package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public abstract class ResourceSpawner {
    protected final Main plugin;
    protected final String type;
    protected final long interval;
    protected BukkitRunnable task;
    protected final Map mapSetup;

    // 每个资源点的活动掉落物
    private final java.util.Map<Location, Item> activeDrops = new HashMap<>();

    // 被暂停的资源点
    private final Set<Location> pausedPoints = new HashSet<>();

    public ResourceSpawner(Main plugin, String type, long interval) {
        this.plugin = plugin;
        this.type = type;
        this.interval = interval;
        this.mapSetup = new Map(plugin);
    }

    public void start() {
        if (task != null) task.cancel();

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (GameManager.getInstance().getCurrentStatus() != GameStatus.INGAME) return;

                List<java.util.Map<?, ?>> spawnerLocations = mapSetup.getMapConfig().getMapList("spawners." + type);
                for (java.util.Map<?, ?> locMap : spawnerLocations) {
                    @SuppressWarnings("unchecked")
                    Location baseLoc = Location.deserialize((java.util.Map<String, Object>) locMap);
                    World world = baseLoc.getWorld();
                    if (world == null) continue;

                    // 检查是否满了
                    int nearbyAmount = countNearbyItems(baseLoc, getMaterial());

                    if (nearbyAmount >= getMaxAmount()) {
                        pausedPoints.add(baseLoc);
                        continue; // 跳过生成
                    } else {
                        pausedPoints.remove(baseLoc);
                    }

                    spawnOrUpdateItem(baseLoc, getItem());
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

        for (Item item : activeDrops.values()) {
            item.remove();
        }
        activeDrops.clear();
        pausedPoints.clear();
    }

    protected abstract ItemStack getItem();
    protected abstract Material getMaterial();
    protected abstract int getMaxAmount();

    private void spawnOrUpdateItem(Location baseLoc, ItemStack toDrop) {
        World world = baseLoc.getWorld();
        if (world == null) return;

        Location loc = baseLoc.clone().add(0.5, 0.1, 0.5); // 精准中心
        Item existing = activeDrops.get(baseLoc);

        if (existing != null && !existing.isDead()) {
            ItemStack current = existing.getItemStack();
            if (current.isSimilar(toDrop)) {
                int newAmount = Math.min(current.getAmount() + toDrop.getAmount(), toDrop.getMaxStackSize());
                current.setAmount(newAmount);
                existing.setItemStack(current);
                return;
            }
        }

        // 新实体
        Item item = world.spawn(loc, Item.class, i -> {
            i.setItemStack(toDrop.clone());
            i.setVelocity(new Vector(0, 0, 0));
            i.setPickupDelay(0);
        });
        activeDrops.put(baseLoc, item);
    }

    private int countNearbyItems(Location baseLoc, Material type) {
        World world = baseLoc.getWorld();
        if (world == null) return 0;

        int total = 0;
        Location center = baseLoc.clone().add(0.5, 0.5, 0.5);
        for (Item item : world.getEntitiesByClass(Item.class)) {
            if (item.isDead()) continue;
            if (!item.getItemStack().getType().equals(type)) continue;

            Location itemLoc = item.getLocation();
            if (Math.abs(itemLoc.getX() - center.getX()) <= 1.5 &&
                    Math.abs(itemLoc.getY() - center.getY()) <= 1.5 &&
                    Math.abs(itemLoc.getZ() - center.getZ()) <= 1.5) {
                total += item.getItemStack().getAmount();
            }
        }

        return total;
    }
}
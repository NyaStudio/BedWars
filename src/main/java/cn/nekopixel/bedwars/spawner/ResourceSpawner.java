package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public abstract class ResourceSpawner implements Listener {
    protected final Main plugin;
    protected final String type;
    protected long interval;
    protected BukkitRunnable task;
    protected final Map mapSetup;

    private final java.util.Map<Location, Item> activeDrops = new HashMap<>();
    private final Set<Location> pausedPoints = new HashSet<>();

    public ResourceSpawner(Main plugin, String type, long interval) {
        this.plugin = plugin;
        this.type = type;
        this.interval = interval;
        this.mapSetup = new Map(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setSpawnInterval(long interval) {
        this.interval = interval;
        if (task != null) {
            task.cancel();
            start();
        }
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

                    int nearbyAmount = countNearbyItems(baseLoc, getMaterial());
                    if (nearbyAmount >= getMaxAmount()) {
                        pausedPoints.add(baseLoc);
                        continue;
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

    private Location findNearestBlock(Location origin, Material target, int radius) {
        World world = origin.getWorld();
        if (world == null) return null;

        Location closest = null;
        double closestDistance = Double.MAX_VALUE;

        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Location check = new Location(world, baseX + dx, baseY + dy, baseZ + dz);
                    if (check.getBlock().getType() == target) {
                        double distance = check.distanceSquared(origin);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closest = check;
                        }
                    }
                }
            }
        }

        return closest;
    }

    private void spawnOrUpdateItem(Location baseLoc, ItemStack toDrop) {
        World world = baseLoc.getWorld();
        if (world == null) return;

        Location center = baseLoc.clone().add(0.5, 1.0, 0.5);

        Material mat = toDrop.getType();
        if (mat == Material.DIAMOND || mat == Material.EMERALD) {
            Material targetBlock = (mat == Material.DIAMOND) ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
            Location nearest = findNearestBlock(baseLoc, targetBlock, 3);
            if (nearest != null) {
                center = nearest.clone().add(0.5, 1.0, 0.5);
            }
        }

        Item existing = activeDrops.get(baseLoc);
        if (existing != null && !existing.isDead()) {
            ItemStack current = existing.getItemStack();
            if (current.isSimilar(toDrop)) {
                int newAmount = Math.min(current.getAmount() + toDrop.getAmount(), toDrop.getMaxStackSize());
                current.setAmount(newAmount);
                existing.setItemStack(current);

                if (existing.getLocation().distanceSquared(center) > 0.01) {
                    existing.teleport(center);
                }
                return;
            }
        }

        Item item = world.dropItem(center, toDrop.clone());
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(0);
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

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        activeDrops.entrySet().removeIf(entry -> entry.getValue().equals(item));
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        activeDrops.values().removeIf(i -> i.equals(event.getEntity()));
    }
}

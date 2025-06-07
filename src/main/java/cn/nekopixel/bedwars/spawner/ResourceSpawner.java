package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;

import java.util.*;

public abstract class ResourceSpawner implements Listener {
    protected final Main plugin;
    protected final String type;
    protected long interval;
    protected BukkitRunnable task;
    protected final Map mapSetup;

    private final java.util.Map<Location, Item> activeDrops = new HashMap<>();
    private final Set<Location> pausedPoints = new HashSet<>();
    private final java.util.Map<Location, List<Entity>> hologramEntities = new HashMap<>();
    private final java.util.Map<Location, BukkitRunnable> countdownTasks = new HashMap<>();
    private int level = 1;

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

        createHolograms();

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
        
        removeHolograms();
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

    private void createHolograms() {
        List<java.util.Map<?, ?>> spawnerLocations = mapSetup.getMapConfig().getMapList("spawners." + type);
        for (java.util.Map<?, ?> locMap : spawnerLocations) {
            @SuppressWarnings("unchecked")
            Location baseLoc = Location.deserialize((java.util.Map<String, Object>) locMap);
            World world = baseLoc.getWorld();
            if (world == null) continue;

            Location center = baseLoc.clone().add(0.5, 1.0, 0.5);

            Material mat = getMaterial();
            Material blockMaterial;
            if (mat == Material.DIAMOND) {
                blockMaterial = Material.DIAMOND_BLOCK;
            } else if (mat == Material.EMERALD) {
                blockMaterial = Material.EMERALD_BLOCK;
            } else {
                continue;
            }

            if (mat == Material.DIAMOND || mat == Material.EMERALD) {
                Material targetBlock = (mat == Material.DIAMOND) ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
                Location nearest = findNearestBlock(baseLoc, targetBlock, 3);
                if (nearest != null) {
                    center = nearest.clone().add(0.5, 1.0, 0.5);
                }
            }

            Location hologramLoc = center.clone().add(0, 4.0, 0);
            List<Entity> entities = new ArrayList<>();

            ArmorStand levelStand = (ArmorStand) world.spawnEntity(hologramLoc.clone().add(0, 0.8, 0), EntityType.ARMOR_STAND);
            levelStand.setVisible(false);
            levelStand.setGravity(false);
            levelStand.setMarker(true);
            levelStand.setCustomName("§e等级 " + "§c" + getRomanNumeral(level));
            levelStand.setCustomNameVisible(true);
            entities.add(levelStand);

            ArmorStand nameStand = (ArmorStand) world.spawnEntity(hologramLoc.clone().add(0, 0.5, 0), EntityType.ARMOR_STAND);
            nameStand.setVisible(false);
            nameStand.setGravity(false);
            nameStand.setMarker(true);
            nameStand.setCustomName((mat == Material.DIAMOND ? "§3" : "§2") + (mat == Material.DIAMOND ? "钻石" : "绿宝石"));
            nameStand.setCustomNameVisible(true);
            entities.add(nameStand);

            ArmorStand countdownStand = (ArmorStand) world.spawnEntity(hologramLoc.clone().add(0, 0.2, 0), EntityType.ARMOR_STAND);
            countdownStand.setVisible(false);
            countdownStand.setGravity(false);
            countdownStand.setMarker(true);
            countdownStand.setCustomName("§e将在 §c" + (interval / 20) + "§e 秒后产出");
            countdownStand.setCustomNameVisible(true);
            entities.add(countdownStand);

            ArmorStand blockStand = (ArmorStand) world.spawnEntity(hologramLoc.clone().add(0, -1.0, 0), EntityType.ARMOR_STAND);
            blockStand.setVisible(false);
            blockStand.setGravity(false);
            blockStand.setMarker(true);
            blockStand.setSmall(true);
            blockStand.setHelmet(new ItemStack(blockMaterial));
            blockStand.setHeadPose(new EulerAngle(0, 0, 0));
            entities.add(blockStand);

            hologramEntities.put(baseLoc, entities);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (GameManager.getInstance().getCurrentStatus() != GameStatus.INGAME) {
                        cancel();
                        return;
                    }
                    EulerAngle currentPose = blockStand.getHeadPose();
                    blockStand.setHeadPose(new EulerAngle(currentPose.getX(), currentPose.getY() + 0.1, currentPose.getZ()));
                }
            }.runTaskTimer(plugin, 0L, 1L);

            BukkitRunnable countdownTask = new BukkitRunnable() {
                private long remainingTicks = interval;

                @Override
                public void run() {
                    if (GameManager.getInstance().getCurrentStatus() != GameStatus.INGAME) {
                        cancel();
                        return;
                    }

                    remainingTicks--;
                    if (remainingTicks <= 0) {
                        remainingTicks = interval;
                    }

                    countdownStand.setCustomName("§e将在 §c" + (remainingTicks / 20) + "§e 秒后产出");
                }
            };
            countdownTask.runTaskTimer(plugin, 0L, 1L);
            countdownTasks.put(baseLoc, countdownTask);
        }
    }

    private void removeHolograms() {
        for (List<Entity> entities : hologramEntities.values()) {
            for (Entity entity : entities) {
                entity.remove();
            }
        }
        hologramEntities.clear();

        for (BukkitRunnable task : countdownTasks.values()) {
            task.cancel();
        }
        countdownTasks.clear();
    }

    private String getRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            default: return "I";
        }
    }

    public void upgrade() {
        level++;
        switch (level) {
            case 2:
                setSpawnInterval(900L); // 45 secs
                break;
            case 3:
                setSpawnInterval(600L); // 30 secs
                break;
        }

        for (List<Entity> entities : hologramEntities.values()) {
            for (Entity entity : entities) {
                if (entity instanceof ArmorStand) {
                    ArmorStand stand = (ArmorStand) entity;
                    if (stand.getCustomName() != null) {
                        if (stand.getCustomName().startsWith("§e等级")) {
                            stand.setCustomName("§e等级 " + getRomanNumeral(level));
                        } else if (stand.getCustomName().startsWith("§e将在")) {
                            stand.setCustomName("§e将在 §c" + (interval / 20) + "§e 秒后产出");
                        }
                    }
                }
            }
        }
    }
}

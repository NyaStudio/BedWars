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
    private static final int MAX_LEVEL = 3;
    private static final int CLEANUP_INTERVAL = 6000;
    private BukkitRunnable cleanupTask;

    private static class HologramText {
        final String prefix;
        final String content;
        final String suffix;

        HologramText(String prefix, String content, String suffix) {
            this.prefix = prefix;
            this.content = content;
            this.suffix = suffix;
        }

        String getFullText() {
            return prefix + content + suffix;
        }
    }

    private static class HologramConfig {
        final Material blockMaterial;
        final String resourceName;
        final String resourceColor;

        HologramConfig(Material material) {
            if (material == Material.DIAMOND) {
                this.blockMaterial = Material.DIAMOND_BLOCK;
                this.resourceName = "钻石";
                this.resourceColor = "§3";
            } else if (material == Material.EMERALD) {
                this.blockMaterial = Material.EMERALD_BLOCK;
                this.resourceName = "绿宝石";
                this.resourceColor = "§2";
            } else {
                throw new IllegalArgumentException("不支持的资源类型");
            }
        }
    }

    public ResourceSpawner(Main plugin, String type, long interval) {
        this.plugin = plugin;
        this.type = type;
        this.interval = plugin.getConfig().getLong("spawner.types." + type + ".interval", interval);
        this.mapSetup = new Map(plugin);
        startCleanupTask();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupResources();
            }
        };
        cleanupTask.runTaskTimer(plugin, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    private void cleanupResources() {
        activeDrops.entrySet().removeIf(entry -> {
            Item item = entry.getValue();
            return item == null || item.isDead() || !item.isValid();
        });

        hologramEntities.entrySet().removeIf(entry -> {
            List<Entity> entities = entry.getValue();
            if (entities == null) return true;
            entities.removeIf(entity -> entity == null || !entity.isValid());
            return entities.isEmpty();
        });

        countdownTasks.entrySet().removeIf(entry -> {
            BukkitRunnable task = entry.getValue();
            return task == null || task.isCancelled();
        });
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
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        removeHolograms();
        cleanupResources();
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
                
                List<Entity> hologramEntities = ResourceSpawner.this.hologramEntities.get(baseLoc);
                if (hologramEntities != null && hologramEntities.size() > 3) {
                    ArmorStand blockStand = (ArmorStand) hologramEntities.get(3);
                    if (blockStand != null && !blockStand.isDead()) {
                        center = blockStand.getLocation().clone();
                    }
                }
            }
        }

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

        Item item = world.dropItem(center, toDrop.clone());
        item.setVelocity(new Vector(0, -0.1, 0));
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

            Location center = findSpawnerCenter(baseLoc);
            Location hologramLoc = center.clone().add(0, 4.0, 0);
            List<Entity> entities = new ArrayList<>();

            try {
                HologramConfig config = new HologramConfig(getMaterial());
                createHologramEntities(world, hologramLoc, config, entities);
                startHologramAnimations(world, entities, config);
                hologramEntities.put(baseLoc, entities);
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
    }

    private Location findSpawnerCenter(Location baseLoc) {
        Location center = baseLoc.clone().add(0.5, 1.0, 0.5);
        Material mat = getMaterial();
        if (mat == Material.DIAMOND || mat == Material.EMERALD) {
            Material targetBlock = (mat == Material.DIAMOND) ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
            Location nearest = findNearestBlock(baseLoc, targetBlock, 3);
            if (nearest != null) {
                center = nearest.clone().add(0.5, 1.0, 0.5);
            }
        }
        return center;
    }

    private void createHologramEntities(World world, Location hologramLoc, HologramConfig config, List<Entity> entities) {
        entities.add(createTextStand(world, hologramLoc.clone().add(0, 0.8, 0), 
            new HologramText("§e等级 ", "§c" + getRomanNumeral(level), "")));

        entities.add(createTextStand(world, hologramLoc.clone().add(0, 0.5, 0),
            new HologramText(config.resourceColor, config.resourceName, "")));

        entities.add(createTextStand(world, hologramLoc.clone().add(0, 0.2, 0),
            new HologramText("§e将在 ", "§c" + (interval / 20), "§e 秒后产出")));

        ArmorStand blockStand = (ArmorStand) world.spawnEntity(hologramLoc.clone().add(0, -1.0, 0), EntityType.ARMOR_STAND);
        blockStand.setVisible(false);
        blockStand.setGravity(false);
        blockStand.setMarker(true);
        blockStand.setSmall(true);
        blockStand.setHelmet(new ItemStack(config.blockMaterial));
        blockStand.setHeadPose(new EulerAngle(0, 0, 0));
        entities.add(blockStand);
    }

    private ArmorStand createTextStand(World world, Location loc, HologramText text) {
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setCustomName(text.getFullText());
        stand.setCustomNameVisible(true);
        return stand;
    }

    private void startHologramAnimations(World world, List<Entity> entities, HologramConfig config) {
        ArmorStand blockStand = (ArmorStand) entities.get(3);
        Location baseLoc = blockStand.getLocation().clone();
        new BukkitRunnable() {
            double t = 0;

            @Override
            public void run() {
                if (GameManager.getInstance().getCurrentStatus() != GameStatus.INGAME) {
                    cancel();
                    return;
                }

                blockStand.setHeadPose(new EulerAngle(0, Math.toRadians(t), 0));

                double dy = Math.sin(Math.toRadians(t)) * 0.1;
                double dx = Math.cos(Math.toRadians(t)) * 0.05;
                double dz = Math.sin(Math.toRadians(t)) * 0.05;

                Location newLoc = baseLoc.clone().add(dx, dy, dz);
                blockStand.teleport(newLoc);

                t += 8;
                if (t >= 360) t = 0;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        ArmorStand countdownStand = (ArmorStand) entities.get(2);
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

                countdownStand.setCustomName(new HologramText("§e将在 ", "§c" + (remainingTicks / 20), "§e 秒后产出").getFullText());
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 1L);
        countdownTasks.put(blockStand.getLocation(), countdownTask);
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
        if (level >= MAX_LEVEL) {
            return;
        }
        level++;
        String upgradePath = "spawner.types." + type + ".upgrade.level" + level + ".interval";
        long newInterval = plugin.getConfig().getLong(upgradePath, interval);
        setSpawnInterval(newInterval);
        updateHologramDisplays();
    }

    private void updateHologramDisplays() {
        for (List<Entity> entities : hologramEntities.values()) {
            for (Entity entity : entities) {
                if (entity instanceof ArmorStand) {
                    ArmorStand stand = (ArmorStand) entity;
                    if (stand.getCustomName() != null) {
                        if (stand.getCustomName().startsWith("§e等级")) {
                            stand.setCustomName(new HologramText("§e等级 ", "§c" + getRomanNumeral(level), "").getFullText());
                        } else if (stand.getCustomName().startsWith("§e将在")) {
                            stand.setCustomName(new HologramText("§e将在 ", "§c" + (interval / 20), "§e 秒后产出").getFullText());
                        }
                    }
                }
            }
        }
    }
}

package cn.nekopixel.bedwars.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LocationUtils {
    public static Location findSafeLocation(Location baseLoc, int radius) {
        World world = baseLoc.getWorld();
        if (world == null) return baseLoc;

        Location center = new Location(world, 
            Math.floor(baseLoc.getX()) + 0.5,
            Math.floor(baseLoc.getY()),
            Math.floor(baseLoc.getZ()) + 0.5,
            baseLoc.getYaw(),
            baseLoc.getPitch()
        );
        
        if (isSafeLocation(center)) {
            return center;
        }

        for (int dy = 0; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Location check = center.clone().add(dx, dy, dz);
                    if (isSafeLocation(check)) {
                        return new Location(world,
                            Math.floor(check.getX()) + 0.5,
                            Math.floor(check.getY()),
                            Math.floor(check.getZ()) + 0.5,
                            check.getYaw(),
                            check.getPitch()
                        );
                    }
                }
            }
        }

        return center;
    }

    private static boolean isSafeLocation(Location loc) {
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().add(0, -1, 0).getBlock();

        return !isColliding(feet.getType()) &&
               !isColliding(head.getType()) &&
               ground.getType().isSolid();
    }

    private static boolean isColliding(Material material) {
        return material.isSolid() || material == Material.WATER || material == Material.LAVA;
    }

    public static Location findNearestBlock(Location origin, Material target, int radius) {
        World world = origin.getWorld();
        if (world == null) return null;

        Location closest = null;
        double closestDistance = Double.MAX_VALUE;

        int baseX = (int)Math.floor(origin.getX());
        int baseY = (int)Math.floor(origin.getY());
        int baseZ = (int)Math.floor(origin.getZ());

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

        if (closest != null) {
            return new Location(world,
                Math.floor(closest.getX()) + 0.5,
                Math.floor(closest.getY()),
                Math.floor(closest.getZ()) + 0.5,
                closest.getYaw(),
                closest.getPitch()
            );
        }

        return null;
    }
} 
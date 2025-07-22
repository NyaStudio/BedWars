package cn.nekopixel.bedwars.shop.utils;

import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.map.MapManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class PopTower {
    public static void createTower(Player player, Material material) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        int px = playerLoc.getBlockX();
        int py = playerLoc.getBlockY();
        int pz = playerLoc.getBlockZ();

        fillBlocks(world, px + 3, py + 6, pz - 1, px + 3, py + 6, pz + 1, material, player);
        setBlock(world, px + 3, py + 7, pz, material, player);
        fillBlocks(world, px - 3, py + 6, pz - 1, px - 3, py + 6, pz + 1, material, player);
        setBlock(world, px - 3, py + 7, pz, material, player);
        fillBlocks(world, px - 2, py + 6, pz - 2, px - 2, py + 7, pz - 2, material, player);
        fillBlocks(world, px - 2, py, pz - 1, px - 2, py + 1, pz + 1, material, player);
        fillBlocks(world, px - 2, py + 2, pz - 1, px + 2, py + 2, pz, material, player);
        fillBlocks(world, px - 2, py + 3, pz - 1, px - 2, py + 4, pz + 1, material, player);
        fillBlocks(world, px - 2, py + 5, pz - 1, px + 2, py + 5, pz, material, player);
        fillBlocks(world, px - 2, py + 2, pz + 1, px - 1, py + 2, pz + 1, material, player);
        fillBlocks(world, px - 2, py + 5, pz + 1, px - 1, py + 5, pz + 1, material, player);
        fillBlocks(world, px - 2, py + 6, pz + 2, px - 2, py + 7, pz + 2, material, player);
        fillBlocks(world, px - 1, py, pz - 2, px - 1, py + 1, pz - 2, material, player);
        fillBlocks(world, px - 1, py + 2, pz - 2, px + 1, py + 5, pz - 2, material, player);
        fillBlocks(world, px - 1, py, pz + 2, px + 1, py + 5, pz + 2, material, player);
        fillBlocks(world, px - 1, py + 6, pz + 3, px + 1, py + 6, pz + 3, material, player);
        setBlock(world, px, py + 7, pz + 3, material, player);
        fillBlocks(world, px + 1, py, pz - 2, px + 1, py + 1, pz - 2, material, player);
        fillBlocks(world, px + 1, py + 2, pz + 1, px + 2, py + 2, pz + 1, material, player);
        fillBlocks(world, px + 1, py + 5, pz + 1, px + 2, py + 5, pz + 1, material, player);
        fillBlocks(world, px + 2, py, pz - 1, px + 2, py + 1, pz + 1, material, player);
        fillBlocks(world, px + 2, py + 3, pz - 1, px + 2, py + 4, pz + 1, material, player);
        fillBlocks(world, px + 2, py + 6, pz + 2, px + 2, py + 7, pz + 2, material, player);
        fillBlocks(world, px + 2, py + 6, pz - 2, px + 2, py + 7, pz - 2, material, player);
        fillBlocks(world, px - 1, py + 6, pz - 3, px + 1, py + 6, pz - 3, material, player);
        setBlock(world, px, py + 7, pz - 3, material, player);
        placeLadder(world, px, py, pz, player);
    }

    private static void placeLadder(World world, int x, int y, int z, Player player) {
        MapManager mapManager = Plugin.getInstance().getMapManager();
        for (int i = 0; i <= 5; i++) {
            Block block = world.getBlockAt(x, y + i, z + 1);
            block.setType(Material.LADDER);
            org.bukkit.block.data.type.Ladder ladder = (org.bukkit.block.data.type.Ladder) block.getBlockData();
            ladder.setFacing(BlockFace.NORTH);
            block.setBlockData(ladder);
            mapManager.markAsPlayerPlaced(block);
        }
    }

    private static void fillBlocks(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material, Player player) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(world, x, y, z, material, player);
                }
            }
        }
    }
    
    private static void setBlock(World world, int x, int y, int z, Material material, Player player) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material);
        Plugin.getInstance().getMapManager().markAsPlayerPlaced(block);
    }
}
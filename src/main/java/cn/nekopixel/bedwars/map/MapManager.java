package cn.nekopixel.bedwars.map;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;

public class MapManager implements Listener {
    private final Main plugin;
    private final List<ProtectedArea> protectedAreas;
    private boolean enabled;

    public MapManager(Main plugin) {
        this.plugin = plugin;
        this.protectedAreas = new ArrayList<>();
        this.enabled = false;
    }

    public void loadProtectedAreas() {
        protectedAreas.clear();
        FileConfiguration config = Plugin.getInstance().getMapConfig();
        
        ConfigurationSection areasSection = config.getConfigurationSection("protection");
        if (areasSection == null) {
            plugin.getLogger().info("Protected area configuration not found, skipping loading");
            return;
        }

        for (String areaName : areasSection.getKeys(false)) {
            ConfigurationSection areaSection = areasSection.getConfigurationSection(areaName);
            if (areaSection == null) continue;

            try {
                String worldName = areaSection.getString("world", "world");
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World '" + worldName + "' does not exist, skipping protected area: " + areaName);
                    continue;
                }

                // pos1
                double x1 = areaSection.getDouble("pos1.x");
                double y1 = areaSection.getDouble("pos1.y");
                double z1 = areaSection.getDouble("pos1.z");

                // pos2
                double x2 = areaSection.getDouble("pos2.x");
                double y2 = areaSection.getDouble("pos2.y");
                double z2 = areaSection.getDouble("pos2.z");

                Location pos1 = new Location(world, x1, y1, z1);
                Location pos2 = new Location(world, x2, y2, z2);

                ProtectedArea area = new ProtectedArea(areaName, pos1, pos2, true);
                protectedAreas.add(area);

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading protected area '" + areaName + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + protectedAreas.size() + " protected areas in total");
    }

    public boolean isPlaceProtected(Location location) {
        if (!enabled) return false;
        
        for (ProtectedArea area : protectedAreas) {
            if (area.contains(location) && area.isPreventPlace()) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isPlayerPlaced(Block block) {
        return block.hasMetadata("player_placed");
    }

    public void markAsPlayerPlaced(Block block) {
        block.setMetadata("player_placed", new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
//        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
//            event.setCancelled(true);
//            Block block = event.getBlock();
//            Player player = event.getPlayer();
//            player.sendBlockChange(block.getLocation(), block.getBlockData());
//            return;
//        }
        
        if (!enabled) return;
        
        Block block = event.getBlock();
        
        if (block.getType().name().contains("BED")) {
            event.setCancelled(false);
            event.setDropItems(false);
            return;
        }
        
        if (!isPlayerPlaced(block)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(block.getLocation(), block.getBlockData());
            
            player.sendMessage("§c你不能在这里破坏方块！");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
//        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
//            event.setCancelled(true);
//            event.getPlayer().sendMessage("§c游戏已结束，无法放置方块！");
//            return;
//        }
        
        if (!enabled) return;
        
        Location location = event.getBlock().getLocation();
        
        if (isPlaceProtected(location)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c你不能在这里放置方块！");
            return;
        }
        
        markAsPlayerPlaced(event.getBlock());
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.INGAME) {
            this.enabled = true;
        } else {
            this.enabled = false;
        }
    }

    public List<ProtectedArea> getProtectedAreas() {
        return new ArrayList<>(protectedAreas);
    }

    public boolean isEnabled() {
        return enabled;
    }
} 
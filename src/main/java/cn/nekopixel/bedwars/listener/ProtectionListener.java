package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.map.MapManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.Iterator;

public class ProtectionListener implements Listener {
    private final Main plugin;
    private final MapManager mapManager;

    public ProtectionListener(Main plugin) {
        this.plugin = plugin;
        this.mapManager = Plugin.getInstance().getMapManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!mapManager.isEnabled()) return;
        
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (mapManager.isPlaceProtected(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c你不能在这里放置液体！");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c你不能在这里收集液体！");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (!mapManager.isEnabled()) return;
        
        Block toBlock = event.getToBlock();
        if (mapManager.isPlaceProtected(toBlock.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!mapManager.isEnabled()) return;
        
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (mapManager.isPlaceProtected(block.getLocation())) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!mapManager.isEnabled()) return;
        
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (mapManager.isPlaceProtected(block.getLocation())) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!mapManager.isEnabled()) return;
        
        for (Block block : event.getBlocks()) {
            Block nextBlock = block.getRelative(event.getDirection());
            if (mapManager.isPlaceProtected(nextBlock.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!mapManager.isEnabled()) return;
        
        for (Block block : event.getBlocks()) {
            if (mapManager.isPlaceProtected(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockForm(BlockFormEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockGrow(BlockGrowEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!mapManager.isEnabled()) return;
        
        if (mapManager.isPlaceProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
} 
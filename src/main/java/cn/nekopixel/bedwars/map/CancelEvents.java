package cn.nekopixel.bedwars.map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class CancelEvents implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null) return;

        Block clicked = event.getClickedBlock();
        Material type = clicked.getType();

        if (type.name().endsWith("_BED")) {
            event.setCancelled(true);
        }
    }
}

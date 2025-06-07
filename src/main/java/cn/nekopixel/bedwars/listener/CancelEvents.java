package cn.nekopixel.bedwars.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public class CancelEvents implements PacketListener, Listener {

    public CancelEvents() {
        super();
    }

    // cancel bed events
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null) return;

        Block clicked = event.getClickedBlock();
        Material type = clicked.getType();

        if (type.name().endsWith("_BED")) {
            event.setCancelled(true);
        }
    }

    // cancel advancements
    // 哎呦我又会了 吗?
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().toString().contains("ADVANCEMENT")) {
            event.setCancelled(true);
        }
    }
}
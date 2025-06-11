package cn.nekopixel.bedwars.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;

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
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        // event.getPlayer().sendMessage("Cancelled 1 Packet");
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().toString().contains("ADVANCEMENT")) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType().toString().contains("ADVANCEMENT")) {
            event.setCancelled(true);
        }
    }

    // 雪豹闭嘴
    @EventHandler
    public void onVillagerSound(org.bukkit.event.entity.EntitySpawnEvent event) {
        if (event.getEntity() instanceof Villager villager) {
            villager.setSilent(true);
        }
    }
}
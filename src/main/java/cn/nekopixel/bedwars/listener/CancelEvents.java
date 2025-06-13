package cn.nekopixel.bedwars.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public class CancelEvents implements Listener {
    private final Plugin plugin;
    private final ProtocolManager protocolManager;

    public CancelEvents(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        advancementCanceller();
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
    // idk, maybe i cant fix this
    private void advancementCanceller() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.ADVANCEMENTS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                event.setCancelled(true);
            }
        });
    }

    // 雪豹闭嘴
    @EventHandler
    public void onVillagerSound(org.bukkit.event.entity.EntitySpawnEvent event) {
        if (event.getEntity() instanceof Villager villager) {
            villager.setSilent(true);
        }
    }
}
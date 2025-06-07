package cn.nekopixel.bedwars.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;

public class CancelEvents implements Listener {

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
    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        AdvancementProgress progress = event.getPlayer().getAdvancementProgress(advancement);
        for (String criterion : progress.getAwardedCriteria()) {
            progress.revokeCriteria(criterion);
        }
    }
}

package cn.nekopixel.bedwars.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.advancement.Advancement;
import org.bukkit.Bukkit;

public class CancelEvents implements Listener {
    private final Plugin plugin;

    public CancelEvents(Plugin plugin) {
        this.plugin = plugin;
        checkWorlds();
    }

    // cancel you can sleep
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null) return;

        Block clicked = event.getClickedBlock();
        Material type = clicked.getType();
        if (type.name().endsWith("_BED") && event.getAction().toString().contains("RIGHT")) {
            Player player = event.getPlayer();
            if (player.isSneaking() && player.getInventory().getItemInMainHand().getType().isBlock()) {
                return;
            }
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

    // cancel advancements
    // so im a baka, i fixed that
    private void checkWorlds() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (Boolean.TRUE.equals(world.getGameRuleValue(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS))) {
                world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        int playerLevel = player.getLevel();
        float playerExp = player.getExp();
        Advancement advancement = event.getAdvancement();
        
        if (!advancement.getKey().getNamespace().contains("minecraft")) return;
        
        for (String criteria : advancement.getCriteria()) {
            player.getAdvancementProgress(advancement).revokeCriteria(criteria);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int playerNewLevel = player.getLevel();
            float playerNewExp = player.getExp();
            if (playerNewExp != playerExp || playerNewLevel != playerLevel) {
                player.setLevel(playerLevel);
                player.setExp(playerExp);
            }
        }, 2L);
    }
}
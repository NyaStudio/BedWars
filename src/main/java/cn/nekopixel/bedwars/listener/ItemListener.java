package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.shop.NamespacedKeys;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.shop.utils.PopTower;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> lastUsage = new HashMap<>();
    private static final long COOLDOWN_MS = 100;
    
    public ItemListener(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        if (container.has(NamespacedKeys.getInstance().getPopTowerKey(), PersistentDataType.BYTE)) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastUsage.get(playerId);
            
            if (lastTime != null && (currentTime - lastTime) < COOLDOWN_MS) {
                event.setCancelled(true);
                return;
            }
            
            lastUsage.put(playerId, currentTime);
            event.setCancelled(true);

            Material woolMaterial = Material.WHITE_WOOL;
            TeamManager teamManager = Plugin.getInstance().getGameManager().getTeamManager();
            String team = teamManager.getPlayerTeam(player);
            if (team != null) {
                woolMaterial = getTeamWool(team);
            }
            
            PopTower.createTower(player, woolMaterial);
            
            ItemStack currentItem = player.getInventory().getItemInMainHand();
            if (currentItem.getAmount() > 1) {
                currentItem.setAmount(currentItem.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }
    
    private Material getTeamWool(String team) {
        return switch (team.toLowerCase()) {
            case "red" -> Material.RED_WOOL;
            case "blue" -> Material.BLUE_WOOL;
            case "green" -> Material.LIME_WOOL;
            case "yellow" -> Material.YELLOW_WOOL;
            case "aqua" -> Material.LIGHT_BLUE_WOOL;
            case "white" -> Material.WHITE_WOOL;
            case "pink" -> Material.PINK_WOOL;
            case "gray" -> Material.GRAY_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }
}
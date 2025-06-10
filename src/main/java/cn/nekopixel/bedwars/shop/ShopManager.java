package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.spawner.NPCManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopManager implements Listener {
    private final Main plugin;
    private final NPCManager npcManager;
    private final ItemShop itemShop;
    private final UpgradeShop upgradeShop;
    private final Map<UUID, Long> lastPurchaseTime = new HashMap<>();
    private static final long PURCHASE_COOLDOWN = 150;

    public ShopManager(Main plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.itemShop = new ItemShop(plugin);
        this.upgradeShop = new UpgradeShop(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();

        if (npcManager.isShopNPC(villager)) {
            event.setCancelled(true);
            itemShop.openShop(player);
        } else if (npcManager.isUpgradeNPC(villager)) {
            event.setCancelled(true);
            upgradeShop.openShop(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.contains("商店")) return;

        event.setCancelled(true);
        player.setItemOnCursor(null);

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory == null || !clickedInventory.equals(topInventory)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(itemShop.getShopItemKey(), PersistentDataType.BYTE) && 
            !data.has(upgradeShop.getShopItemKey(), PersistentDataType.BYTE)) return;

        // 防止买两次
        long currentTime = System.currentTimeMillis();
        Long lastPurchase = lastPurchaseTime.get(player.getUniqueId());
        if (lastPurchase != null && currentTime - lastPurchase < PURCHASE_COOLDOWN) {
            return;
        }
        // 更新购买时间
        lastPurchaseTime.put(player.getUniqueId(), currentTime);

        int price = data.getOrDefault(itemShop.getPriceKey(), PersistentDataType.INTEGER, 0);
        String currency = data.getOrDefault(itemShop.getCurrencyKey(), PersistentDataType.STRING, "iron");

        Material costMaterial = switch (currency.toLowerCase()) {
            case "iron" -> Material.IRON_INGOT;
            case "gold" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            default -> null;
        };

        if (costMaterial == null) {
            player.sendMessage("§c未知的货币类型: " + currency);
            return;
        }

        int playerAmount = countMaterial(player, costMaterial);
        if (playerAmount < price) {
            player.sendMessage("§c你没有足够的 " + translateCurrency(currency) + "！");
            return;
        }

        removeMaterial(player, costMaterial, price);
        ItemStack reward = new ItemStack(clickedItem.getType());
        reward.setAmount(1);

        player.getInventory().addItem(reward);
        player.sendMessage("§a购买成功: §f" + meta.getDisplayName() + " §7（花费 " + price + " " + translateCurrency(currency) + "）");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().contains("商店")) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.setItemOnCursor(null);
            }
        }
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != material) continue;

            int amt = item.getAmount();
            if (amt >= amount) {
                item.setAmount(amt - amount);
                return;
            } else {
                player.getInventory().clear(i);
                amount -= amt;
            }
        }
    }

    private String translateCurrency(String currency) {
        return switch (currency.toLowerCase()) {
            case "iron" -> "铁锭";
            case "gold" -> "金锭";
            case "diamond" -> "钻石";
            case "emerald" -> "绿宝石";
            default -> currency;
        };
    }
}

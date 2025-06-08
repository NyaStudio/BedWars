package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.spawner.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import java.util.*;

public class ShopManager implements Listener {
    private final Main plugin;
    private final NPCManager npcManager;
    private final Map<String, String> shopTitles;
    private final Map<String, Inventory> shopInventories;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey currencyKey;
    private final Map<UUID, Long> lastPurchaseTime = new HashMap<>();
    private static final long PURCHASE_COOLDOWN = 150;

    public ShopManager(Main plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.shopInventories = new HashMap<>();
        this.shopTitles = new HashMap<>();
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.currencyKey = new NamespacedKey(plugin, "shop_currency");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initializeShops();
    }

    private void initializeShops() {
        String itemShopTitle = "§b道具商店";
        String upgradeShopTitle = "§b队伍升级";
        
        Inventory itemShop = Bukkit.createInventory(null, 54, itemShopTitle);
        setupItemShop(itemShop);
        shopInventories.put("item_shop", itemShop);
        shopTitles.put("item_shop", itemShopTitle);

        Inventory upgradeShop = Bukkit.createInventory(null, 54, upgradeShopTitle);
        setupUpgradeShop(upgradeShop);
        shopInventories.put("upgrade_shop", upgradeShop);
        shopTitles.put("upgrade_shop", upgradeShopTitle);
    }

    private void setupItemShop(Inventory inventory) {
        inventory.setItem(0, createShopItem(Material.WHITE_WOOL, "§f羊毛", 4, "iron"));
        inventory.setItem(1, createShopItem(Material.STONE_SWORD, "§f石剑", 10, "iron"));
    }

    private void setupUpgradeShop(Inventory inventory) {
        inventory.setItem(0, createShopItem(Material.DIAMOND_SWORD, "§b锋利", 4, "diamond"));
        inventory.setItem(1, createShopItem(Material.IRON_CHESTPLATE, "§b防护", 4, "diamond"));
    }

    private ItemStack createShopItem(Material material, String name, int price, String currency) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7价格: §e" + price + " " + translateCurrency(currency));
        meta.setLore(lore);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(shopItemKey, PersistentDataType.BYTE, (byte) 1);
        data.set(priceKey, PersistentDataType.INTEGER, price);
        data.set(currencyKey, PersistentDataType.STRING, currency);

        item.setItemMeta(meta);
        return item;
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();

        if (npcManager.isShopNPC(villager)) {
            event.setCancelled(true);
            openShop(player, "item_shop");
        } else if (npcManager.isUpgradeNPC(villager)) {
            event.setCancelled(true);
            openShop(player, "upgrade_shop");
        }
    }

    private void openShop(Player player, String shopName) {
        Inventory template = shopInventories.get(shopName);
        if (template == null) return;

        String title = shopTitles.get(shopName);
        if (title == null) return;

        Inventory copy = Bukkit.createInventory(null, template.getSize(), title);
        for (int i = 0; i < template.getSize(); i++) {
            ItemStack item = template.getItem(i);
            copy.setItem(i, item == null ? null : item.clone());
        }

        player.openInventory(copy);
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
        if (!data.has(shopItemKey, PersistentDataType.BYTE)) return;

        // 防止买两次
        long currentTime = System.currentTimeMillis();
        Long lastPurchase = lastPurchaseTime.get(player.getUniqueId());
        if (lastPurchase != null && currentTime - lastPurchase < PURCHASE_COOLDOWN) {
            return;
        }
        // 更新购买时间
        lastPurchaseTime.put(player.getUniqueId(), currentTime);

        int price = data.getOrDefault(priceKey, PersistentDataType.INTEGER, 0);
        String currency = data.getOrDefault(currencyKey, PersistentDataType.STRING, "iron");

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
}

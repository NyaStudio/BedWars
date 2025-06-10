package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class ItemShop {
    private final Main plugin;
    private final Inventory inventory;
    private final String title;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey currencyKey;
    private final NamespacedKey shopTypeKey;

    public ItemShop(Main plugin) {
        this.plugin = plugin;
        this.title = "§b道具商店";
        this.inventory = Bukkit.createInventory(null, 54, title);
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.currencyKey = new NamespacedKey(plugin, "shop_currency");
        this.shopTypeKey = new NamespacedKey(plugin, "shop_type");
    }

    public void setupShop(Map<String, ShopItem> items) {
        for (ShopItem item : items.values()) {
            String[] typeParts = item.getType().split(":");
            if (typeParts.length != 2) continue;

            Material material = Material.getMaterial(typeParts[1].toUpperCase());
            if (material == null) continue;

            ItemStack shopItem = ((ShopManager) plugin.getShopManager())
                .createShopItem(material, item, shopItemKey, priceKey, currencyKey, shopTypeKey, "item_shop");
            inventory.setItem(item.getIndex(), shopItem);
        }
    }

    public void openShop(Player player) {
        Inventory copy = Bukkit.createInventory(null, inventory.getSize(), title);
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            copy.setItem(i, item == null ? null : item.clone());
        }
        player.openInventory(copy);
    }

    public boolean isShopItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(shopItemKey, PersistentDataType.BYTE) && 
               data.has(shopTypeKey, PersistentDataType.STRING) &&
               "item_shop".equals(data.get(shopTypeKey, PersistentDataType.STRING));
    }

    public boolean isShopInventory(String inventoryTitle) {
        return inventoryTitle.equals(title);
    }

    public NamespacedKey getShopItemKey() {
        return shopItemKey;
    }

    public NamespacedKey getPriceKey() {
        return priceKey;
    }

    public NamespacedKey getCurrencyKey() {
        return currencyKey;
    }

    public NamespacedKey getShopTypeKey() {
        return shopTypeKey;
    }
} 
package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class ItemShop {
    private final Main plugin;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey currencyKey;
    private final NamespacedKey shopTypeKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey separatorKey;
    private final Map<String, ShopItem> items = new HashMap<>();
    private final ItemSort itemSort;

    public ItemShop(Main plugin) {
        this.plugin = plugin;
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.priceKey = new NamespacedKey(plugin, "price");
        this.currencyKey = new NamespacedKey(plugin, "currency");
        this.shopTypeKey = new NamespacedKey(plugin, "shop_type");
        this.categoryKey = new NamespacedKey(plugin, "category");
        this.separatorKey = new NamespacedKey(plugin, "separator");
        this.itemSort = ItemSort.getInstance();
    }

    public void setupShop(Map<String, ShopItem> items) {
        this.items.clear();
        this.items.putAll(items);
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8物品商店");
        
        // 分类按钮
        int categoryIndex = 0;
        for (ItemSort.SortCategory category : itemSort.getCategories().values()) {
            if (categoryIndex < 9) {
                boolean isSelected = category.getId().equals(itemSort.getCurrentCategory());
                plugin.getLogger().info("创建分类按钮: " + category.getId() + ", 是否选中: " + isSelected);
                ItemStack categoryItem = itemSort.createCategoryItem(category, isSelected);
                ItemMeta meta = categoryItem.getItemMeta();
                meta.getPersistentDataContainer()
                    .set(categoryKey, PersistentDataType.STRING, category.getId());
                categoryItem.setItemMeta(meta);
                inv.setItem(categoryIndex, categoryItem);
                categoryIndex++;
            }
        }

        // 分隔线
        String currentCategory = itemSort.getCurrentCategory();
        plugin.getLogger().info("当前分类: " + currentCategory);
        ItemStack separator = currentCategory != null ? 
            itemSort.createSelectedSeparator() : itemSort.createSeparator();
        ItemMeta meta = separator.getItemMeta();
        meta.getPersistentDataContainer().set(separatorKey, PersistentDataType.BYTE, (byte) 1);
        meta.setDisplayName("§r");
        separator.setItemMeta(meta);
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, separator.clone());
        }

        // 物品
        if (currentCategory != null) {
            for (ShopItem item : items.values()) {
                plugin.getLogger().info("检查物品: " + item.getName() + ", 分类: " + item.getCategory() + ", 当前分类: " + currentCategory);
                if (item.getCategory().equals(currentCategory)) {
                    String type = item.getType();
                    Material material;
                    
                    try {
                        if (type.startsWith("minecraft:potion{")) {
                            material = Material.POTION;
                        } else if (type.startsWith("minecraft:")) {
                            String materialName = type.substring(10).toUpperCase();
                            material = Material.valueOf(materialName);
                        } else {
                            material = Material.valueOf(type.toUpperCase());
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的物品类型: " + type + "，跳过此物品");
                        continue;
                    }

                    ItemStack shopItem = plugin.getShopManager().createShopItem(
                        material,
                        item,
                        shopItemKey,
                        priceKey,
                        currencyKey,
                        shopTypeKey,
                        "item"
                    );
                    inv.setItem(item.getSlot(), shopItem);
                }
            }
        }

        player.openInventory(inv);
    }

    public boolean isShopItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(shopItemKey, PersistentDataType.BYTE);
    }

    public boolean isCategoryItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(categoryKey, PersistentDataType.STRING);
    }

    public boolean isSeparator(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(separatorKey, PersistentDataType.BYTE);
    }

    public String getCategoryFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
    }

    public NamespacedKey getPriceKey() {
        return priceKey;
    }

    public NamespacedKey getCurrencyKey() {
        return currencyKey;
    }
} 
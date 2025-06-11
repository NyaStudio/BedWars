package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
        Map<String, ItemSort.SortCategory> categories = itemSort.getCategories();
        if (!categories.isEmpty()) {
            String firstCategory = categories.keySet().iterator().next();
            itemSort.setCurrentCategory(firstCategory);
        }

        Inventory inv = Bukkit.createInventory(null, 54, "§8物品商店");
        
        int categoryIndex = 0;
        int selectedIndex = -1;
        for (ItemSort.SortCategory category : itemSort.getCategories().values()) {
            if (categoryIndex < 9) {
                boolean isSelected = category.getId().equals(itemSort.getCurrentCategory());
                if (isSelected) {
                    selectedIndex = categoryIndex;
                }
                ItemStack categoryItem = itemSort.createCategoryItem(category, isSelected);
                ItemMeta meta = categoryItem.getItemMeta();
                meta.getPersistentDataContainer()
                    .set(categoryKey, PersistentDataType.STRING, category.getId());
                categoryItem.setItemMeta(meta);
                inv.setItem(categoryIndex, categoryItem);
                categoryIndex++;
            }
        }

        String currentCategory = itemSort.getCurrentCategory();
        
        ItemStack graySeparator = itemSort.createSeparator();
        ItemMeta grayMeta = graySeparator.getItemMeta();
        grayMeta.getPersistentDataContainer().set(separatorKey, PersistentDataType.BYTE, (byte) 1);
        grayMeta.setDisplayName("§r");
        graySeparator.setItemMeta(grayMeta);
        
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, graySeparator.clone());
        }
        
        if (selectedIndex != -1) {
            ItemStack greenSeparator = itemSort.createSelectedSeparator();
            ItemMeta greenMeta = greenSeparator.getItemMeta();
            greenMeta.getPersistentDataContainer().set(separatorKey, PersistentDataType.BYTE, (byte) 1);
            greenMeta.setDisplayName("§r");
            greenSeparator.setItemMeta(greenMeta);
            inv.setItem(selectedIndex + 9, greenSeparator);
        }

        for (int i = 18; i < 54; i++) {
            inv.setItem(i, null);
        }

        if (currentCategory != null) {
            Map<String, ShopItem> itemsToShow = currentCategory.equals("quick_buy") ? 
                plugin.getShopManager().getQuickBuyItems() : items;

            for (ShopItem item : itemsToShow.values()) {
                if (currentCategory.equals("quick_buy")) {
                    if (!item.getCategory().equals("quick_buy")) continue;
                } else {
                    if (!item.getCategory().equals(currentCategory)) continue;
                }

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

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§8物品商店")) return;
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

    public NamespacedKey getShopItemKey() {
        return shopItemKey;
    }

    public NamespacedKey getShopTypeKey() {
        return shopTypeKey;
    }

    public void updateInventory(Inventory inv, Player player) {
        int categoryIndex = 0;
        int selectedIndex = -1;
        for (ItemSort.SortCategory category : itemSort.getCategories().values()) {
            if (categoryIndex < 9) {
                boolean isSelected = category.getId().equals(itemSort.getCurrentCategory());
                if (isSelected) {
                    selectedIndex = categoryIndex;
                }
                ItemStack categoryItem = itemSort.createCategoryItem(category, isSelected);
                ItemMeta meta = categoryItem.getItemMeta();
                meta.getPersistentDataContainer()
                    .set(categoryKey, PersistentDataType.STRING, category.getId());
                categoryItem.setItemMeta(meta);
                inv.setItem(categoryIndex, categoryItem);
                categoryIndex++;
            }
        }

        String currentCategory = itemSort.getCurrentCategory();
        
        ItemStack graySeparator = itemSort.createSeparator();
        ItemMeta grayMeta = graySeparator.getItemMeta();
        grayMeta.getPersistentDataContainer().set(separatorKey, PersistentDataType.BYTE, (byte) 1);
        grayMeta.setDisplayName("§r");
        graySeparator.setItemMeta(grayMeta);
        
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, graySeparator.clone());
        }
        
        if (selectedIndex != -1) {
            ItemStack greenSeparator = itemSort.createSelectedSeparator();
            ItemMeta greenMeta = greenSeparator.getItemMeta();
            greenMeta.getPersistentDataContainer().set(separatorKey, PersistentDataType.BYTE, (byte) 1);
            greenMeta.setDisplayName("§r");
            greenSeparator.setItemMeta(greenMeta);
            inv.setItem(selectedIndex + 9, greenSeparator);
        }

        for (int i = 18; i < 54; i++) {
            inv.setItem(i, null);
        }

        if (currentCategory != null) {
            Map<String, ShopItem> itemsToShow = currentCategory.equals("quick_buy") ? 
                plugin.getShopManager().getQuickBuyItems() : items;

            for (ShopItem item : itemsToShow.values()) {
                if (currentCategory.equals("quick_buy")) {
                    if (!item.getCategory().equals("quick_buy")) continue;
                } else {
                    if (!item.getCategory().equals(currentCategory)) continue;
                }

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
} 
package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private final Map<String, ShopItem> items = new HashMap<>();
    private final ItemCategory itemCategory;

    public ItemShop(Main plugin) {
        this.plugin = plugin;
        this.itemCategory = ItemCategory.getInstance();
    }

    public void setupShop(Map<String, ShopItem> items) {
        this.items.clear();
        this.items.putAll(items);
    }

    public void openShop(Player player) {
        Map<String, ItemCategory.SortCategory> categories = itemCategory.getCategories();
        if (!categories.isEmpty()) {
            String firstCategory = categories.keySet().iterator().next();
            itemCategory.setCurrentCategory(firstCategory);
        }

        Inventory inv = Bukkit.createInventory(null, 54, "§8物品商店");
        
        int categoryIndex = 0;
        int selectedIndex = -1;
        for (ItemCategory.SortCategory category : itemCategory.getCategories().values()) {
            if (categoryIndex < 9) {
                boolean isSelected = category.getId().equals(itemCategory.getCurrentCategory());
                if (isSelected) {
                    selectedIndex = categoryIndex;
                }
                ItemStack categoryItem = itemCategory.createCategoryItem(category, isSelected);
                ItemMeta meta = categoryItem.getItemMeta();
                meta.getPersistentDataContainer()
                    .set(NamespacedKeys.getInstance().getCategoryKey(), PersistentDataType.STRING, category.getId());
                categoryItem.setItemMeta(meta);
                inv.setItem(categoryIndex, categoryItem);
                categoryIndex++;
            }
        }

        String currentCategory = itemCategory.getCurrentCategory();
        
        ItemStack graySeparator = itemCategory.createSeparator();
        ItemMeta grayMeta = graySeparator.getItemMeta();
        grayMeta.getPersistentDataContainer().set(NamespacedKeys.getInstance().getSeparatorKey(), PersistentDataType.BYTE, (byte) 1);
        grayMeta.setDisplayName("§r");
        graySeparator.setItemMeta(grayMeta);
        
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, graySeparator.clone());
        }
        
        if (selectedIndex != -1) {
            ItemStack greenSeparator = itemCategory.createSelectedSeparator();
            ItemMeta greenMeta = greenSeparator.getItemMeta();
            greenMeta.getPersistentDataContainer().set(NamespacedKeys.getInstance().getSeparatorKey(), PersistentDataType.BYTE, (byte) 1);
            greenMeta.setDisplayName("§r");
            greenSeparator.setItemMeta(greenMeta);
            inv.setItem(selectedIndex + 9, greenSeparator);
        }

        for (int i = 18; i < 54; i++) {
            inv.setItem(i, null);
        }

        if (currentCategory != null) {
            Map<String, ShopItem> itemsToShow = currentCategory.equals("quick_buy") ? 
                Plugin.getInstance().getShopManager().getQuickBuyItems() : items;

            for (ShopItem item : itemsToShow.values()) {
                if (currentCategory.equals("quick_buy")) {
                    if (!item.getCategory().equals("quick_buy")) continue;
                } else {
                    if (!item.getCategory().equals(currentCategory)) continue;
                }

                String type = item.getType();
                Material material;
                
                try {
                    if (type.equals("quick_buy:empty_slot")) {
                        material = Material.RED_STAINED_GLASS_PANE;
                    } else if (type.equals("pop_tower")) {
                        material = Material.TRAPPED_CHEST;
                    } else if (type.startsWith("nekopixel:")) {
                        String customType = type.substring(10);
                        switch (customType) {
                            case "pop_tower" -> material = Material.TRAPPED_CHEST;
                            default -> {
                                plugin.getLogger().warning("Unknown custom item type: " + customType);
                                material = Material.BARRIER;
                            }
                        }
                    } else if (type.startsWith("minecraft:potion{")) {
                        material = Material.POTION;
                    } else if (type.startsWith("minecraft:")) {
                        String materialName = type.substring(10).toUpperCase();
                        material = Material.valueOf(materialName);
                    } else {
                        try {
                            material = Material.valueOf(type.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Unknown item type: " + type);
                            material = Material.BARRIER;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Failed to parse item type: " + type + ", skipping");
                    continue;
                }

                ItemStack shopItem = Plugin.getInstance().getShopManager().createShopItem(
                    material,
                    item,
                    player
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
        return meta.getPersistentDataContainer().has(NamespacedKeys.getInstance().getShopItemKey(), PersistentDataType.BYTE);
    }

    public boolean isCategoryItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(NamespacedKeys.getInstance().getCategoryKey(), PersistentDataType.STRING);
    }

    public boolean isSeparator(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(NamespacedKeys.getInstance().getSeparatorKey(), PersistentDataType.BYTE);
    }

    public String getCategoryFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(NamespacedKeys.getInstance().getCategoryKey(), PersistentDataType.STRING);
    }

    public void updateInventory(Inventory inv, Player player) {
        int categoryIndex = 0;
        int selectedIndex = -1;
        for (ItemCategory.SortCategory category : itemCategory.getCategories().values()) {
            if (categoryIndex < 9) {
                boolean isSelected = category.getId().equals(itemCategory.getCurrentCategory());
                if (isSelected) {
                    selectedIndex = categoryIndex;
                }
                ItemStack categoryItem = itemCategory.createCategoryItem(category, isSelected);
                ItemMeta meta = categoryItem.getItemMeta();
                meta.getPersistentDataContainer()
                    .set(NamespacedKeys.getInstance().getCategoryKey(), PersistentDataType.STRING, category.getId());
                categoryItem.setItemMeta(meta);
                inv.setItem(categoryIndex, categoryItem);
                categoryIndex++;
            }
        }

        String currentCategory = itemCategory.getCurrentCategory();
        
        ItemStack graySeparator = itemCategory.createSeparator();
        ItemMeta grayMeta = graySeparator.getItemMeta();
        grayMeta.getPersistentDataContainer().set(NamespacedKeys.getInstance().getSeparatorKey(), PersistentDataType.BYTE, (byte) 1);
        grayMeta.setDisplayName("§r");
        graySeparator.setItemMeta(grayMeta);
        
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, graySeparator.clone());
        }
        
        if (selectedIndex != -1) {
            ItemStack greenSeparator = itemCategory.createSelectedSeparator();
            ItemMeta greenMeta = greenSeparator.getItemMeta();
            greenMeta.getPersistentDataContainer().set(NamespacedKeys.getInstance().getSeparatorKey(), PersistentDataType.BYTE, (byte) 1);
            greenMeta.setDisplayName("§r");
            greenSeparator.setItemMeta(greenMeta);
            inv.setItem(selectedIndex + 9, greenSeparator);
        }

        for (int i = 18; i < 54; i++) {
            inv.setItem(i, null);
        }

        if (currentCategory != null) {
            Map<String, ShopItem> itemsToShow = currentCategory.equals("quick_buy") ? 
                Plugin.getInstance().getShopManager().getQuickBuyItems() : items;

            for (ShopItem item : itemsToShow.values()) {
                if (currentCategory.equals("quick_buy")) {
                    if (!item.getCategory().equals("quick_buy")) continue;
                } else {
                    if (!item.getCategory().equals(currentCategory)) continue;
                }

                String type = item.getType();
                Material material;
                
                try {
                    if (type.equals("quick_buy:empty_slot")) {
                        material = Material.RED_STAINED_GLASS_PANE;
                    } else if (type.equals("pop_tower")) {
                        material = Material.TRAPPED_CHEST;
                    } else if (type.startsWith("nekopixel:")) {
                        String customType = type.substring(10);
                        switch (customType) {
                            case "pop_tower" -> material = Material.TRAPPED_CHEST;
                            default -> {
                                plugin.getLogger().warning("Unknown custom item type: " + customType);
                                material = Material.BARRIER;
                            }
                        }
                    } else if (type.startsWith("minecraft:potion{")) {
                        material = Material.POTION;
                    } else if (type.startsWith("minecraft:")) {
                        String materialName = type.substring(10).toUpperCase();
                        material = Material.valueOf(materialName);
                    } else {
                        try {
                            material = Material.valueOf(type.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Unknown item type: " + type);
                            material = Material.BARRIER;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Failed to parse item type: " + type + ", skipping");
                    continue;
                }

                ItemStack shopItem = Plugin.getInstance().getShopManager().createShopItem(
                    material,
                    item,
                    player
                );
                inv.setItem(item.getSlot(), shopItem);
            }
        }
    }
} 
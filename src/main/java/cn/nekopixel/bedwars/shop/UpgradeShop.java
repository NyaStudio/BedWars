package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class UpgradeShop {
    private final Main plugin;
    private final Inventory inventory;
    private final String title;

    public UpgradeShop(Main plugin) {
        this.plugin = plugin;
        this.title = LanguageManager.getInstance().getMessage("shop.upgrade.title");
        this.inventory = Bukkit.createInventory(null, 54, title);
        setupShop();
    }

    private void setupShop() {
        inventory.setItem(0, createShopItem(Material.DIAMOND_SWORD, LanguageManager.getInstance().getMessage("shop.upgrade.sharpness"), 4, "diamond"));
        inventory.setItem(1, createShopItem(Material.IRON_CHESTPLATE, LanguageManager.getInstance().getMessage("shop.upgrade.protection"), 4, "diamond"));
    }

    private ItemStack createShopItem(Material material, String name, int price, String currency) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add(LanguageManager.getInstance().getMessage("shop.upgrade.price", "price", String.valueOf(price), "currency", translateCurrency(currency)));
        meta.setLore(lore);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(NamespacedKeys.getInstance().getShopItemKey(), PersistentDataType.BYTE, (byte) 1);
        data.set(NamespacedKeys.getInstance().getPriceKey(), PersistentDataType.INTEGER, price);
        data.set(NamespacedKeys.getInstance().getCurrencyKey(), PersistentDataType.STRING, currency);
        data.set(NamespacedKeys.getInstance().getShopTypeKey(), PersistentDataType.STRING, "upgrade_shop");

        item.setItemMeta(meta);
        return item;
    }

    private String translateCurrency(String currency) {
        return switch (currency.toLowerCase()) {
            case "iron" -> LanguageManager.getInstance().getMessage("currency.iron");
            case "gold" -> LanguageManager.getInstance().getMessage("currency.gold");
            case "diamond" -> LanguageManager.getInstance().getMessage("currency.diamond");
            case "emerald" -> LanguageManager.getInstance().getMessage("currency.emerald");
            default -> currency;
        };
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
        return data.has(NamespacedKeys.getInstance().getShopItemKey(), PersistentDataType.BYTE) &&
               data.has(NamespacedKeys.getInstance().getShopTypeKey(), PersistentDataType.STRING) &&
               "upgrade_shop".equals(data.get(NamespacedKeys.getInstance().getShopTypeKey(), PersistentDataType.STRING));
    }
} 
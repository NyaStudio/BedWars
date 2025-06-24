package cn.nekopixel.bedwars.shop;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class NamespacedKeys {
    private static NamespacedKeys instance;
    
    private final NamespacedKey shopItemKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey currencyKey;
    private final NamespacedKey shopTypeKey;
    
    private final NamespacedKey categoryKey;
    private final NamespacedKey separatorKey;
    
    private NamespacedKeys(Plugin plugin) {
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.priceKey = new NamespacedKey(plugin, "price");
        this.currencyKey = new NamespacedKey(plugin, "currency");
        this.shopTypeKey = new NamespacedKey(plugin, "shop_type");
        this.categoryKey = new NamespacedKey(plugin, "category");
        this.separatorKey = new NamespacedKey(plugin, "separator");
    }
    
    public static void initialize(Plugin plugin) {
        if (instance == null) {
            instance = new NamespacedKeys(plugin);
        }
    }
    
    public static NamespacedKeys getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NamespacedKeys 未初始化");
        }
        return instance;
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
    
    public NamespacedKey getCategoryKey() {
        return categoryKey;
    }
    
    public NamespacedKey getSeparatorKey() {
        return separatorKey;
    }
} 
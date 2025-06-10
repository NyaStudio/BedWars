package cn.nekopixel.bedwars.shop;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class ShopItem {
    private final int index;
    private final String type;
    private final String name;
    private final List<String> lore;
    private final String pricingType;
    private final int pricing;
    private final ConfigurationSection enchantments;

    public ShopItem(int index, String type, String name, List<String> lore, 
                   String pricingType, int pricing, ConfigurationSection enchantments) {
        this.index = index;
        this.type = type;
        this.name = name;
        this.lore = lore;
        this.pricingType = pricingType;
        this.pricing = pricing;
        this.enchantments = enchantments;
    }

    public int getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getPricingType() {
        return pricingType;
    }

    public int getPricing() {
        return pricing;
    }

    public ConfigurationSection getEnchantments() {
        return enchantments;
    }
} 
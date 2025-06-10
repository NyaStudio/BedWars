package cn.nekopixel.bedwars.shop;

import java.util.List;
import java.util.Map;

public class ShopItem {
    private final int index;
    private final String type;
    private final String name;
    private final List<String> lore;
    private final String pricingType;
    private final int pricing;
    private final List<Map<String, Object>> enchantments;
    private final int potionLevel;
    private final int potionDuration;

    public ShopItem(int index, String type, String name, List<String> lore, 
                   String pricingType, int pricing, List<Map<String, Object>> enchantments,
                   int potionLevel, int potionDuration) {
        this.index = index;
        this.type = type;
        this.name = name;
        this.lore = lore;
        this.pricingType = pricingType;
        this.pricing = pricing;
        this.enchantments = enchantments;
        this.potionLevel = potionLevel;
        this.potionDuration = potionDuration;
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

    public List<Map<String, Object>> getEnchantments() {
        return enchantments;
    }

    public int getPotionLevel() {
        return potionLevel;
    }

    public int getPotionDuration() {
        return potionDuration;
    }
} 
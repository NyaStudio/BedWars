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
    private final int amount;
    private final String category;

    public ShopItem(int index, String type, String name, List<String> lore, 
                   String pricingType, int pricing, List<Map<String, Object>> enchantments,
                   int potionLevel, int potionDuration, int amount, String category) {
        this.index = index;
        this.type = type;
        this.name = name;
        this.lore = lore;
        this.pricingType = pricingType;
        this.pricing = pricing;
        this.enchantments = enchantments;
        this.potionLevel = potionLevel;
        this.potionDuration = potionDuration;
        this.amount = amount;
        this.category = category;
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

    public int getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public int getSlot() {
        return index + 18;   // 0 = 18，你们知道吗
    }
} 
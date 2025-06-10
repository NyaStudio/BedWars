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
    private final int row;    // 行号 (0-5)
    private final int column; // 列号 (0-8)

    public ShopItem(int index, String type, String name, List<String> lore, 
                   String pricingType, int pricing, List<Map<String, Object>> enchantments,
                   int potionLevel, int potionDuration, int amount, String category,
                   int row, int column) {
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
        this.row = Math.min(Math.max(row, 0), 5);    // 限制在 0-5
        this.column = Math.min(Math.max(column, 0), 8); // 限制在 0-8
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

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public int getSlot() {
        return (row + 2) * 9 + column; // 从第三行开始，所以是 row + 2
    }
} 
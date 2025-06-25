package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.utils.shop.PurchaseUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Placeholders {
    public static List<String> processPlaceholders(List<String> lore, ShopItem item, Player player, boolean canAfford) {
        List<String> processedLore = new ArrayList<>();
        String currencyName = PurchaseUtils.translateCurrency(item.getPricingType());
        
        for (String line : lore) {
            String processedLine = line;
            
            if (processedLine.contains("{purchase_status}")) {
                if (canAfford) {
                    processedLine = processedLine.replace("{purchase_status}", "§e点击购买！");
                } else {
                    processedLine = processedLine.replace("{purchase_status}", "§c你没有足够的" + currencyName + "！");
                }
            }
            
            if (processedLine.contains("{price}")) {
                processedLine = processedLine.replace("{price}", String.valueOf(item.getPricing()));
            }
            
            if (processedLine.contains("{currency}")) {
                processedLine = processedLine.replace("{currency}", currencyName);
            }
            
            if (processedLine.contains("{price_display}")) {
                String priceDisplay = getColoredPriceDisplay(item.getPricing(), item.getPricingType(), currencyName);
                processedLine = processedLine.replace("{price_display}", priceDisplay);
            }
            
            processedLore.add(processedLine);
        }
        
        if (item.getCategory().equals("quick_buy") && processedLore.size() > 0) {
            processedLore.add(processedLore.size() - 1, "§bShift + 左键从快速购买中移除！");
        }
        
        return processedLore;
    }

    private static String getColoredPriceDisplay(int price, String currencyType, String currencyName) {
        if (currencyType.startsWith("minecraft:")) {
            currencyType = currencyType.substring(10);
        }
        
        String color = switch (currencyType.toLowerCase()) {
            case "iron_ingot" -> "§f";
            case "gold_ingot" -> "§6";
            case "diamond" -> "§b";
            case "emerald" -> "§2";
            default -> "";
        };
        
        return color + price + " " + currencyName;
    }
} 
package cn.nekopixel.bedwars.utils.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Map;

public class PurchaseUtils {
    public static ItemStack createPurchaseItem(ItemStack shopItem) {
        ItemStack reward = new ItemStack(shopItem.getType(), shopItem.getAmount());
        ItemMeta rewardMeta = reward.getItemMeta();
        
        if (rewardMeta != null) {
            ItemMeta shopMeta = shopItem.getItemMeta();
            
            if (shopMeta != null && shopMeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : shopMeta.getEnchants().entrySet()) {
                    rewardMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }
            
            if (shopMeta instanceof PotionMeta && rewardMeta instanceof PotionMeta) {
                PotionMeta shopPotionMeta = (PotionMeta) shopMeta;
                PotionMeta rewardPotionMeta = (PotionMeta) rewardMeta;
                rewardPotionMeta.setBasePotionData(shopPotionMeta.getBasePotionData());
                if (shopPotionMeta.hasCustomEffects()) {
                    shopPotionMeta.getCustomEffects().forEach(effect -> 
                        rewardPotionMeta.addCustomEffect(effect, true));
                }
            }
            
            reward.setItemMeta(rewardMeta);
        }
        
        return reward;
    }

    public static int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static void removeMaterial(Player player, Material material, int amount) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != material) continue;

            int amt = item.getAmount();
            if (amt >= amount) {
                item.setAmount(amt - amount);
                return;
            } else {
                player.getInventory().clear(i);
                amount -= amt;
            }
        }
    }

    public static Material parseCurrency(String currency) {
        if (currency.startsWith("minecraft:")) {
            currency = currency.substring(10);
        }

        return switch (currency.toLowerCase()) {
            case "iron_ingot" -> Material.IRON_INGOT;
            case "gold_ingot" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            default -> null;
        };
    }

    public static String translateCurrency(String currency) {
        if (currency.startsWith("minecraft:")) {
            currency = currency.substring(10);
        }

        return switch (currency.toLowerCase()) {
            case "iron_ingot" -> "铁锭";
            case "gold_ingot" -> "金锭";
            case "diamond" -> "钻石";
            case "emerald" -> "绿宝石";
            default -> currency;
        };
    }

    public static boolean hasEnoughSpace(Player player, ItemStack item, int maxStackSize) {
        int amount = item.getAmount();
        int maxAmount = maxStackSize * 36; // 36个槽位

        int currentAmount = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                currentAmount += invItem.getAmount();
            }
        }

        return (currentAmount + amount) <= maxAmount;
    }
} 
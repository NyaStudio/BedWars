package cn.nekopixel.bedwars.utils.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import cn.nekopixel.bedwars.shop.NamespacedKeys;

import java.util.Map;

public class PurchaseUtils {
    public static ItemStack createPurchaseItem(ItemStack shopItem, Player player) {
        Material shopType = shopItem.getType();
        ItemStack reward;
        
        if (shopType.name().endsWith("_WOOL")) {
            TeamManager teamManager = Plugin.getInstance().getGameManager().getTeamManager();
            String team = teamManager.getPlayerTeam(player);
            if (team != null) {
                Material teamWool = getTeamWool(team);
                reward = new ItemStack(teamWool, shopItem.getAmount());
            } else {
                reward = new ItemStack(shopType, shopItem.getAmount());
            }
        } else {
            reward = new ItemStack(shopType, shopItem.getAmount());
        }
        
        ItemMeta rewardMeta = reward.getItemMeta();
        
        if (rewardMeta != null) {
            ItemMeta shopMeta = shopItem.getItemMeta();
            
            if (shopMeta != null && shopMeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : shopMeta.getEnchants().entrySet()) {
                    rewardMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }
            
            if (shopMeta != null) {
                PersistentDataContainer shopContainer = shopMeta.getPersistentDataContainer();
                PersistentDataContainer rewardContainer = rewardMeta.getPersistentDataContainer();
                
                if (shopContainer.has(NamespacedKeys.getInstance().getPopTowerKey(), PersistentDataType.BYTE)) {
                    rewardContainer.set(
                        NamespacedKeys.getInstance().getPopTowerKey(),
                        PersistentDataType.BYTE,
                        shopContainer.get(NamespacedKeys.getInstance().getPopTowerKey(), PersistentDataType.BYTE)
                    );
                    
                    rewardContainer.set(
                        NamespacedKeys.getInstance().getShopTypeKey(),
                        PersistentDataType.STRING,
                        "pop_tower"
                    );
                }
            }
            
            if (shopMeta instanceof PotionMeta && rewardMeta instanceof PotionMeta) {
                PotionMeta shopPotionMeta = (PotionMeta) shopMeta;
                PotionMeta rewardPotionMeta = (PotionMeta) rewardMeta;
                
                PersistentDataContainer container = shopMeta.getPersistentDataContainer();
                Integer customLevel = container.get(NamespacedKeys.getInstance().getCustomPotionLevel(), PersistentDataType.INTEGER);
                Integer customDuration = container.get(NamespacedKeys.getInstance().getCustomPotionDuration(), PersistentDataType.INTEGER);
                String customType = container.get(NamespacedKeys.getInstance().getCustomPotionType(), PersistentDataType.STRING);
                
                if (customLevel != null && customDuration != null && customType != null) {
                    PotionType potionType = PotionType.valueOf(customType);
                    
                    try {
                        boolean upgraded = customLevel >= 2 && isUpgradeable(potionType);
                        rewardPotionMeta.setBasePotionData(new PotionData(potionType, false, upgraded));
                    } catch (Exception e) {
                        rewardPotionMeta.setBasePotionData(new PotionData(PotionType.WATER));
                    }
                    
                    PersistentDataContainer rewardContainer = rewardMeta.getPersistentDataContainer();
                    rewardContainer.set(NamespacedKeys.getInstance().getCustomPotionLevel(), PersistentDataType.INTEGER, customLevel);
                    rewardContainer.set(NamespacedKeys.getInstance().getCustomPotionDuration(), PersistentDataType.INTEGER, customDuration);
                    rewardContainer.set(NamespacedKeys.getInstance().getCustomPotionType(), PersistentDataType.STRING, customType);
                } else {
                    rewardPotionMeta.setBasePotionData(shopPotionMeta.getBasePotionData());
                    if (shopPotionMeta.hasCustomEffects()) {
                        shopPotionMeta.getCustomEffects().forEach(effect -> {
                            rewardPotionMeta.addCustomEffect(effect, true);
                        });
                    }
                }
            }
            
            if (isWeapon(reward.getType())) {
                rewardMeta.setUnbreakable(true);
            }
            
            reward.setItemMeta(rewardMeta);
        }
        
        return reward;
    }

    private static boolean isWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || 
               name.endsWith("_AXE") || 
               name.endsWith("_PICKAXE") || 
               name.endsWith("_SHOVEL") || 
               name.endsWith("_HOE");
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

    private static int getArmorTier(Material material) {
        String name = material.name();
        if (name.startsWith("LEATHER_")) return 1;
        if (name.startsWith("CHAINMAIL_")) return 2;
        if (name.startsWith("IRON_")) return 3;
        if (name.startsWith("GOLDEN_") || name.startsWith("GOLD_")) return 4;
        if (name.startsWith("DIAMOND_")) return 5;
        if (name.startsWith("NETHERITE_")) return 6;
        return 0;
    }

    public static boolean canPurchaseArmor(Player player, ItemStack purchaseItem) {
        Material itemType = purchaseItem.getType();
        if (!itemType.name().endsWith("_BOOTS")) {
            return true;
        }
        
        int purchaseTier = getArmorTier(itemType);
        
        ItemStack currentLeggings = player.getInventory().getLeggings();
        
        if (currentLeggings == null) {
            return true;
        }
        
        int currentTier = getArmorTier(currentLeggings.getType());
        return purchaseTier > currentTier;
    }

    private static void upgradeArmor(Player player, Material bootsMaterial) {
        String materialPrefix = bootsMaterial.name().replace("_BOOTS", "");
        
        try {
            Material leggingsMaterial = Material.valueOf(materialPrefix + "_LEGGINGS");
            ItemStack leggings = new ItemStack(leggingsMaterial);
            ItemMeta leggingsMeta = leggings.getItemMeta();
            if (leggingsMeta != null) {
                leggingsMeta.setUnbreakable(true);
                leggings.setItemMeta(leggingsMeta);
            }
            
            player.getInventory().setLeggings(leggings);
            
        } catch (IllegalArgumentException e) {}
    }

    public static void giveItemToPlayer(Player player, ItemStack item) {
        Material itemType = item.getType();
        
        if (itemType.name().endsWith("_BOOTS")) {
            upgradeArmor(player, itemType);
        }
        
        Material woodenVersion = getWoodenVersion(itemType);
        
        if (woodenVersion != null) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack invItem = player.getInventory().getItem(i);
                if (invItem != null && invItem.getType() == woodenVersion) {
                    player.getInventory().setItem(i, item);
                    return;
                }
            }
        }
        
        if (itemType.name().endsWith("_HELMET") ||
            itemType.name().endsWith("_LEGGINGS") || 
            itemType.name().endsWith("_BOOTS")) {
            
            if (itemType.name().endsWith("_HELMET")) {
                player.getInventory().setHelmet(item);
            } else if (itemType.name().endsWith("_LEGGINGS")) {
                player.getInventory().setLeggings(item);
            } else if (itemType.name().endsWith("_BOOTS")) {
                player.getInventory().setBoots(item);
            }
            return;
        }
        
        player.getInventory().addItem(item);
    }
    
    private static Material getWoodenVersion(Material material) {
        String name = material.name();
        
        if (name.endsWith("_SWORD") && !name.equals("WOODEN_SWORD")) {
            return Material.WOODEN_SWORD;
        }
        if (name.endsWith("_AXE") && !name.equals("WOODEN_AXE")) {
            return Material.WOODEN_AXE;
        }
        if (name.endsWith("_PICKAXE") && !name.equals("WOODEN_PICKAXE")) {
            return Material.WOODEN_PICKAXE;
        }
        if (name.endsWith("_SHOVEL") && !name.equals("WOODEN_SHOVEL")) {
            return Material.WOODEN_SHOVEL;
        }
        if (name.endsWith("_HOE") && !name.equals("WOODEN_HOE")) {
            return Material.WOODEN_HOE;
        }
        
        return null;
    }

    private static Material getTeamWool(String team) {
        return switch (team.toLowerCase()) {
            case "red" -> Material.RED_WOOL;
            case "blue" -> Material.BLUE_WOOL;
            case "green" -> Material.LIME_WOOL;
            case "yellow" -> Material.YELLOW_WOOL;
            case "aqua" -> Material.LIGHT_BLUE_WOOL;
            case "white" -> Material.WHITE_WOOL;
            case "pink" -> Material.PINK_WOOL;
            case "gray" -> Material.GRAY_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }
    
    private static boolean isUpgradeable(PotionType type) {
        return switch (type) {
            case SPEED, SLOWNESS, STRENGTH, JUMP, REGEN, POISON, INSTANT_DAMAGE, INSTANT_HEAL -> true;
            default -> false;
        };
    }
} 
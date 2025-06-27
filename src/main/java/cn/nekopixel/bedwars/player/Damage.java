package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Damage implements Listener {
    private final Main plugin;
    
    public Damage(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        double baseDamage = getWeaponDamage(attacker.getInventory().getItemInMainHand());
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        int sharpnessLevel = (weapon != null) ? weapon.getEnchantmentLevel(Enchantment.DAMAGE_ALL) : 0;
        double sharpnessBonus = sharpnessLevel * 1.25;
        boolean isCritical = attacker.getFallDistance() > 0.0F && !attacker.isOnGround()
            && !attacker.isInWater() && attacker.getVelocity().getY() < 0;
        double criticalMultiplier = isCritical ? 1.5 : 1.0;
        double totalDamage = (baseDamage + sharpnessBonus);
        totalDamage *= criticalMultiplier;

        double armorReduction = getArmorReduction(victim);
        double protectionReduction = getProtectionReduction(victim, armorReduction);
        double totalReduction = armorReduction + protectionReduction;
        double finalDamage = totalDamage * (1 - totalReduction);
        event.setDamage(Math.max(0.1, finalDamage));
        
        if (plugin.getConfig().getBoolean("debug.damage", false)) {
            plugin.getLogger().info(String.format(
                "伤害计算: %s -> %s | 武器伤害:%.1f 锐利加成:%.1f 暴击:%.1fx 护甲减伤:%.1f%% Prot减伤:%.1f%% 最终伤害:%.1f",
                attacker.getName(), victim.getName(), 
                baseDamage / 2.0, sharpnessBonus / 2.0, criticalMultiplier,
                armorReduction * 100, protectionReduction * 100, 
                finalDamage / 2.0
            ));
        }
    }

    private double getWeaponDamage(ItemStack weapon) {
        if (weapon == null) {
            return 1.0;
        }
        
        Material type = weapon.getType();
        return switch (type) {
            case WOODEN_SWORD -> 5.0;
            case STONE_SWORD -> 6.0;
            case IRON_SWORD -> 7.0;
            case GOLDEN_SWORD -> 7.0;
            case DIAMOND_SWORD -> 8.0;
            default -> 1.0;
        };
    }

    private double getArmorReduction(Player player) {
        PlayerInventory inv = player.getInventory();
        double reduction = 0.0;
        
        ArmorType armorType = getArmorType(inv);
        
        switch (armorType) {
            case LEATHER -> reduction = 0.28;
            case CHAINMAIL -> reduction = 0.36;
            case IRON -> reduction = 0.44;
            case DIAMOND -> reduction = 0.52;
            case MIXED -> reduction = calculateMixedArmor(inv);
        }
        
        return reduction;
    }

    private ArmorType getArmorType(PlayerInventory inv) {
        Material helmet = inv.getHelmet() != null ? inv.getHelmet().getType() : null;
        Material chestplate = inv.getChestplate() != null ? inv.getChestplate().getType() : null;
        Material leggings = inv.getLeggings() != null ? inv.getLeggings().getType() : null;
        Material boots = inv.getBoots() != null ? inv.getBoots().getType() : null;
        
        if (isFullSet(helmet, chestplate, leggings, boots, "LEATHER")) {
            return ArmorType.LEATHER;
        } else if (isFullSet(helmet, chestplate, leggings, boots, "CHAINMAIL")) {
            return ArmorType.CHAINMAIL;
        } else if (isFullSet(helmet, chestplate, leggings, boots, "IRON")) {
            return ArmorType.IRON;
        } else if (isFullSet(helmet, chestplate, leggings, boots, "DIAMOND")) {
            return ArmorType.DIAMOND;
        }
        
        return ArmorType.MIXED;
    }

    private boolean isFullSet(Material helmet, Material chestplate, Material leggings, Material boots, String type) {
        String helmetType = helmet != null ? helmet.name() : "";
        String chestplateType = chestplate != null ? chestplate.name() : "";
        String leggingsType = leggings != null ? leggings.name() : "";
        String bootsType = boots != null ? boots.name() : "";
        
        return helmetType.startsWith(type) && chestplateType.startsWith(type) 
            && leggingsType.startsWith(type) && bootsType.startsWith(type);
    }

    private double calculateMixedArmor(PlayerInventory inv) {
        double totalPoints = 0;
        
        if (inv.getHelmet() != null) {
            totalPoints += getArmorPoints(inv.getHelmet().getType());
        }

        if (inv.getChestplate() != null) {
            totalPoints += getArmorPoints(inv.getChestplate().getType());
        }

        if (inv.getLeggings() != null) {
            totalPoints += getArmorPoints(inv.getLeggings().getType());
        }

        if (inv.getBoots() != null) {
            totalPoints += getArmorPoints(inv.getBoots().getType());
        }

        return Math.min(1.0, totalPoints * 0.04);
    }

    private double getArmorPoints(Material armor) {
        return switch (armor) {
            // 皮
            case LEATHER_HELMET -> 1;
            case LEATHER_CHESTPLATE -> 3;
            case LEATHER_LEGGINGS -> 2;
            case LEATHER_BOOTS -> 1;
            // 链
            case CHAINMAIL_HELMET -> 2;
            case CHAINMAIL_CHESTPLATE -> 5;
            case CHAINMAIL_LEGGINGS -> 4;
            case CHAINMAIL_BOOTS -> 1;
            // 铁
            case IRON_HELMET -> 2;
            case IRON_CHESTPLATE -> 6;
            case IRON_LEGGINGS -> 5;
            case IRON_BOOTS -> 2;
            // 钻石
            case DIAMOND_HELMET -> 3;
            case DIAMOND_CHESTPLATE -> 8;
            case DIAMOND_LEGGINGS -> 6;
            case DIAMOND_BOOTS -> 3;
            default -> 0;
        };
    }

    private double getProtectionReduction(Player player, double baseReduction) {
        PlayerInventory inv = player.getInventory();
        int totalProtLevel = 0;
        
        if (inv.getHelmet() != null) {
            totalProtLevel += inv.getHelmet().getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
        }

        if (inv.getChestplate() != null) {
            totalProtLevel += inv.getChestplate().getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
        }

        if (inv.getLeggings() != null) {
            totalProtLevel += inv.getLeggings().getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
        }

        if (inv.getBoots() != null) {
            totalProtLevel += inv.getBoots().getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
        }
        
        double protReduction = Math.min(20, totalProtLevel * 1.0) * 0.04 * (1 - baseReduction);
        
        return protReduction;
    }

    private enum ArmorType {
        LEATHER, CHAINMAIL, IRON, DIAMOND, MIXED
    }
}
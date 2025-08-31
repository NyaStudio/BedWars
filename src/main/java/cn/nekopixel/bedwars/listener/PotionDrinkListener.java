package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.shop.NamespacedKeys;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionDrinkListener implements Listener {

    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer customLevel = container.get(NamespacedKeys.getInstance().getCustomPotionLevel(), PersistentDataType.INTEGER);
        Integer customDuration = container.get(NamespacedKeys.getInstance().getCustomPotionDuration(), PersistentDataType.INTEGER);
        String customType = container.get(NamespacedKeys.getInstance().getCustomPotionType(), PersistentDataType.STRING);
        
        if (customLevel != null && customDuration != null && customType != null) {
            Player player = event.getPlayer();
            event.setCancelled(true);
            
            PotionEffectType effectType = getPotionEffectType(customType);
            if (effectType != null) {
                int amplifier = customLevel == 0 ? 0 : customLevel - 1;
                
                int duration;
                if (isInstantEffect(effectType)) {
                    duration = 1;
                } else {
                    duration = customDuration * 20;
                    player.removePotionEffect(effectType);
                }
                
                PotionEffect effect = new PotionEffect(effectType, duration, amplifier, true, true);
                player.addPotionEffect(effect);
                
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
            } else {
                Bukkit.getLogger().warning("未知的药水类型: " + customType);
            }
        }
    }
    
    private PotionEffectType getPotionEffectType(String type) {
        return switch (type) {
            case "SPEED" -> PotionEffectType.SPEED;
            case "SLOWNESS" -> PotionEffectType.SLOW;
            case "STRENGTH" -> PotionEffectType.INCREASE_DAMAGE;
            case "WEAKNESS" -> PotionEffectType.WEAKNESS;
            case "JUMP" -> PotionEffectType.JUMP;
            case "POISON" -> PotionEffectType.POISON;
            case "REGEN" -> PotionEffectType.REGENERATION;
            case "FIRE_RESISTANCE" -> PotionEffectType.FIRE_RESISTANCE;
            case "WATER_BREATHING" -> PotionEffectType.WATER_BREATHING;
            case "INVISIBILITY" -> PotionEffectType.INVISIBILITY;
            case "NIGHT_VISION" -> PotionEffectType.NIGHT_VISION;
            case "INSTANT_HEAL" -> PotionEffectType.HEAL;
            case "INSTANT_DAMAGE" -> PotionEffectType.HARM;
            case "SLOW_FALLING" -> PotionEffectType.SLOW_FALLING;
            case "LUCK" -> PotionEffectType.LUCK;
            default -> null;
        };
    }
    
    private boolean isInstantEffect(PotionEffectType type) {
        return type == PotionEffectType.HEAL || type == PotionEffectType.HARM;
    }
} 
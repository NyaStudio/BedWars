package cn.nekopixel.bedwars.utils.team;

import cn.nekopixel.bedwars.language.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class TeamEquipments {

    public static void setupTeamArmor(Player player, Color teamColor) {
        ItemStack[] armor = new ItemStack[4];
        
        armor[0] = createLeatherArmor(Material.LEATHER_BOOTS, teamColor);
        armor[1] = createLeatherArmor(Material.LEATHER_LEGGINGS, teamColor);
        armor[2] = createLeatherArmor(Material.LEATHER_CHESTPLATE, teamColor);
        armor[3] = createLeatherArmor(Material.LEATHER_HELMET, teamColor);

        player.getInventory().setArmorContents(armor);
    }

    private static ItemStack createLeatherArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        
        if (material == Material.LEATHER_HELMET) {
//            meta.addEnchant(Enchantment.OXYGEN, 1, true);
//            meta.addEnchant(Enchantment.DURABILITY, 3, true);
            meta.addEnchant(Enchantment.WATER_WORKER, 1, true);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    public static void setupTeamItems(Player player) {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true);
        sword.setItemMeta(swordMeta);
        player.getInventory().setItem(0, sword);

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GOLD + LanguageManager.getInstance().getMessage("item.unknown"));
        compass.setItemMeta(compassMeta);
        player.getInventory().setItem(8, compass);
    }
} 
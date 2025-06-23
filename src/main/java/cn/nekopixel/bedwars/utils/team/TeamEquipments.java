package cn.nekopixel.bedwars.utils.team;

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
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    public static void setupTeamItems(Player player) {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.DURABILITY, 10, true);
        sword.setItemMeta(swordMeta);
        player.getInventory().setItem(0, sword);

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GOLD + "这是啥来着");
        compass.setItemMeta(compassMeta);
        player.getInventory().setItem(8, compass);
    }
} 
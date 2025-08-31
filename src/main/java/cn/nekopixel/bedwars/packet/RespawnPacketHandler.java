package cn.nekopixel.bedwars.packet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RespawnPacketHandler {
    private static final Set<UUID> hiddenPlayers = new HashSet<>();
    private static Plugin plugin;
    
    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    public static void hidePlayer(Player player) {
        hiddenPlayers.add(player.getUniqueId());
        
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hidePlayer(plugin, player);
            }
        }
        
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY, 
            Integer.MAX_VALUE, 
            0, 
            false, 
            false
        ));
    }
    
    public static void showPlayer(Player player) {
        hiddenPlayers.remove(player.getUniqueId());
        
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.showPlayer(plugin, player);
            }
        }
        
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }
    
    public static boolean isHidden(UUID playerUUID) {
        return hiddenPlayers.contains(playerUUID);
    }
} 
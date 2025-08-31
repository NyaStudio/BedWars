package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpectatorManager {
    private final Main plugin;
    private final Set<UUID> spectatorPlayers = new HashSet<>();
    
    public SpectatorManager(Main plugin) {
        this.plugin = plugin;
    }
    
    public void setSpectator(Player player, boolean spectator) {
        if (spectator) {
            player.setGameMode(GameMode.ADVENTURE);
            spectatorPlayers.add(player.getUniqueId());
        } else {
            spectatorPlayers.remove(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
    
    public boolean isSpectator(UUID playerId) {
        return spectatorPlayers.contains(playerId);
    }
    
    public boolean isSpectator(Player player) {
        return spectatorPlayers.contains(player.getUniqueId());
    }
    
    public boolean shouldCancelInteraction(Player player) {
        return isSpectator(player);
    }
    
    public boolean shouldCancelDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (isSpectator(attacker)) {
                return true;
            }
        }
        
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (isSpectator(victim)) {
                return true;
            }
        }
        
        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                if (isSpectator(shooter)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void clearAll() {
        spectatorPlayers.clear();
    }
    
    public Set<UUID> getSpectatorPlayers() {
        return new HashSet<>(spectatorPlayers);
    }
} 
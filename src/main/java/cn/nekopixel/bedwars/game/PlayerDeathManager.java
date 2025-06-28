package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.packet.RespawnPacketHandler;
import cn.nekopixel.bedwars.tab.TabListManager;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerDeathManager {
    private final Main plugin;
    private final Set<UUID> respawningPlayers = new HashSet<>();
    private final Map<UUID, DeathState> disconnectedDeathStates = new HashMap<>();
    private final Map<UUID, Long> invulnerablePlayers = new HashMap<>();
    
    public static class DeathState {
        public final boolean isSpectator;
        public final String team;
        public final long disconnectTime;
        
        public DeathState(boolean isSpectator, String team) {
            this.isSpectator = isSpectator;
            this.team = team;
            this.disconnectTime = System.currentTimeMillis();
        }
    }
    
    public PlayerDeathManager(Main plugin) {
        this.plugin = plugin;
    }
    
    public void prepareForDeath(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
    }
    
    public void setRespawning(Player player, boolean respawning) {
        if (respawning) {
            respawningPlayers.add(player.getUniqueId());
            player.setAllowFlight(true);
            player.setFlying(true);
            RespawnPacketHandler.hidePlayer(player);
            
            TabListManager tabListManager = Plugin.getInstance().getTabListManager();
            if (tabListManager != null) {
                tabListManager.setTemporarySpectator(player, true);
            }
        } else {
            respawningPlayers.remove(player.getUniqueId());
            player.setFlying(false);
            player.setAllowFlight(false);
            RespawnPacketHandler.showPlayer(player);
            
            TabListManager tabListManager = Plugin.getInstance().getTabListManager();
            if (tabListManager != null) {
                tabListManager.setTemporarySpectator(player, false);
            }
        }
    }
    
    public boolean isRespawning(UUID playerId) {
        return respawningPlayers.contains(playerId);
    }
    
    public void saveDisconnectedState(UUID playerId, DeathState state) {
        disconnectedDeathStates.put(playerId, state);
    }
    
    public DeathState getDisconnectedState(UUID playerId) {
        return disconnectedDeathStates.get(playerId);
    }
    
    public void removeDisconnectedState(UUID playerId) {
        disconnectedDeathStates.remove(playerId);
    }
    
    public Set<UUID> getRespawningPlayers() {
        return new HashSet<>(respawningPlayers);
    }
    
    public void setRespawnInvulnerable(Player player, int seconds) {
        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        invulnerablePlayers.put(player.getUniqueId(), endTime);
    }
    
    public boolean isInvulnerable(UUID playerId) {
        Long endTime = invulnerablePlayers.get(playerId);
        if (endTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= endTime) {
            invulnerablePlayers.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    public int getRemainingInvulnerableTime(UUID playerId) {
        Long endTime = invulnerablePlayers.get(playerId);
        if (endTime == null) {
            return 0;
        }
        
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) {
            invulnerablePlayers.remove(playerId);
            return 0;
        }
        
        return (int) Math.ceil(remaining / 1000.0);
    }
    
    public void removeInvulnerable(UUID playerId) {
        invulnerablePlayers.remove(playerId);
    }
    
    public void clearAll() {
        respawningPlayers.clear();
        disconnectedDeathStates.clear();
        invulnerablePlayers.clear();
    }
} 
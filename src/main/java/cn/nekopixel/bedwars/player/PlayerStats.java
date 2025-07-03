package cn.nekopixel.bedwars.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private static final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    
    private int kills = 0;
    private int finalKills = 0;
    private int bedsBroken = 0;
    
    private PlayerStats() {}
    
    public static PlayerStats getStats(UUID playerUUID) {
        return playerStats.computeIfAbsent(playerUUID, k -> new PlayerStats());
    }
    
    public static void clearAll() {
        playerStats.clear();
    }
    
    public static void clearPlayer(UUID playerUUID) {
        playerStats.remove(playerUUID);
    }
    
    public void addKill() {
        this.kills++;
    }
    
    public void addFinalKill() {
        this.finalKills++;
    }
    
    public void addBedBroken() {
        this.bedsBroken++;
    }
    
    public int getKills() {
        return kills;
    }
    
    public int getFinalKills() {
        return finalKills;
    }
    
    public int getBedsBroken() {
        return bedsBroken;
    }
    
    public void reset() {
        this.kills = 0;
        this.finalKills = 0;
        this.bedsBroken = 0;
    }
} 
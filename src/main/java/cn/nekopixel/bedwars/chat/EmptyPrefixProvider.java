package cn.nekopixel.bedwars.chat;

import org.bukkit.entity.Player;

import java.util.UUID;

public class EmptyPrefixProvider implements PrefixProvider {
    
    @Override
    public String getPrefix(Player player) {
        return "";
    }
    
    @Override
    public String getPrefix(UUID playerId) {
        return "";
    }
    
    @Override
    public void clearCache(UUID playerId) {}
    
    @Override
    public void clearAllCache() {}
    
    @Override
    public void reload() {}
} 
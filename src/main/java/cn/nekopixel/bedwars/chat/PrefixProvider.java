package cn.nekopixel.bedwars.chat;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface PrefixProvider {
    String getPrefix(Player player);
    String getPrefix(UUID playerId);

    void clearCache(UUID playerId);
    void clearAllCache();
    void reload();
} 
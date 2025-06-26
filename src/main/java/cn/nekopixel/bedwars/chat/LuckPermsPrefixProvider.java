package cn.nekopixel.bedwars.chat;

import cn.nekopixel.bedwars.Main;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LuckPermsPrefixProvider implements PrefixProvider {
    
    private final Main plugin;
    private final Map<UUID, String> prefixCache = new ConcurrentHashMap<>();
    private LuckPerms luckPerms;
    
    public LuckPermsPrefixProvider(Main plugin) {
        this.plugin = plugin;
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            plugin.getLogger().warning("无法获取LuckPerms API: " + e.getMessage());
        }
    }
    
    @Override
    public String getPrefix(Player player) {
        return getPrefix(player.getUniqueId());
    }
    
    @Override
    public String getPrefix(UUID playerId) {
        if (luckPerms == null) {
            return "";
        }
        
        String cachedPrefix = prefixCache.get(playerId);
        if (cachedPrefix != null) {
            return cachedPrefix;
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(playerId);
            if (user == null) {
                user = luckPerms.getUserManager().loadUser(playerId).join();
            }
            
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                
                if (prefix != null && !prefix.isEmpty()) {
                    String formattedPrefix = ChatColor.translateAlternateColorCodes('&', prefix);
                    prefixCache.put(playerId, formattedPrefix);
                    return formattedPrefix;
                }
            }
        } catch (Exception e) {
            Player player = Bukkit.getPlayer(playerId);
            String playerName = player != null ? player.getName() : playerId.toString();
            plugin.getLogger().warning("获取玩家 " + playerName + " 的 LuckPerms 前缀时出错: " + e.getMessage());
        }
        
        prefixCache.put(playerId, "");
        return "";
    }
    
    @Override
    public void clearCache(UUID playerId) {
        prefixCache.remove(playerId);
    }
    
    @Override
    public void clearAllCache() {
        prefixCache.clear();
    }
    
    @Override
    public void reload() {
        clearAllCache();
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            plugin.getLogger().warning("重载时无法获取 LuckPerms API: " + e.getMessage());
        }
    }
} 
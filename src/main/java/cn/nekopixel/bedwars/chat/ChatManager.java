package cn.nekopixel.bedwars.chat;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.SpectatorManager;
import cn.nekopixel.bedwars.game.PlayerDeathManager;
import cn.nekopixel.bedwars.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {
    private final Main plugin;
    private final TeamManager teamManager;
    private final Map<String, String> teamColors;
    private final Map<String, String> teamNames;
    private String chatFormat;
    private PrefixProvider prefixProvider;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.teamManager = Plugin.getInstance().getGameManager().getTeamManager();
        this.teamColors = new HashMap<>();
        this.teamNames = new HashMap<>();
        initPrefixProvider();
        loadConfig();
    }

    private void initPrefixProvider() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                Class<?> clazz = Class.forName("cn.nekopixel.bedwars.chat.LuckPermsPrefixProvider");
                this.prefixProvider = (PrefixProvider) clazz.getConstructor(Main.class).newInstance(plugin);
                plugin.getLogger().info("检测到 LuckPerms，将与其一起工作！");
            } catch (Exception e) {
                plugin.getLogger().warning("无法加载LuckPerms支持: " + e.getMessage());
                this.prefixProvider = new EmptyPrefixProvider();
            }
        } else {
            this.prefixProvider = new EmptyPrefixProvider();
        }
    }

    public String getPlayerPrefix(Player player) {
        return prefixProvider.getPrefix(player);
    }
    
    public void clearPrefixCache(UUID playerId) {
        prefixProvider.clearCache(playerId);
    }
    
    public void clearAllPrefixCache() {
        prefixProvider.clearAllCache();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "chatting.yml");
        if (!configFile.exists()) {
            plugin.saveResource("chatting.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        chatFormat = ChatColor.translateAlternateColorCodes('&', config.getString("chat.format", "[%team_color%%team_name%&r] &7%player%&8: &f%message%"));

        if (config.contains("chat.team_colors")) {
            for (String key : config.getConfigurationSection("chat.team_colors").getKeys(false)) {
                String color = config.getString("chat.team_colors." + key);
                teamColors.put(key.toLowerCase(), ChatColor.translateAlternateColorCodes('&', color));
            }
        }

        if (config.contains("chat.team_names")) {
            for (String key : config.getConfigurationSection("chat.team_names").getKeys(false)) {
                String name = config.getString("chat.team_names." + key);
                teamNames.put(key.toLowerCase(), name);
            }
        }
    }

    public String formatMessage(Player player, String message) {
        GameManager gameManager = Plugin.getInstance().getGameManager();
        String prefix = getPlayerPrefix(player);
        
        if (gameManager.isStatus(GameStatus.WAITING) || gameManager.isStatus(GameStatus.ENDING)) {
            if (!prefix.isEmpty()) {
                String separator = prefix.endsWith(" ") ? "" : " ";
                return prefix + separator + player.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + message;
            }
            return ChatColor.GRAY + player.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + message;
        }
        
        if (!gameManager.isStatus(GameStatus.INGAME)) {
            return ChatColor.GRAY + player.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + message;
        }

        String team = teamManager.getPlayerTeam(player);
        
        SpectatorManager spectatorManager = GameManager.getInstance().getSpectatorManager();
        PlayerDeathManager deathManager = GameManager.getInstance().getPlayerDeathManager();
        boolean isObserverState = team == null || 
                                  spectatorManager.isSpectator(player.getUniqueId()) || 
                                  deathManager.isRespawning(player.getUniqueId());
        
        if (isObserverState) {
            team = "spectator";
        }

        String teamColor = teamColors.getOrDefault(team.toLowerCase(), "&7");
        String teamName = teamNames.getOrDefault(team.toLowerCase(), "未知队伍");

        String playerDisplay;
        if (prefix.isEmpty()) {
            playerDisplay = player.getName();
        } else {
            String separator = prefix.endsWith(" ") ? "" : " ";
            playerDisplay = prefix + separator + player.getName();
        }
        
        return chatFormat
                .replace("%team_color%", teamColor)
                .replace("%team_name%", teamName)
                .replace("%player%", playerDisplay)
                .replace("%message%", message);
    }

    public String formatShoutMessage(Player player, String message) {
        GameManager gameManager = Plugin.getInstance().getGameManager();
        String prefix = getPlayerPrefix(player);
        
        if (!gameManager.isStatus(GameStatus.INGAME)) {
            if (!prefix.isEmpty()) {
                String separator = prefix.endsWith(" ") ? "" : " ";
                return ChatColor.GOLD + "[喊话] " + prefix + separator + player.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + message;
            }
            return ChatColor.GOLD + "[喊话] " + ChatColor.GRAY + player.getName() + ChatColor.DARK_GRAY + ": " + ChatColor.WHITE + message;
        }

        String team = teamManager.getPlayerTeam(player);
        if (team == null) team = "spectator";

        String teamColor = teamColors.getOrDefault(team.toLowerCase(), "&7");
        String teamName = teamNames.getOrDefault(team.toLowerCase(), "未知队伍");

        String playerDisplay;
        if (prefix.isEmpty()) {
            playerDisplay = player.getName();
        } else {
            String separator = prefix.endsWith(" ") ? "" : " ";
            playerDisplay = prefix + separator + player.getName();
        }
        
        String shoutFormat = "&6[喊话] " + chatFormat;
        
        return ChatColor.translateAlternateColorCodes('&', shoutFormat
                .replace("%team_color%", teamColor)
                .replace("%team_name%", teamName)
                .replace("%player%", playerDisplay)
                .replace("%message%", message));
    }

    public void reloadConfig() {
        teamColors.clear();
        teamNames.clear();
        initPrefixProvider();
        prefixProvider.reload();
        loadConfig();
    }
}
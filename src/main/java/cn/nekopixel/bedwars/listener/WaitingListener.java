package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WaitingListener implements Listener {
    private final Main plugin;

    public WaitingListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            Player player = event.getPlayer();
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setLevel(0);
            player.setExp(0f);
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            
            int currentPlayers = Bukkit.getOnlinePlayers().size();
            int maxPlayers = getMaxPlayers();
            
            String coloredName = getColoredPlayerName(player);
            String joinMessage = coloredName + " §e加入了游戏 (§b" + currentPlayers + "§e/§b" + maxPlayers + "§e).";
            Bukkit.broadcastMessage(joinMessage);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            Player player = event.getPlayer();
            
            String coloredName = getColoredPlayerName(player);
            String quitMessage = coloredName + " §e离开了游戏！";
            Bukkit.broadcastMessage(quitMessage);
        }
    }
    
    private String getColoredPlayerName(Player player) {
        ChatManager chatManager = Plugin.getInstance().getChatManager();
        String prefix = chatManager.getPlayerPrefix(player);
        
        if (prefix.isEmpty()) {
            return player.getName();
        }
        
        String lastColor = extractLastColor(prefix);
        return lastColor + player.getName();
    }
    
    private String extractLastColor(String text) {
        String lastColor = "";
        
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§' || text.charAt(i) == '&') {
                char colorCode = text.charAt(i + 1);
                if ((colorCode >= '0' && colorCode <= '9') ||
                    (colorCode >= 'a' && colorCode <= 'f') ||
                    (colorCode >= 'A' && colorCode <= 'F')) {
                    lastColor = "§" + colorCode;
                }
            }
        }
        
        return lastColor;
    }
    
    private int getMaxPlayers() {
        String mode = plugin.getConfig().getString("game.mode", "4s").toLowerCase();
        return switch (mode) {
            case "solo" -> 8;
            case "double" -> 16;
            case "3s" -> 12;
            case "4s" -> 16;
            case "4v4" -> 8;
            default -> 16;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(event.getBlock().getLocation(), event.getBlock().getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            Player player = event.getPlayer();
            
            if (player.getLocation().getY() < -100) {
                Location joinLocation = getJoinLocation();
                if (joinLocation != null) {
                    player.teleport(joinLocation);
                }
            }
        }
    }
    
    private Location getJoinLocation() {
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup == null) {
            return null;
        }
        
        if (mapSetup.getMapConfig().contains("join")) {
            return Location.deserialize(mapSetup.getMapConfig().getConfigurationSection("join").getValues(false));
        }
        
        return null;
    }
} 
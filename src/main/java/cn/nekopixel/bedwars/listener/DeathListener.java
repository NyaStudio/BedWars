package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.config.ConfigLoader;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.packet.RespawnPacketHandler;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.utils.LocationUtils;
import cn.nekopixel.bedwars.utils.team.TeamEquipments;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DeathListener implements Listener {
    private final Main plugin;
    private final Set<UUID> respawningPlayers = new HashSet<>();
    
    public DeathListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        if (respawningPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth <= 0) {
            event.setCancelled(true);
            handlePlayerDeath(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        
        for (UUID respawningPlayerId : respawningPlayers) {
            Player respawningPlayer = Bukkit.getPlayer(respawningPlayerId);
            if (respawningPlayer != null && respawningPlayer.isOnline()) {
                joiningPlayer.hidePlayer(plugin, respawningPlayer);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (respawningPlayers.contains(playerId)) {
            respawningPlayers.remove(playerId);
            player.setFlying(false);
            player.setAllowFlight(false);
            RespawnPacketHandler.showPlayer(player);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        Player player = event.getPlayer();
        if (player.getLocation().getY() < -100) {
            if (respawningPlayers.contains(player.getUniqueId())) {
                Location respawningLocation = getRespawningLocation();
                if (respawningLocation != null) {
                    player.teleport(respawningLocation);
                }
            } else {
                handlePlayerDeath(player);
            }
        }
    }

    
    private void handlePlayerDeath(Player player) {
        respawningPlayers.add(player.getUniqueId());
        
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
                player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        
        player.setAllowFlight(true);
        player.setFlying(true);
        
        Location respawningLocation = getRespawningLocation();
        if (respawningLocation != null) {
            player.teleport(respawningLocation);
        }
        
        player.sendTitle("§c你死了！", "§e你将在§c5§e秒后重生！", 10, 70, 20);
        player.sendMessage("§e你将在§c5§e秒后重生！");
        
        RespawnPacketHandler.hidePlayer(player);
        
        new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                countdown--;
                
                if (countdown > 0) {
                    player.sendTitle("§c你死了！", "§e你将在§c" + countdown + "§e秒后重生！", 0, 40, 10);
                    player.sendMessage("§e你将在§c" + countdown + "§e秒后重生！");
                } else {
                    respawnPlayer(player);
                    respawningPlayers.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void respawnPlayer(Player player) {
        RespawnPacketHandler.showPlayer(player);
        
        player.setFlying(false);
        player.setAllowFlight(false);
        
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        String team = teamManager.getPlayerTeam(player);
        
        if (team == null) {
            plugin.getLogger().warning("玩家 " + player.getName() + " 没有队伍！");
            return;
        }
        
        java.util.Map<String, Location> spawnPoints = ConfigLoader.loadTeamSpawns();
        Location spawnPoint = spawnPoints.get(team.toLowerCase());
        
        if (spawnPoint != null) {
            Location safeLocation = LocationUtils.findSafeLocation(spawnPoint, 3);
            player.teleport(safeLocation);
            
            setupPlayerEquipment(player, team);
            
            player.sendMessage("§e你已经重生！");
            player.sendTitle("§a已重生！", "", 10, 40, 20);
        } else {
            plugin.getLogger().warning("队伍 " + team + " 没有设置出生点！");
        }
    }
    
    private Location getRespawningLocation() {
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup == null) {
            return null;
        }
        
        if (mapSetup.getMapConfig().contains("respawning")) {
            return Location.deserialize(mapSetup.getMapConfig().getConfigurationSection("respawning").getValues(false));
        }
        
        return null;
    }
    
    private void setupPlayerEquipment(Player player, String team) {
        Color teamColor = getTeamColor(team);
        TeamEquipments.setupTeamArmor(player, teamColor);
        TeamEquipments.setupTeamItems(player);
    }
    
    private Color getTeamColor(String team) {
        return switch (team.toLowerCase()) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "yellow" -> Color.YELLOW;
            case "aqua" -> Color.AQUA;
            case "white" -> Color.WHITE;
            case "pink" -> Color.FUCHSIA;
            case "gray" -> Color.GRAY;
            default -> Color.RED;
        };
    }
} 
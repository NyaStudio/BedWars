package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.config.ConfigLoader;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.utils.LocationUtils;
import cn.nekopixel.bedwars.utils.team.TeamEquipments;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RespawnManager {
    private final Main plugin;
    private final PlayerDeathManager deathManager;
    
    public RespawnManager(Main plugin, PlayerDeathManager deathManager) {
        this.plugin = plugin;
        this.deathManager = deathManager;
    }
    
    public void startRespawnCountdown(Player player, int seconds) {
        INGameTitle.showDynamic(player,
            remainingSeconds -> "§c你死了！", 
            remainingSeconds -> "§e你将在§c" + remainingSeconds + "§e秒后重生！", 
            seconds, 0, 0);
        
        new BukkitRunnable() {
            int countdown = seconds;
            
            @Override
            public void run() {
                countdown--;
                
                if (countdown > 0) {
                    player.sendMessage("§e你将在§c" + countdown + "§e秒后重生！");
                } else {
                    INGameTitle.cancel(player);
                    respawnPlayer(player);
                    deathManager.setRespawning(player, false);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    public void respawnPlayer(Player player) {
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
            
            int invulnerableTime = plugin.getConfig().getInt("game.respawn_invulnerable_time", 3);
            if (invulnerableTime > 0) {
                PlayerDeathManager deathManager = GameManager.getInstance().getPlayerDeathManager();
                deathManager.setRespawnInvulnerable(player, invulnerableTime);
            }
            
            player.sendMessage("§e你已经重生！");
            INGameTitle.show(player, "§a已重生！", "", 3, 10, 20);
        } else {
            plugin.getLogger().warning("队伍 " + team + " 没有设置出生点！");
        }
    }
    
    public Location getRespawningLocation() {
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
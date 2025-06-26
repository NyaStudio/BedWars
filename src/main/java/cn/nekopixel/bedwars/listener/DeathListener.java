package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.*;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DeathListener implements Listener {
    private final Main plugin;
    private final PlayerDeathManager deathManager;
    private final SpectatorManager spectatorManager;
    private final RespawnManager respawnManager;
    private static DeathListener instance;
    
    public DeathListener(Main plugin) {
        this.plugin = plugin;
        this.deathManager = GameManager.getInstance().getPlayerDeathManager();
        this.spectatorManager = GameManager.getInstance().getSpectatorManager();
        this.respawnManager = new RespawnManager(plugin, deathManager);
        instance = this;
    }
    
    public static DeathListener getInstance() {
        return instance;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
            event.setCancelled(true);
            return;
        }
        
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        if (deathManager.isRespawning(player.getUniqueId())) {
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
        UUID playerId = joiningPlayer.getUniqueId();
        
        for (UUID respawningPlayerId : deathManager.getRespawningPlayers()) {
            Player respawningPlayer = Bukkit.getPlayer(respawningPlayerId);
            if (respawningPlayer != null && respawningPlayer.isOnline()) {
                joiningPlayer.hidePlayer(plugin, respawningPlayer);
            }
        }
        
        PlayerDeathManager.DeathState deathState = deathManager.getDisconnectedState(playerId);
        if (deathState != null && GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            deathManager.removeDisconnectedState(playerId);
            
            if (deathState.team != null) {
                BedManager bedManager = GameManager.getInstance().getBedManager();
                boolean hasBed = bedManager.hasBed(deathState.team);
                
                if (!deathState.isSpectator && hasBed) {
                    handleReconnectDeath(joiningPlayer, deathState.team);
                } else {
                    makeSpectator(joiningPlayer);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (deathManager.isRespawning(playerId)) {
            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            String team = teamManager.getPlayerTeam(player);
            boolean isSpectator = spectatorManager.isSpectator(playerId);
            
            deathManager.saveDisconnectedState(playerId, 
                new PlayerDeathManager.DeathState(isSpectator, team));
            
            deathManager.setRespawning(player, false);
        } else if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            String team = teamManager.getPlayerTeam(player);
            
            if (team != null && !spectatorManager.isSpectator(playerId)) {
                deathManager.saveDisconnectedState(playerId, 
                    new PlayerDeathManager.DeathState(false, team));
            }
        }
        
        spectatorManager.setSpectator(player, false);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        Player player = event.getPlayer();
        if (player.getLocation().getY() < -100) {
            if (deathManager.isRespawning(player.getUniqueId())) {
                Location respawningLocation = respawnManager.getRespawningLocation();
                if (respawningLocation != null) {
                    player.teleport(respawningLocation);
                }
            } else {
                handlePlayerDeath(player);
            }
        }
    }
    
    private void handlePlayerDeath(Player player) {
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        String team = teamManager.getPlayerTeam(player);
        
        if (team == null) {
            plugin.getLogger().warning("玩家 " + player.getName() + " 没有队伍！");
            return;
        }
        
        BedManager bedManager = GameManager.getInstance().getBedManager();
        boolean hasBed = bedManager.hasBed(team);
        
        deathManager.prepareForDeath(player);
        
        if (hasBed) {
            deathManager.setRespawning(player, true);
            
            Location respawningLocation = respawnManager.getRespawningLocation();
            if (respawningLocation != null) {
                player.teleport(respawningLocation);
            }
            
            player.sendTitle("§c你死了！", "§e你将在§c5§e秒后重生！", 0, 70, 0);
            player.sendMessage("§e你将在§c5§e秒后重生！");
            
            respawnManager.startRespawnCountdown(player, 5);
        } else {
            makeSpectator(player);
            player.sendTitle("§c你死了！", "§7你现在是观察者！", 0, 70, 0);
        }
        
        checkTeamElimination(team);
    }
    
    private void makeSpectator(Player player) {
        deathManager.setRespawning(player, true);
        spectatorManager.setSpectator(player, true);
        
        Location respawningLocation = respawnManager.getRespawningLocation();
        if (respawningLocation != null) {
            player.teleport(respawningLocation);
        }
    }
    
    private void checkTeamElimination(String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        if (bedManager.hasBed(team)) {
            return;
        }
        
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        Set<UUID> teamPlayers = teamManager.getTeamPlayers(team);
        
        for (UUID playerId : teamPlayers) {
            Player teamPlayer = Bukkit.getPlayer(playerId);
            if (teamPlayer != null && teamPlayer.isOnline()) {
                if (!deathManager.isRespawning(playerId) && !spectatorManager.isSpectator(playerId)) {
                    return;
                }
            }
        }
        
        String teamColor = bedManager.getTeamChatColor(team);
        String teamName = bedManager.getTeamDisplayName(team);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f团灭 > " + teamColor + teamName + " §7已被团灭！");
        Bukkit.broadcastMessage("");
        
        checkVictory();
    }
    
    public void checkVictory() {
        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
            return;
        }
        
        BedManager bedManager = GameManager.getInstance().getBedManager();
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        
        List<String> aliveTeams = new ArrayList<>();
        String[] allTeams = {"red", "blue", "green", "yellow", "aqua", "white", "pink", "gray"};
        
        for (String team : allTeams) {
            if (bedManager.hasBed(team)) {
                aliveTeams.add(team);
                continue;
            }
            
            Set<UUID> teamPlayers = teamManager.getTeamPlayers(team);
            if (teamPlayers == null || teamPlayers.isEmpty()) {
                continue;
            }
            
            boolean hasAlivePlayer = false;
            for (UUID playerId : teamPlayers) {
                Player teamPlayer = Bukkit.getPlayer(playerId);
                if (teamPlayer != null && teamPlayer.isOnline()) {
                    if (!deathManager.isRespawning(playerId) && !spectatorManager.isSpectator(playerId)) {
                        hasAlivePlayer = true;
                        break;
                    }
                }
            }
            
            if (hasAlivePlayer) {
                aliveTeams.add(team);
            }
        }
        
        if (aliveTeams.size() == 1) {
            String winningTeam = aliveTeams.get(0);
            announceVictory(winningTeam);
        }
    }
    
    private void announceVictory(String winningTeam) {
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        Set<UUID> winningPlayers = teamManager.getTeamPlayers(winningTeam);
        
        BedManager bedManager = GameManager.getInstance().getBedManager();
        String teamColor = bedManager.getTeamChatColor(winningTeam);
        String teamName = bedManager.getTeamDisplayName(winningTeam);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e游戏结束！");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f获胜队伍 - " + teamColor + teamName);
        Bukkit.broadcastMessage("");
        
        for (UUID playerId : winningPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendTitle("§6胜利！", "", 0, 100, 0);
            }
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!winningPlayers.contains(player.getUniqueId())) {
                player.sendTitle("§c游戏结束！", "", 0, 100, 0);
            }
        }
        
        GameManager.getInstance().setStatus(GameStatus.ENDING);
        
        plugin.getLogger().info("游戏结束，即将关闭服务器...");
        
        new BukkitRunnable() {
            int countdown = 60;
            
            @Override
            public void run() {
                if (countdown == 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Bukkit.getServer().shutdown();
                    }, 20L);
                    
                    this.cancel();
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(event.getBlock().getLocation(), event.getBlock().getBlockData());
            return;
        }

        if (spectatorManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            
            final org.bukkit.block.data.BlockData blockData = event.getBlock().getBlockData();
            final Location blockLoc = event.getBlock().getLocation();
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendBlockChange(blockLoc, blockData);
            }, 1L);
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
            event.setCancelled(true);
            return;
        }
        
        if (spectatorManager.isSpectator(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (spectatorManager.shouldCancelInteraction(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (spectatorManager.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.ENDING)) {
            event.setCancelled(true);
            return;
        }
        
        if (spectatorManager.shouldCancelDamage(event)) {
            event.setCancelled(true);
            return;
        }
        
        if (event.getDamager() instanceof Player &&
            deathManager.isRespawning(((Player) event.getDamager()).getUniqueId())) {
            event.setCancelled(true);
        }
        
        if (event.getEntity() instanceof Player && 
            deathManager.isRespawning(((Player) event.getEntity()).getUniqueId())) {
            event.setCancelled(true);
        }
        
        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                if (deathManager.isRespawning(shooter.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            if (deathManager.isRespawning(shooter.getUniqueId()) || 
                spectatorManager.isSpectator(shooter)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.RESETTING) {
            deathManager.clearAll();
            spectatorManager.clearAll();
        }
    }
    
    private void handleReconnectDeath(Player player, String team) {
        deathManager.prepareForDeath(player);
        deathManager.setRespawning(player, true);
        
        Location respawningLocation = respawnManager.getRespawningLocation();
        if (respawningLocation != null) {
            player.teleport(respawningLocation);
        }
        
        player.sendTitle("§c你死了！", "§e你将在§c10§e秒后重生！", 0, 70, 0);
        player.sendMessage("§e你将在§c10§e秒后重生！");
        
        respawnManager.startRespawnCountdown(player, 10);
    }
}
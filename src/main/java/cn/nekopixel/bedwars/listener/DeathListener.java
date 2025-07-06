package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.*;
import cn.nekopixel.bedwars.game.SpectatorManager;
import cn.nekopixel.bedwars.game.RespawnManager;
import cn.nekopixel.bedwars.packet.RespawnPacketHandler;
import cn.nekopixel.bedwars.player.PlayerStats;
import cn.nekopixel.bedwars.tab.TabListManager;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.utils.SoundUtils;
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
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class DeathListener implements Listener {
    private final Main plugin;
    private final PlayerDeathManager deathManager;
    private final SpectatorManager spectatorManager;
    private final RespawnManager respawnManager;
    private static DeathListener instance;
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Set<String> eliminatedTeams = new HashSet<>();
    
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
        
        if (deathManager.isInvulnerable(player.getUniqueId())) {
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
        
        lastDamager.remove(playerId);
        lastDamager.values().removeIf(damager -> damager.equals(playerId));
        lastDamageTime.remove(playerId);
        
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
        
        UUID killerId = lastDamager.get(player.getUniqueId());
        Long damageTime = lastDamageTime.get(player.getUniqueId());
        
        if (killerId != null && damageTime != null && 
            (System.currentTimeMillis() - damageTime) <= 5000) {
            Player killer = Bukkit.getPlayer(killerId);
            if (killer != null && killer.isOnline() && !killer.equals(player)) {
                SoundUtils.killed(killer);
                
                PlayerStats killerStats = PlayerStats.getStats(killer.getUniqueId());
                if (!hasBed) {
                    killerStats.addFinalKill();
                } else {
                    killerStats.addKill();
                }
            }
        }
        
        lastDamager.remove(player.getUniqueId());
        lastDamageTime.remove(player.getUniqueId());
        
        deathManager.prepareForDeath(player);
        
        if (hasBed) {
            deathManager.setRespawning(player, true);
            
            Location respawningLocation = respawnManager.getRespawningLocation();
            if (respawningLocation != null) {
                player.teleport(respawningLocation);
            }
            
            player.sendMessage("§e你将在§c5§e秒后重生！");
            
            respawnManager.startRespawnCountdown(player, 5);
        } else {
            makeSpectator(player);
            INGameTitle.show(player, "§c你死了！", "§7你现在是观察者！", 5);
            player.sendMessage("§c你已被淘汰！");
            
            if (Plugin.getInstance().getScoreboardManager() != null) {
                Plugin.getInstance().getScoreboardManager().forceUpdateAll();
            }
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            checkTeamElimination(team);
        }, 10L);
    }
    
    private void makeSpectator(Player player) {
        // deathManager.setRespawning(player, true);
        spectatorManager.setSpectator(player, true);
        
        player.setAllowFlight(true);
        player.setFlying(true);
        
        RespawnPacketHandler.hidePlayer(player);
        
        TabListManager tabListManager = Plugin.getInstance().getTabListManager();
        if (tabListManager != null) {
            tabListManager.setTemporarySpectator(player, true);
        }
        
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
        
        if (eliminatedTeams.contains(team)) {
            return;
        }
        
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        Set<UUID> teamPlayers = teamManager.getTeamPlayers(team);
        
        for (UUID playerId : teamPlayers) {
            Player teamPlayer = Bukkit.getPlayer(playerId);
            if (teamPlayer != null && teamPlayer.isOnline()) {
                if (spectatorManager.isSpectator(playerId)) {
                    continue;
                }
                if (deathManager.isRespawning(playerId)) {
                    return;
                }
                return;
            }
        }
        
        eliminatedTeams.add(team);
        
        String teamColor = bedManager.getTeamChatColor(team);
        String teamName = bedManager.getTeamDisplayName(team);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f团灭 > " + teamColor + teamName + " §7已被淘汰！");
        Bukkit.broadcastMessage("");
        
        checkVictory();
    }
    
    public void checkTeamEliminationDelayed(String team) {
        checkTeamElimination(team);
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
                    if (!spectatorManager.isSpectator(playerId)) {
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
                INGameTitle.show(player, "§6胜利！", "", 5, 0, 0);
            }
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!winningPlayers.contains(player.getUniqueId())) {
                INGameTitle.show(player, "§c游戏结束！", "", 5, 0, 0);
            }
        }
        
        GameManager.getInstance().setStatus(GameStatus.ENDING);
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
        
        if (!plugin.getConfig().getBoolean("game.friendly_fire", false)) {
            Player attacker = null;
            Player victim = null;
            
            if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
                attacker = (Player) event.getDamager();
                victim = (Player) event.getEntity();
            } else if (event.getDamager() instanceof Projectile && event.getEntity() instanceof Player) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    attacker = (Player) projectile.getShooter();
                    victim = (Player) event.getEntity();
                }
            }
            
            if (attacker != null && victim != null) {
                TeamManager teamManager = GameManager.getInstance().getTeamManager();
                String attackerTeam = teamManager.getPlayerTeam(attacker);
                String victimTeam = teamManager.getPlayerTeam(victim);
                
                if (attackerTeam != null && attackerTeam.equals(victimTeam)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        PlayerDeathManager deathManager = GameManager.getInstance().getPlayerDeathManager();
        
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (deathManager.isInvulnerable(attacker.getUniqueId())) {
                deathManager.removeInvulnerable(attacker.getUniqueId());
                attacker.removePotionEffect(PotionEffectType.GLOWING);
            }
            
            if (deathManager.isRespawning(attacker.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            
            if (deathManager.isRespawning(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            
            if (deathManager.isInvulnerable(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                lastDamager.put(victim.getUniqueId(), attacker.getUniqueId());
                lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                if (projectile.getShooter() instanceof Player) {
                    Player shooter = (Player) projectile.getShooter();
                    lastDamager.put(victim.getUniqueId(), shooter.getUniqueId());
                    lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
                }
            }
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
            INGameTitle.cancelAll();
            lastDamager.clear();
            lastDamageTime.clear();
            eliminatedTeams.clear();
        }
    }
    
    private void handleReconnectDeath(Player player, String team) {
        deathManager.prepareForDeath(player);
        deathManager.setRespawning(player, true);
        
        Location respawningLocation = respawnManager.getRespawningLocation();
        if (respawningLocation != null) {
            player.teleport(respawningLocation);
        }
        
        player.sendMessage("§e你将在§c10§e秒后重生！");
        
        respawnManager.startRespawnCountdown(player, 10);
    }
}
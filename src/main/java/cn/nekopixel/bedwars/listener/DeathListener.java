package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.config.ConfigLoader;
import cn.nekopixel.bedwars.game.BedManager;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.packet.RespawnPacketHandler;
import cn.nekopixel.bedwars.tab.TabListManager;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import cn.nekopixel.bedwars.game.GameStatusChange;

import java.util.*;

public class DeathListener implements Listener {
    private final Main plugin;
    private final Set<UUID> respawningPlayers = new HashSet<>();
    private final Set<UUID> spectatorPlayers = new HashSet<>();
    private final java.util.Map<UUID, DeathState> disconnectedDeathStates = new HashMap<>();
    private static DeathListener instance;
    
    public DeathListener(Main plugin) {
        this.plugin = plugin;
        instance = this;
    }
    
    public static DeathListener getInstance() {
        return instance;
    }
    
    private static class DeathState {
        final boolean isSpectator;
        final String team;
        final long disconnectTime;
        
        DeathState(boolean isSpectator, String team) {
            this.isSpectator = isSpectator;
            this.team = team;
            this.disconnectTime = System.currentTimeMillis();
        }
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
        UUID playerId = joiningPlayer.getUniqueId();
        
        for (UUID respawningPlayerId : respawningPlayers) {
            Player respawningPlayer = Bukkit.getPlayer(respawningPlayerId);
            if (respawningPlayer != null && respawningPlayer.isOnline()) {
                joiningPlayer.hidePlayer(plugin, respawningPlayer);
            }
        }
        
        if (disconnectedDeathStates.containsKey(playerId) &&
            GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            
            DeathState deathState = disconnectedDeathStates.remove(playerId);
            
            if (deathState.team != null) {
                BedManager bedManager = GameManager.getInstance().getBedManager();
                boolean hasBed = bedManager.hasBed(deathState.team);
                
                if (!deathState.isSpectator) {
                    if (hasBed) {
                        handleReconnectDeath(joiningPlayer, deathState.team);
                    } else {
                        restoreSpectatorState(joiningPlayer);
                    }
                } else {
                    restoreSpectatorState(joiningPlayer);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (respawningPlayers.contains(playerId)) {
            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            String team = teamManager.getPlayerTeam(player);
            boolean isSpectator = spectatorPlayers.contains(playerId);
            
            disconnectedDeathStates.put(playerId, new DeathState(isSpectator, team));
            
            respawningPlayers.remove(playerId);
            player.setFlying(false);
            player.setAllowFlight(false);
            RespawnPacketHandler.showPlayer(player);
            
            TabListManager tabListManager = Plugin.getInstance().getTabListManager();
            if (tabListManager != null) {
                tabListManager.setTemporarySpectator(player, false);
            }
        } else if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            String team = teamManager.getPlayerTeam(player);
            
            if (team != null && !spectatorPlayers.contains(playerId)) {
                disconnectedDeathStates.put(playerId, new DeathState(false, team));
            }
        }
        
        spectatorPlayers.remove(playerId);
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
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        String team = teamManager.getPlayerTeam(player);
        
        if (team == null) {
            plugin.getLogger().warning("玩家 " + player.getName() + " 没有队伍！");
            return;
        }
        
        BedManager bedManager = GameManager.getInstance().getBedManager();
        boolean hasBed = bedManager.hasBed(team);
        
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        
        if (hasBed) {
            respawningPlayers.add(player.getUniqueId());
            
            player.setAllowFlight(true);
            player.setFlying(true);
            
            Location respawningLocation = getRespawningLocation();
            if (respawningLocation != null) {
                player.teleport(respawningLocation);
            }
            
            player.sendTitle("§c你死了！", "§e你将在§c5§e秒后重生！", 0, 70, 0);
            player.sendMessage("§e你将在§c5§e秒后重生！");
            
            RespawnPacketHandler.hidePlayer(player);
            
            TabListManager tabListManager = Plugin.getInstance().getTabListManager();
            if (tabListManager != null) {
                tabListManager.setTemporarySpectator(player, true);
            }
            
            new BukkitRunnable() {
                int countdown = 5;
                
                @Override
                public void run() {
                    countdown--;
                    
                    if (countdown > 0) {
                        player.sendTitle("§c你死了！", "§e你将在§c" + countdown + "§e秒后重生！", 0, 40, 0);
                        player.sendMessage("§e你将在§c" + countdown + "§e秒后重生！");
                    } else {
                        respawnPlayer(player);
                        respawningPlayers.remove(player.getUniqueId());
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
        } else {
            respawningPlayers.add(player.getUniqueId());
            spectatorPlayers.add(player.getUniqueId());
            
            player.setAllowFlight(true);
            player.setFlying(true);

            Location respawningLocation = getRespawningLocation();
            if (respawningLocation != null) {
                player.teleport(respawningLocation);
            }

            RespawnPacketHandler.hidePlayer(player);
            
            TabListManager tabListManager = Plugin.getInstance().getTabListManager();
            if (tabListManager != null) {
                tabListManager.setTemporarySpectator(player, true);
            }

            player.sendTitle("§c你死了！", "§7你现在是观察者！", 0, 70, 0);
        }
        
        checkTeamElimination(team);
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
                if (!respawningPlayers.contains(playerId) && !spectatorPlayers.contains(playerId)) {
                    return;
                }
            }
        }
        
        String teamColor = getTeamChatColor(team);
        String teamName = getTeamDisplayName(team);
        
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
                    if (!respawningPlayers.contains(playerId) && !spectatorPlayers.contains(playerId)) {
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
        
        String teamColor = getTeamChatColor(winningTeam);
        String teamName = getTeamDisplayName(winningTeam);
        
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
    
    private String getTeamChatColor(String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        if (bedManager != null) {
            return bedManager.getTeamChatColor(team);
        }
        
        return switch (team.toLowerCase()) {
            case "red" -> "§c";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "yellow" -> "§e";
            case "aqua" -> "§b";
            case "white" -> "§f";
            case "pink" -> "§d";
            case "gray" -> "§7";
            default -> "§7";
        };
    }
    
    private String getTeamDisplayName(String team) {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        if (bedManager != null) {
            return bedManager.getTeamDisplayName(team);
        }
        
        return switch (team.toLowerCase()) {
            case "red" -> "红队";
            case "blue" -> "蓝队";
            case "green" -> "绿队";
            case "yellow" -> "黄队";
            case "aqua" -> "青队";
            case "white" -> "白队";
            case "pink" -> "粉队";
            case "gray" -> "灰队";
            default -> team;
        };
    }
    
    private void respawnPlayer(Player player) {
        RespawnPacketHandler.showPlayer(player);
        
        player.setFlying(false);
        player.setAllowFlight(false);
        
        TabListManager tabListManager = Plugin.getInstance().getTabListManager();
        if (tabListManager != null) {
            tabListManager.setTemporarySpectator(player, false);
        }
        
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
            player.sendTitle("§a已重生！", "", 10, 50, 20);
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
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (spectatorPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(event.getBlock().getLocation(), event.getBlock().getBlockData());
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (spectatorPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (spectatorPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (spectatorPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (respawningPlayers.contains(attacker.getUniqueId()) || spectatorPlayers.contains(attacker.getUniqueId())) {
                event.setCancelled(true);
            }
        }
        
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (respawningPlayers.contains(victim.getUniqueId()) || spectatorPlayers.contains(victim.getUniqueId())) {
                event.setCancelled(true);
            }
        }
        
        if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                if (respawningPlayers.contains(shooter.getUniqueId()) || spectatorPlayers.contains(shooter.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            if (respawningPlayers.contains(shooter.getUniqueId()) || spectatorPlayers.contains(shooter.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.RESETTING) {
            for (UUID playerId : new HashSet<>(respawningPlayers)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    RespawnPacketHandler.showPlayer(player);
                }
                respawningPlayers.remove(playerId);
            }
            spectatorPlayers.clear();
            disconnectedDeathStates.clear();
            
            TabListManager tabListManager = Plugin.getInstance().getTabListManager();
            if (tabListManager != null) {
                tabListManager.clearTemporarySpectators();
            }
        }
    }
    
    private void handleReconnectDeath(Player player, String team) {
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
        
        RespawnPacketHandler.hidePlayer(player);
        
        TabListManager tabListManager = Plugin.getInstance().getTabListManager();
        if (tabListManager != null) {
            tabListManager.setTemporarySpectator(player, true);
        }
        
        player.sendTitle("§c你死了！", "§e你将在§c10§e秒后重生！", 0, 70, 0);
        player.sendMessage("§e你将在§c10§e秒后重生！");
        
        new BukkitRunnable() {
            int countdown = 10;
            
            @Override
            public void run() {
                countdown--;
                
                if (countdown > 0) {
                    player.sendTitle("§c你死了！", "§e你将在§c" + countdown + "§e秒后重生！", 0, 40, 0);
                    player.sendMessage("§e你将在§c" + countdown + "§e秒后重生！");
                } else {
                    respawnPlayer(player);
                    respawningPlayers.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void restoreSpectatorState(Player player) {
        respawningPlayers.add(player.getUniqueId());
        spectatorPlayers.add(player.getUniqueId());
        
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
        
        RespawnPacketHandler.hidePlayer(player);
        
        TabListManager tabListManager = Plugin.getInstance().getTabListManager();
        if (tabListManager != null) {
            tabListManager.setTemporarySpectator(player, true);
        }
        
        player.sendMessage("§c你现在是观察者！");
    }
} 
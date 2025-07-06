package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.spawner.Diamond;
import cn.nekopixel.bedwars.spawner.Emerald;
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.utils.SoundUtils;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

public class EventManager {
    private final Main plugin;
    private final Diamond diamondSpawner;
    private final Emerald emeraldSpawner;
    private BukkitRunnable timerTask;
    private int gameTimeSeconds = 0;
    
    private boolean diamondLevel2Upgraded = false;
    private boolean diamondLevel3Upgraded = false;
    private boolean emeraldLevel2Upgraded = false;
    private boolean emeraldLevel3Upgraded = false;
    
    private boolean bedDestructionStarted = false;
    private boolean suddenDeathStarted = false;
    private boolean gameEndingStarted = false;
    
    private int bedDestructionCountdown = 300;
    private int suddenDeathCountdown = 600;
    private int gameEndingCountdown = 300;
    
    private FileConfiguration chattingConfig;

    public EventManager(Main plugin, Diamond diamondSpawner, Emerald emeraldSpawner) {
        this.plugin = plugin;
        this.diamondSpawner = diamondSpawner;
        this.emeraldSpawner = emeraldSpawner;
        loadChattingConfig();
    }
    
    private void loadChattingConfig() {
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "chatting.yml");
            if (file.exists()) {
                chattingConfig = YamlConfiguration.loadConfiguration(file);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("无法加载 chatting.yml: " + e.getMessage());
        }
    }

    public void start() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        gameTimeSeconds = 0;
        diamondLevel2Upgraded = false;
        diamondLevel3Upgraded = false;
        emeraldLevel2Upgraded = false;
        emeraldLevel3Upgraded = false;
        
        if (diamondSpawner instanceof Diamond) {
            Diamond diamond = (Diamond) diamondSpawner;
            int currentLevel = diamond.getLevel();
            if (currentLevel >= 2) {
                diamondLevel2Upgraded = true;
            }
            if (currentLevel >= 3) {
                diamondLevel3Upgraded = true;
            }
        }

        if (emeraldSpawner instanceof Emerald) {
            Emerald emerald = (Emerald) emeraldSpawner;
            int currentLevel = emerald.getLevel();
            if (currentLevel >= 2) {
                emeraldLevel2Upgraded = true;
            }
            if (currentLevel >= 3) {
                emeraldLevel3Upgraded = true;
            }
        }
        
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameTimeSeconds++;
                checkEvents();
            }
        };
        timerTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        gameTimeSeconds = 0;
        
        bedDestructionStarted = false;
        suddenDeathStarted = false;
        gameEndingStarted = false;
        bedDestructionCountdown = 300;
        suddenDeathCountdown = 600;
        gameEndingCountdown = 300;
    }

    private void checkEvents() {
        int minutes = gameTimeSeconds / 60;
        int seconds = gameTimeSeconds % 60;
        
        if (seconds == 0) {
            switch (minutes) {
                case 5: // 5:00 - 钻石点 II
                    if (!diamondLevel2Upgraded) {
                        diamondSpawner.upgrade();
                        diamondLevel2Upgraded = true;
                    }
                    break;
                case 10: // 10:00 - 绿宝石点 II
                    if (!emeraldLevel2Upgraded) {
                        emeraldSpawner.upgrade();
                        emeraldLevel2Upgraded = true;
                    }
                    break;
                case 12: // 12:00 - 钻石点 III
                    if (!diamondLevel3Upgraded) {
                        diamondSpawner.upgrade();
                        diamondLevel3Upgraded = true;
                    }
                    break;
                case 15: // 15:00 - 绿宝石点 III
                    if (!emeraldLevel3Upgraded) {
                        emeraldSpawner.upgrade();
                        emeraldLevel3Upgraded = true;
                    }
                    break;
            }
        }
        
        if (emeraldLevel3Upgraded && !bedDestructionStarted) {
            bedDestructionStarted = true;
        }

        if (bedDestructionStarted && !suddenDeathStarted) {
            bedDestructionCountdown--;
            
            if (bedDestructionCountdown <= 0) {
                executeAllBedsDestruction();
                suddenDeathStarted = true;
            }
        }
        
        if (suddenDeathStarted && !gameEndingStarted) {
            suddenDeathCountdown--;
            
            if (suddenDeathCountdown <= 0) {
                executeSuddenDeath();
                gameEndingStarted = true;
            }
        }
        
        if (gameEndingStarted) {
            gameEndingCountdown--;
            
            if (gameEndingCountdown <= 0) {
                executeGameEnd();
            }
        }
    }
    
    public void onDiamondUpgraded(int level) {
        if (level == 2) {
            diamondLevel2Upgraded = true;
        } else if (level == 3) {
            diamondLevel3Upgraded = true;
        }
    }
    
    public void onEmeraldUpgraded(int level) {
        if (level == 2) {
            emeraldLevel2Upgraded = true;
        } else if (level == 3) {
            emeraldLevel3Upgraded = true;
        }
    }
    
    public NextEvent getNextEvent() {
        int elapsedMinutes = gameTimeSeconds / 60;
        
        if (bedDestructionStarted && !suddenDeathStarted) {
            return new NextEvent("床自毁", bedDestructionCountdown);
        }
        
        if (suddenDeathStarted && !gameEndingStarted) {
            return new NextEvent("绝杀模式", suddenDeathCountdown);
        }
        
        if (gameEndingStarted) {
            return new NextEvent("游戏结束", gameEndingCountdown);
        }
        
        if (!diamondLevel2Upgraded && elapsedMinutes < 5) {
            return new NextEvent("钻石生成点II级", 5 * 60 - gameTimeSeconds);
        }
        
        if (!emeraldLevel2Upgraded && elapsedMinutes < 10) {
            return new NextEvent("绿宝石生成点II级", 10 * 60 - gameTimeSeconds);
        }
        
        if (!diamondLevel3Upgraded && elapsedMinutes < 12) {
            return new NextEvent("钻石生成点III级", 12 * 60 - gameTimeSeconds);
        }
        
        if (!emeraldLevel3Upgraded && elapsedMinutes < 15) {
            return new NextEvent("绿宝石生成点III级", 15 * 60 - gameTimeSeconds);
        }

        return new NextEvent("游戏结束", -1);
    }
    
    private void executeAllBedsDestruction() {
        BedManager bedManager = GameManager.getInstance().getBedManager();
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        Map mapSetup = Plugin.getInstance().getMapSetup();
        
        if (mapSetup == null) return;
        
        Set<String> teams = teamManager.getConfigTeams();
        
        for (String team : teams) {
            if (bedManager.hasBed(team)) {
                Location bedLoc = mapSetup.getBedLocation(team);
                if (bedLoc != null) {
                    Block bedBlock = bedLoc.getBlock();
                    
                    if (bedBlock.getType().name().endsWith("_BED")) {
                        bedBlock.setType(Material.AIR);
                        
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x == 0 && z == 0) continue;
                                Block relative = bedBlock.getRelative(x, 0, z);
                                if (relative.getType().name().endsWith("_BED")) {
                                    relative.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
                
                bedManager.getTeamBeds().put(team.toLowerCase(), false);
            }
        }
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§f床自毁 §7> §f所有的床均已被破坏！");
        Bukkit.broadcastMessage("");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            INGameTitle.show(player, "§c床已被破坏！", "§7所有的床均已被破坏！", 5, 10, 20);
            SoundUtils.yourBedDestroyed(player);
        }
    }
    
    private void executeSuddenDeath() {
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        RespawnManager respawnManager = new RespawnManager(plugin, GameManager.getInstance().getPlayerDeathManager());
        Location respawnLoc = respawnManager.getRespawningLocation();
        
        if (respawnLoc == null) {
            plugin.getLogger().warning("未找到 respawning 位置，无法生成末影龙");
            return;
        }
        
        Location dragonLoc = respawnLoc.clone().add(0, -25, 0);
        
        Set<String> teams = teamManager.getConfigTeams();
        for (String team : teams) {
            List<Player> alivePlayers = teamManager.getAlivePlayersInTeam(team);
            
            if (!alivePlayers.isEmpty()) {
                EnderDragon dragon = (EnderDragon) dragonLoc.getWorld().spawnEntity(dragonLoc, EntityType.ENDER_DRAGON);
                
                String tablistTeamName = getTablistTeamName(team);
                dragon.setCustomName(getTeamChatColor(team) + tablistTeamName + "队末影龙");
                dragon.setCustomNameVisible(true);
                dragon.setPhase(EnderDragon.Phase.CIRCLING);
                
                setupDragonAI(dragon, team);
            }
        }
    }
    
    private void setupDragonAI(EnderDragon dragon, String ownerTeam) {
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    this.cancel();
                    return;
                }
                
                Player nearestEnemy = null;
                double nearestDistance = Double.MAX_VALUE;
                
                for (Player player : dragon.getWorld().getPlayers()) {
                    if (!player.isOnline() || player.isDead()) continue;
                    
                    String playerTeam = teamManager.getPlayerTeam(player);
                    if (playerTeam == null || playerTeam.equalsIgnoreCase(ownerTeam)) continue;
                    
                    if (GameManager.getInstance().getSpectatorManager().isSpectator(player)) continue;
                    
                    double distance = dragon.getLocation().distance(player.getLocation());
                    if (distance < nearestDistance && distance < 160) {
                        nearestDistance = distance;
                        nearestEnemy = player;
                    }
                }
                
                if (nearestEnemy != null) {
                    Location dragonLoc = dragon.getLocation();
                    Location playerLoc = nearestEnemy.getLocation();
                    
                    double deltaX = playerLoc.getX() - dragonLoc.getX();
                    double deltaZ = playerLoc.getZ() - dragonLoc.getZ();
                    float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
                    
                    dragonLoc.setYaw(yaw);
                    dragon.teleport(dragonLoc);
                    
                    if (nearestDistance < 50) {
                        if (dragon.getPhase() != EnderDragon.Phase.STRAFING &&
                            dragon.getPhase() != EnderDragon.Phase.BREATH_ATTACK) {
                            dragon.setPhase(EnderDragon.Phase.STRAFING);
                        }
                        
                        if (Math.random() < 0.1) {
                            Location fireballLoc = dragonLoc.clone().add(dragonLoc.getDirection().multiply(5));
                            dragon.getWorld().spawn(fireballLoc, org.bukkit.entity.DragonFireball.class, fireball -> {
                                fireball.setDirection(playerLoc.toVector().subtract(fireballLoc.toVector()).normalize());
                                fireball.setShooter(dragon);
                            });
                        }
                    } else {
                        dragon.setPhase(EnderDragon.Phase.CIRCLING);
                    }
                } else {
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    private void executeGameEnd() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l游戏结束！");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e本局游戏平局！");
        Bukkit.broadcastMessage("");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            INGameTitle.show(player, "§c游戏结束！", "", 5, 0, 0);
        }
        
        GameManager.getInstance().setStatus(GameStatus.ENDING);
    }
    
    private String getTeamChatColor(String team) {
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
    
    private String getTablistTeamName(String team) {
        if (chattingConfig != null) {
            String configPath = "tablist.team_names." + team.toLowerCase();
            if (chattingConfig.contains(configPath)) {
                return chattingConfig.getString(configPath);
            }
        }
        
        return switch (team.toLowerCase()) {
            case "red" -> "红";
            case "blue" -> "蓝";
            case "green" -> "绿";
            case "yellow" -> "黄";
            case "aqua" -> "青";
            case "white" -> "白";
            case "pink" -> "粉";
            case "gray" -> "灰";
            default -> team;
        };
    }

    public static class NextEvent {
        private final String name;
        private final int secondsRemaining;
        
        public NextEvent(String name, int secondsRemaining) {
            this.name = name;
            this.secondsRemaining = secondsRemaining;
        }
        
        public String getName() {
            return name;
        }
        
        public int getSecondsRemaining() {
            return secondsRemaining;
        }
        
        public String getFormattedTime() {
            if (secondsRemaining < 0) return "N/A";
            int minutes = secondsRemaining / 60;
            int seconds = secondsRemaining % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}

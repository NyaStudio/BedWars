package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.broadcast.BroadcastManager;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.spawner.Diamond;
import cn.nekopixel.bedwars.spawner.Emerald;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class EventManager {
    private final Main plugin;
    private final Diamond diamondSpawner;
    private final Emerald emeraldSpawner;
    private final DragonManager dragonManager;
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

    public EventManager(Main plugin, Diamond diamondSpawner, Emerald emeraldSpawner) {
        this.plugin = plugin;
        this.diamondSpawner = diamondSpawner;
        this.emeraldSpawner = emeraldSpawner;
        this.dragonManager = new DragonManager(plugin);
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
        
        String romanLevel = getLevelRoman(level);
        BroadcastManager.getInstance().diamondUpgrade(romanLevel);
    }
    
    public void onEmeraldUpgraded(int level) {
        if (level == 2) {
            emeraldLevel2Upgraded = true;
        } else if (level == 3) {
            emeraldLevel3Upgraded = true;
        }
        
        String romanLevel = getLevelRoman(level);
        BroadcastManager.getInstance().emeraldUpgrade(romanLevel);
    }
    
    private String getLevelRoman(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
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
                        bedBlock.setType(Material.AIR, false);
                        
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                if (x == 0 && z == 0) continue;
                                Block relative = bedBlock.getRelative(x, 0, z);
                                if (relative.getType().name().endsWith("_BED")) {
                                    relative.setType(Material.AIR, false);
                                }
                            }
                        }
                    }
                }
                
                bedManager.getTeamBeds().put(team.toLowerCase(), false);
            }
        }
        
        BroadcastManager.getInstance().allBedsDestroyed();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            INGameTitle.show(player, "§c床已被破坏！", "§7所有的床均已被破坏！", 5, 10, 20);
            SoundUtils.yourBedDestroyed(player);
        }
    }
    
    private void executeSuddenDeath() {
        RespawnManager respawnManager = new RespawnManager(plugin, GameManager.getInstance().getPlayerDeathManager());
        Location respawnLoc = respawnManager.getRespawningLocation();
        
        dragonManager.spawnDragons(respawnLoc);
    }

    private void executeGameEnd() {
        BroadcastManager.getInstance().gameDraw();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            INGameTitle.show(player, "§c游戏结束！", "", 5, 0, 0);
        }
        
        GameManager.getInstance().setStatus(GameStatus.ENDING);
    }
    
    public DragonManager getDragonManager() {
        return dragonManager;
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

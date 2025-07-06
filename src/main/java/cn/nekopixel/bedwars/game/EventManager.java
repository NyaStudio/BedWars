package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.spawner.Diamond;
import cn.nekopixel.bedwars.spawner.Emerald;
import org.bukkit.scheduler.BukkitRunnable;

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

    public EventManager(Main plugin, Diamond diamondSpawner, Emerald emeraldSpawner) {
        this.plugin = plugin;
        this.diamondSpawner = diamondSpawner;
        this.emeraldSpawner = emeraldSpawner;
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

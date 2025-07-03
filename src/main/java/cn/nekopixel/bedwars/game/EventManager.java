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
                    diamondSpawner.upgrade();
                    break;
                case 10: // 10:00 - 绿宝石点 II
                    emeraldSpawner.upgrade();
                    break;
                case 12: // 12:00 - 钻石点 III
                    diamondSpawner.upgrade();
                    break;
                case 15: // 15:00 - 绿宝石点 III
                    emeraldSpawner.upgrade();
                    break;
            }
        }
    }
    
    public NextEvent getNextEvent() {
        int elapsedMinutes = gameTimeSeconds / 60;
        
        if (elapsedMinutes < 5) {
            return new NextEvent("钻石生成点II级", 5 * 60 - gameTimeSeconds);
        } else if (elapsedMinutes < 10) {
            return new NextEvent("绿宝石生成点II级", 10 * 60 - gameTimeSeconds);
        } else if (elapsedMinutes < 12) {
            return new NextEvent("钻石生成点III级", 12 * 60 - gameTimeSeconds);
        } else if (elapsedMinutes < 15) {
            return new NextEvent("绿宝石生成点III级", 15 * 60 - gameTimeSeconds);
        } else {
            return new NextEvent("游戏结束", -1);
        }
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

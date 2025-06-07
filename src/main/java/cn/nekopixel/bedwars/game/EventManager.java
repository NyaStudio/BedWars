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
    private int gameTime = 0; // 游戏时间（分钟）

    public EventManager(Main plugin, Diamond diamondSpawner, Emerald emeraldSpawner) {
        this.plugin = plugin;
        this.diamondSpawner = diamondSpawner;
        this.emeraldSpawner = emeraldSpawner;
    }

    public void start() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        gameTime = 0;
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameTime++;
                checkEvents();
            }
        };
        timerTask.runTaskTimer(plugin, 1200L, 1200L); // 20 ticks = 1 sec, 1200 ticks = 1 min
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        gameTime = 0;
    }

    private void checkEvents() {
        switch (gameTime) {
            case 6: // 6:00 - 钻石点 II
                diamondSpawner.upgrade();
                break;
            case 9: // 9:00 - 绿宝石点 II
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

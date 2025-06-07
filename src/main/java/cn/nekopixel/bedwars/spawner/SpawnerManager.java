package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SpawnerManager implements Listener {
    private final Main plugin;
    private final Iron ironSpawner;
    private final Gold goldSpawner;
    private final Diamond diamondSpawner;
    private final Emerald emeraldSpawner;

    public SpawnerManager(Main plugin) {
        this.plugin = plugin;
        this.ironSpawner = new Iron(plugin);
        this.goldSpawner = new Gold(plugin);
        this.diamondSpawner = new Diamond(plugin);
        this.emeraldSpawner = new Emerald(plugin);
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.INGAME) {
            startSpawners();
        } else {
            stopSpawners();
        }
    }

    private void startSpawners() {
        ironSpawner.start();
        goldSpawner.start();
        diamondSpawner.start();
        emeraldSpawner.start();
    }

    private void stopSpawners() {
        ironSpawner.stop();
        goldSpawner.stop();
        diamondSpawner.stop();
        emeraldSpawner.stop();
    }

    public Diamond getDiamondSpawner() {
        return diamondSpawner;
    }

    public Emerald getEmeraldSpawner() {
        return emeraldSpawner;
    }
} 
package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.player.FoodLock;
import cn.nekopixel.bedwars.player.RemoveItems;
import cn.nekopixel.bedwars.spawner.Diamond;
import cn.nekopixel.bedwars.spawner.Emerald;
import cn.nekopixel.bedwars.spawner.SpawnerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class GameManager {
    private static GameManager instance;
    private GameStatus currentStatus;
    private EventManager eventManager;
    private final Main plugin;
    private final SpawnerManager spawnerManager;
    private final RemoveItems removeItems;
    private final FoodLock foodLock;

    private GameManager(Main plugin) {
        this.plugin = plugin;
        this.currentStatus = GameStatus.WAITING;
        this.spawnerManager = new SpawnerManager(plugin);
        this.removeItems = new RemoveItems(plugin);
        this.foodLock = new FoodLock(plugin);
        Bukkit.getPluginManager().registerEvents(spawnerManager, plugin);
    }

    public static GameManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameManager 未初始化");
        }
        return instance;
    }

    public static void initialize(Main plugin) {
        if (instance != null) {
            throw new IllegalStateException("GameManager 初始化过了");
        }
        instance = new GameManager(plugin);
    }

    public GameStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setStatus(GameStatus status) {
        GameStatus oldStatus = this.currentStatus;
        this.currentStatus = status;
        Bukkit.getPluginManager().callEvent(new GameStatusChange(oldStatus, status));
        Bukkit.broadcastMessage(ChatColor.GREEN + "状态已更改为: " + status.name());

        if (status == GameStatus.INGAME) {
            if (eventManager == null) {
                Diamond diamondSpawner = spawnerManager.getDiamondSpawner();
                Emerald emeraldSpawner = spawnerManager.getEmeraldSpawner();
                eventManager = new EventManager(plugin, diamondSpawner, emeraldSpawner);
            }
            eventManager.start();
            removeItems.start();
            foodLock.start();
        } else if (status == GameStatus.ENDING) {
            if (eventManager != null) {
                eventManager.stop();
            }
            removeItems.stop();
            foodLock.stop();
        }
    }

    public boolean isStatus(GameStatus status) {
        return currentStatus == status;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }
} 
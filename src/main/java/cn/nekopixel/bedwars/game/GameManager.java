package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.auth.AuthInterceptor;
import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.player.FoodLock;
import cn.nekopixel.bedwars.player.RemoveItems;
import cn.nekopixel.bedwars.player.PlayerStats;
import cn.nekopixel.bedwars.player.Connection;
import cn.nekopixel.bedwars.spawner.Diamond;
import cn.nekopixel.bedwars.spawner.Emerald;
import cn.nekopixel.bedwars.spawner.SpawnerManager;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.player.NameTag;
import cn.nekopixel.bedwars.listener.WaitingListener;
import cn.nekopixel.bedwars.tab.TabListManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitRunnable;

public class GameManager {
    private static GameManager instance;
    private GameStatus currentStatus;
    private EventManager eventManager;
    private final Main plugin;
    private final SpawnerManager spawnerManager;
    private final RemoveItems removeItems;
    private final FoodLock foodLock;
    private final TeamManager teamManager;
    private final ChatManager chatManager;
    private final NameTag nameTag;
    private final QueueManager queueManager;
    private final BedManager bedManager;
    private final PlayerDeathManager playerDeathManager;
    private final SpectatorManager spectatorManager;

    private GameManager(Main plugin) {
        this.plugin = plugin;
        this.currentStatus = null;
        this.spawnerManager = new SpawnerManager(plugin);
        this.removeItems = new RemoveItems(plugin);
        this.foodLock = new FoodLock(plugin);
        this.teamManager = new TeamManager(plugin);
        this.chatManager = new ChatManager(plugin);
        this.nameTag = new NameTag(plugin);
        this.queueManager = new QueueManager(plugin);
        this.bedManager = new BedManager(plugin);
        this.playerDeathManager = new PlayerDeathManager(plugin);
        this.spectatorManager = new SpectatorManager(plugin);
        
        Bukkit.getPluginManager().registerEvents(spawnerManager, plugin);
        Bukkit.getPluginManager().registerEvents(new WaitingListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(foodLock, plugin);
        Bukkit.getPluginManager().registerEvents(bedManager, plugin);
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
        Connection.initialize(plugin);
        instance = new GameManager(plugin);
    }

    public GameStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setStatus(GameStatus status) {
        if (!AuthInterceptor.checkAuth()) {
            return;
        }
        
        if (this.currentStatus == status) {
            return;
        }

        GameStatus oldStatus = this.currentStatus;
        this.currentStatus = status;
        Bukkit.getPluginManager().callEvent(new GameStatusChange(oldStatus, status));
//        Bukkit.broadcastMessage(ChatColor.GREEN + "状态已更改为: " + status.name());
        
        TabListManager tabListManager = Plugin.getInstance().getTabListManager();
        if (tabListManager != null) {
            tabListManager.onGameStatusChange();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (status == GameStatus.INGAME) {
                player.setGameMode(GameMode.SURVIVAL);
            } else {
                player.setGameMode(GameMode.ADVENTURE);
            }
        }

        if (status == GameStatus.INGAME) {
            Connection.getInstance().recordGamePlayers();
            
            if (eventManager == null) {
                Diamond diamondSpawner = spawnerManager.getDiamondSpawner();
                Emerald emeraldSpawner = spawnerManager.getEmeraldSpawner();
                eventManager = new EventManager(plugin, diamondSpawner, emeraldSpawner);
            }
            eventManager.start();
            removeItems.start();
            teamManager.assignTeams();
            nameTag.startUpdateTask();

            PlayerStats.clearAll();
        } else if (status == GameStatus.ENDING) {
            if (eventManager != null) {
                eventManager.stop();
            }
            removeItems.stop();
            nameTag.stop();
            
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

        } else if (status == GameStatus.RESETTING) {
            Connection.getInstance().clearAuthorizedPlayers();
            
            playerDeathManager.clearAll();
            spectatorManager.clearAll();
            PlayerStats.clearAll();
        }
    }

    public boolean isStatus(GameStatus status) {
        return currentStatus == status;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public NameTag getNameTag() {
        return nameTag;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }
    
    public BedManager getBedManager() {
        return bedManager;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    public PlayerDeathManager getPlayerDeathManager() {
        return playerDeathManager;
    }
    
    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }
} 
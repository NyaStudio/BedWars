package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.utils.LocationUtils;
import cn.nekopixel.bedwars.utils.SoundUtils;
import cn.nekopixel.bedwars.utils.INGameTitle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class QueueManager implements Listener {
    private final Main plugin;
    private Location joinLocation;
    private BukkitTask countdownTask;
    private int seconds;
    private boolean isCountingDown;

    public QueueManager(Main plugin) {
        this.plugin = plugin;
        this.isCountingDown = false;
        this.seconds = 0;
        loadJoinLocation();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadJoinLocation() {
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup == null) {
            plugin.getLogger().warning("地图配置未加载，无法设置等待位置");
            return;
        }

        ConfigurationSection joinSection = mapSetup.getMapConfig().getConfigurationSection("join");
        if (joinSection == null) {
            plugin.getLogger().warning("地图配置中未找到等待位置");
            return;
        }

        joinLocation = Location.deserialize(joinSection.getValues(false));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (GameManager.getInstance().isStatus(GameStatus.WAITING)) {
            Player player = event.getPlayer();
            if (joinLocation != null) {
                Location safeLocation = LocationUtils.findSafeLocation(joinLocation, 3);
                player.teleport(safeLocation);
            }
        }
    }

    public void checkAndUpdateCountdown() {
        int playerCount = Bukkit.getOnlinePlayers().size();
        String mode = plugin.getConfig().getString("game.mode", "4s").toLowerCase();
        
        int requiredSeconds = getRequiredSeconds(mode, playerCount);
        
        if (requiredSeconds == -1) {
            if (isCountingDown) {
                stopCountdown();
            }
        } else {
            if (!isCountingDown) {
                startCountdown(requiredSeconds);
            } else if (requiredSeconds < seconds) {
                seconds = requiredSeconds;
            }
        }
    }
    
    private int getRequiredSeconds(String mode, int playerCount) {
        switch (mode) {
            case "solo":
                if (playerCount < 4) return -1;
                if (playerCount >= 8) return 5;
                if (playerCount >= 6) return 10;
                return 30;
                
            case "double":
                if (playerCount < 8) return -1;
                if (playerCount >= 16) return 5;
                if (playerCount >= 12) return 10;
                return 30;
                
            case "3s":
                if (playerCount < 8) return -1;
                if (playerCount >= 16) return 5;
                if (playerCount >= 12) return 5;
                return 30;
                
            case "4s":
                if (playerCount < 8) return -1;
                if (playerCount >= 16) return 5;
                if (playerCount >= 12) return 10;
                return 30;
                
            case "4v4":
                if (playerCount < 4) return -1;
                if (playerCount >= 8) return 5;
                if (playerCount >= 6) return 10;
                return 30;
                
            default:
                return -1;
        }
    }
    
    private void startCountdown(int initialSeconds) {
        if (isCountingDown) return;
        
        isCountingDown = true;
        seconds = initialSeconds;
        
        if (seconds <= 5 || seconds == 10 || seconds == 15 || seconds == 30) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.YELLOW + "游戏将在 " + 
                    ChatColor.RED + seconds + ChatColor.YELLOW + " 秒后开始!");
                
                SoundUtils.countDown(player);
                if (seconds == 10) {
                    INGameTitle.show(player, ChatColor.YELLOW + "10", "", 0, 20, 0);
                } else if (seconds <= 5) {
                    String color = (seconds <= 3) ? ChatColor.RED.toString() : ChatColor.YELLOW.toString();
                    INGameTitle.show(player, color + seconds, "", 0, 20, 0);
                }
            }
        }
        
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                seconds--;
                
                if (seconds <= 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        INGameTitle.cancel(player);
                    }
                    
                    if (Plugin.getInstance().getGameManager() != null) {
                        Plugin.getInstance().getGameManager().setStatus(GameStatus.INGAME);
                    }
                    cancel();
                    isCountingDown = false;
                    return;
                }
                
                if (seconds <= 5 || seconds == 10 || seconds == 15 || seconds == 30) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(ChatColor.YELLOW + "游戏将在 " + 
                            ChatColor.RED + seconds + ChatColor.YELLOW + " 秒后开始!");
                        
                        SoundUtils.countDown(player);
                        if (seconds == 10) {
                            INGameTitle.show(player, ChatColor.YELLOW + "10", "", 0, 20, 0);
                        } else if (seconds <= 5) {
                            String color = (seconds <= 3) ? ChatColor.RED.toString() : ChatColor.YELLOW.toString();
                            INGameTitle.show(player, color + seconds, "", 0, 20, 0);
                        }
                    }
                }
                
                checkAndUpdateCountdown();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void stopCountdown() {
        if (!isCountingDown) return;
        
        isCountingDown = false;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        if (Plugin.getInstance().getGameManager() != null) {
            Plugin.getInstance().getGameManager().setStatus(GameStatus.WAITING);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.RED + "玩家数量不足，等待更多玩家.....");
            INGameTitle.cancel(player);
            
            INGameTitle.show(player, ChatColor.RED + "等待更多玩家加入.....", "", 2, 0, 0);
            SoundUtils.countDown(player);
        }
    }
    
    public boolean isCountingDown() {
        return isCountingDown;
    }
    
    public int getSeconds() {
        return seconds;
    }
    
    public void stop() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        isCountingDown = false;
        seconds = 0;
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {}

    public Location getJoinLocation() {
        return joinLocation;
    }
} 
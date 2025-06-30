package cn.nekopixel.bedwars.utils;

import cn.nekopixel.bedwars.Main;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class INGameTitle {
    private static final Map<UUID, BukkitTask> activeTitles = new HashMap<>();
    private static Main plugin;
    
    public static void init(Main mainPlugin) {
        plugin = mainPlugin;
    }
    public static void show(Player player, String title, String subtitle, int durationSeconds) {
        show(player, title, subtitle, durationSeconds, 0, 0);
    }
    
    public static void show(Player player, String title, String subtitle, int durationSeconds, int fadeIn, int fadeOut) {
        cancel(player);
        BukkitTask task = new BukkitRunnable() {
            int remainingTicks = durationSeconds * 20;
            
            @Override
            public void run() {
                if (!player.isOnline() || remainingTicks <= 0) {
                    cancel();
                    activeTitles.remove(player.getUniqueId());
                    return;
                }
                
                if (remainingTicks % 5 == 0) {
                    player.sendTitle(title, subtitle, fadeIn, 15, fadeOut);
                }
                
                remainingTicks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        activeTitles.put(player.getUniqueId(), task);
    }

    public static void showDynamic(Player player, TitleProvider titleProvider, TitleProvider subtitleProvider, int durationSeconds) {
        showDynamic(player, titleProvider, subtitleProvider, durationSeconds, 0, 0);
    }
    
    public static void showDynamic(Player player, TitleProvider titleProvider, TitleProvider subtitleProvider, int durationSeconds, int fadeIn, int fadeOut) {
        cancel(player);
        BukkitTask task = new BukkitRunnable() {
            int remainingTicks = durationSeconds * 20;
            
            @Override
            public void run() {
                if (!player.isOnline() || remainingTicks <= 0) {
                    cancel();
                    activeTitles.remove(player.getUniqueId());
                    return;
                }
                
                if (remainingTicks % 5 == 0) {
                    int remainingSeconds = (remainingTicks + 19) / 20;
                    String currentTitle = titleProvider.provide(remainingSeconds);
                    String currentSubtitle = subtitleProvider.provide(remainingSeconds);
                    player.sendTitle(currentTitle, currentSubtitle, fadeIn, 15, fadeOut);
                }
                
                remainingTicks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        activeTitles.put(player.getUniqueId(), task);
    }

    public static void cancel(Player player) {
        BukkitTask task = activeTitles.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public static void cancelAll() {
        activeTitles.values().forEach(BukkitTask::cancel);
        activeTitles.clear();
    }
    
    @FunctionalInterface
    public interface TitleProvider {
        String provide(int remainingSeconds);
    }
}
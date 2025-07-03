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
        
        player.sendTitle(title, subtitle, fadeIn, durationSeconds * 20, fadeOut);
        
        if (durationSeconds > 1) {
            BukkitTask task = new BukkitRunnable() {
                int refreshCount = 0;
                int maxRefreshes = durationSeconds - 1;
                
                @Override
                public void run() {
                    if (!player.isOnline() || refreshCount >= maxRefreshes) {
                        cancel();
                        activeTitles.remove(player.getUniqueId());
                        return;
                    }
                    
                    player.sendTitle(title, subtitle, 0, 30, 0);
                    refreshCount++;
                }
            }.runTaskTimer(plugin, 20L, 20L);
            
            activeTitles.put(player.getUniqueId(), task);
        }
    }

    public static void showDynamic(Player player, TitleProvider titleProvider, TitleProvider subtitleProvider, int durationSeconds, int fadeIn, int fadeOut) {
        cancel(player);
        BukkitTask task = new BukkitRunnable() {
            int remainingTicks = durationSeconds * 20;
            int totalTicks = durationSeconds * 20;
            boolean fadeInSent = false;
            boolean fadeOutSent = false;
            
            @Override
            public void run() {
                if (!player.isOnline() || remainingTicks <= 0) {
                    cancel();
                    activeTitles.remove(player.getUniqueId());
                    return;
                }
                
                int elapsedTicks = totalTicks - remainingTicks;
                int remainingSeconds = (remainingTicks + 19) / 20;
                String currentTitle = titleProvider.provide(remainingSeconds);
                String currentSubtitle = subtitleProvider.provide(remainingSeconds);
                
                // in
                if (!fadeInSent && elapsedTicks == 0) {
                    player.sendTitle(currentTitle, currentSubtitle, fadeIn, fadeIn + 5, 0);
                    fadeInSent = true;
                }
                // stay
                else if (elapsedTicks >= fadeIn && remainingTicks > fadeOut) {
                    player.sendTitle(currentTitle, currentSubtitle, 0, 5, 0);
                }
                // out
                else if (!fadeOutSent && remainingTicks == fadeOut) {
                    player.sendTitle(currentTitle, currentSubtitle, 0, fadeOut, fadeOut);
                    fadeOutSent = true;
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
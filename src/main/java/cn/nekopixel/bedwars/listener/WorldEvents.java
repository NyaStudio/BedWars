package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;

public class WorldEvents implements Listener {
    private final Main plugin;
    private boolean lockTime;
    private int lockedTime;
    private boolean disableWeather;
    private boolean disableDaylightCycle;

    public WorldEvents(Main plugin) {
        this.plugin = plugin;
        loadWorldSettings();
    }

    private void loadWorldSettings() {
        lockTime = plugin.getConfig().getBoolean("world.lock_time", true);
        lockedTime = plugin.getConfig().getInt("world.locked_time", 6000);
        disableWeather = plugin.getConfig().getBoolean("world.disable_weather", true);
        disableDaylightCycle = plugin.getConfig().getBoolean("world.disable_daylight_cycle", true);

        for (World world : plugin.getServer().getWorlds()) {
            if (lockTime) {
                world.setTime(lockedTime);
            }
            if (disableWeather) {
                world.setStorm(false);
                world.setThundering(false);
            }
            if (disableDaylightCycle) {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            }
        }

        if (lockTime) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (World world : Bukkit.getWorlds()) {
                    world.setTime(lockedTime);
                }
            }, 0L, 100L);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        event.setCancelled(true);
    }
} 
package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class QueueManager implements Listener {
    private final Main plugin;
    private Location joinLocation;

    public QueueManager(Main plugin) {
        this.plugin = plugin;
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

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {}

    public Location getJoinLocation() {
        return joinLocation;
    }
} 
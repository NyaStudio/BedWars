package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.commands.CommandManager;
import cn.nekopixel.bedwars.listener.CancelEvents;
import cn.nekopixel.bedwars.listener.WorldEvents;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class Loader {
    private static Map mapSetup;

    public static void registerAllEvents(Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new CancelEvents(), plugin);
        pm.registerEvents(new WorldEvents(), plugin);

        plugin.getLogger().info("功能加载完成！");
    }

    public static void registerCommands(Main plugin) {
        mapSetup = new Map(plugin);
        plugin.getCommand("bw").setExecutor(new CommandManager(plugin));
    }

    public static void reloadAll(Main plugin) {
        try {
            plugin.reloadConfig();
            if (mapSetup != null) {
                mapSetup.reloadMapConfig();
            } else {
                // plugin.getLogger().warning("mapSetup 为空，无法重载地图配置");
                mapSetup = new Map(plugin);
                mapSetup.reloadMapConfig();
            }
            
            if (plugin.getShopManager() != null) {
                plugin.getShopManager().reloadConfigs();
            }
            
            plugin.getLogger().info("所有配置文件已重新加载！");
        } catch (Exception e) {
            plugin.getLogger().severe("重载配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

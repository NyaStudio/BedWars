package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.map.CancelEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class Loader {

    public static void registerAllEvents(Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new CancelEvents(), plugin);

        plugin.getLogger().info("功能加载完成！");
    }
}

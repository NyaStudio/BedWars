package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.commands.CommandManager;
import cn.nekopixel.bedwars.listener.CancelEvents;
import cn.nekopixel.bedwars.listener.ChatListener;
import cn.nekopixel.bedwars.listener.WorldEvents;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.player.NameTag;
import cn.nekopixel.bedwars.player.HealthBar;
import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.tab.TabListManager;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.shop.ShopManager;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.map.MapManager;
import org.bukkit.plugin.PluginManager;

public class Loader {
    private static Map mapSetup;

    public static void registerEvents(org.bukkit.plugin.Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new CancelEvents(plugin), plugin);
        pm.registerEvents(new WorldEvents((Main) plugin), plugin);
        pm.registerEvents(new ChatListener((Main) plugin), plugin);

        plugin.getLogger().info("功能加载完成！");
    }

    public static void registerCommands(Main plugin) {
        mapSetup = new Map(plugin);
        plugin.getCommand("bw").setExecutor(new CommandManager(plugin));
    }

    public static void initializeManagers(Main plugin) {
        Plugin bedWarsPlugin = Plugin.getInstance();
        
        GameManager.initialize(plugin);
        bedWarsPlugin.setGameManager(GameManager.getInstance());

        NPCManager npcManager = new NPCManager(plugin);
        bedWarsPlugin.setNPCManager(npcManager);
        plugin.getServer().getPluginManager().registerEvents(npcManager, plugin);

        ShopManager shopManager = new ShopManager(plugin, npcManager);
        bedWarsPlugin.setShopManager(shopManager);
        plugin.getServer().getPluginManager().registerEvents(shopManager, plugin);

        bedWarsPlugin.setMapSetup(new Map(plugin));
        bedWarsPlugin.setChatManager(new ChatManager(plugin));
        bedWarsPlugin.setTabListManager(new TabListManager(plugin));
        bedWarsPlugin.setNameTag(new NameTag(plugin));

        MapManager mapManager = new MapManager(plugin);
        bedWarsPlugin.setMapManager(mapManager);
        plugin.getServer().getPluginManager().registerEvents(mapManager, plugin);
        mapManager.loadProtectedAreas();

        HealthBar healthBar = new HealthBar(plugin);
        plugin.getServer().getPluginManager().registerEvents(healthBar, plugin);
    }

    public static void reloadAll(Main plugin) {
        try {
            plugin.reloadConfig();
            if (mapSetup != null) {
                mapSetup.reloadMapConfig();
            } else {
                mapSetup = new Map(plugin);
                mapSetup.reloadMapConfig();
            }
            
            Plugin bedWarsPlugin = Plugin.getInstance();
            if (bedWarsPlugin.getShopManager() != null) {
                bedWarsPlugin.getShopManager().reloadConfigs();
            }

            if (bedWarsPlugin.getChatManager() != null) {
                bedWarsPlugin.getChatManager().reloadConfig();
            }
            if (bedWarsPlugin.getTabListManager() != null) {
                bedWarsPlugin.getTabListManager().reloadConfig();
            }
            if (bedWarsPlugin.getGameManager() != null && bedWarsPlugin.getGameManager().getNameTag() != null) {
                bedWarsPlugin.getGameManager().getNameTag().reloadConfig();
            }
            
            if (bedWarsPlugin.getMapManager() != null) {
                bedWarsPlugin.getMapManager().loadProtectedAreas();
            }
            
            plugin.getLogger().info("所有配置文件已重新加载！");
        } catch (Exception e) {
            plugin.getLogger().severe("重载配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

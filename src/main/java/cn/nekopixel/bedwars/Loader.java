package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.commands.CommandManager;
import cn.nekopixel.bedwars.listener.CancelEvents;
import cn.nekopixel.bedwars.listener.ChatListener;
import cn.nekopixel.bedwars.listener.WorldEvents;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.player.NameTag;
import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.tab.TabListManager;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.shop.ShopManager;
import cn.nekopixel.bedwars.game.GameManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class Loader {
    private static Map mapSetup;

    public static void registerEvents(Plugin plugin) {
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
        GameManager.initialize(plugin);
        plugin.setGameManager(GameManager.getInstance());

        NPCManager npcManager = new NPCManager(plugin);
        plugin.setNPCManager(npcManager);
        plugin.getServer().getPluginManager().registerEvents(npcManager, plugin);

        ShopManager shopManager = new ShopManager(plugin, npcManager);
        plugin.setShopManager(shopManager);
        plugin.getServer().getPluginManager().registerEvents(shopManager, plugin);

        plugin.setMapSetup(new Map(plugin));
        plugin.setChatManager(new ChatManager(plugin));
        plugin.setTabListManager(new TabListManager(plugin));
        plugin.setNameTag(new NameTag(plugin));
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

            if (plugin.getChatManager() != null) {
                plugin.getChatManager().reloadConfig();
            }
            if (plugin.getTabListManager() != null) {
                plugin.getTabListManager().reloadConfig();
            }
            if (plugin.getGameManager() != null && plugin.getGameManager().getNameTag() != null) {
                plugin.getGameManager().getNameTag().reloadConfig();
            }
            
            plugin.getLogger().info("所有配置文件已重新加载！");
        } catch (Exception e) {
            plugin.getLogger().severe("重载配置文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

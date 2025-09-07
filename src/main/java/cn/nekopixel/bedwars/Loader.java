package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.broadcast.BroadcastManager;
import cn.nekopixel.bedwars.commands.CommandManager;
import cn.nekopixel.bedwars.commands.ShoutCommand;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.language.LanguageManager;
import cn.nekopixel.bedwars.listener.*;
import cn.nekopixel.bedwars.map.MapManager;
import cn.nekopixel.bedwars.player.Damage;
import cn.nekopixel.bedwars.player.HealthBar;
import cn.nekopixel.bedwars.player.KnockBack;
import cn.nekopixel.bedwars.scoreboard.ScoreboardListener;
import cn.nekopixel.bedwars.scoreboard.ScoreboardManager;
import cn.nekopixel.bedwars.setup.Init;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.shop.ShopManager;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.tab.TabListManager;
import org.bukkit.plugin.PluginManager;

public class Loader {
    private static Map mapSetup;
    public static void registerEvents(org.bukkit.plugin.Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new Damage((Main) plugin), plugin);
        pm.registerEvents(new KnockBack((Main) plugin), plugin);
        pm.registerEvents(new CancelEvents(plugin), plugin);
        pm.registerEvents(new WorldEvents((Main) plugin), plugin);
        pm.registerEvents(new ChatListener((Main) plugin), plugin);
        pm.registerEvents(new PlayerHealth((Main) plugin), plugin);
        pm.registerEvents(new ProtectionListener((Main) plugin), plugin);
        pm.registerEvents(new PotionDrinkListener(), plugin);
        pm.registerEvents(new DeathListener((Main) plugin), plugin);
        pm.registerEvents(new PrefixCacheListener((Main) plugin), plugin);
        pm.registerEvents(new ItemListener((Main) plugin), plugin);
    }

    public static void registerCommands(Main plugin) {
        mapSetup = new Map(plugin);

        plugin.getCommand("bw").setExecutor(new CommandManager(plugin));
        plugin.getCommand("shout").setExecutor(new ShoutCommand(plugin));
    }

    public static void initializeManagers(Main plugin) {
        Plugin bedWarsPlugin = Plugin.getInstance();

        LanguageManager.initialize(plugin);

        mapSetup = new Map(plugin);
        bedWarsPlugin.setMapSetup(mapSetup);

        BroadcastManager.initialize(plugin);
        GameManager.initialize(plugin);
        bedWarsPlugin.setGameManager(GameManager.getInstance());

        NPCManager npcManager = new NPCManager(plugin);
        bedWarsPlugin.setNPCManager(npcManager);
        plugin.getServer().getPluginManager().registerEvents(npcManager, plugin);

        ShopManager shopManager = new ShopManager(plugin, npcManager);
        bedWarsPlugin.setShopManager(shopManager);
        plugin.getServer().getPluginManager().registerEvents(shopManager, plugin);

        bedWarsPlugin.setChatManager(GameManager.getInstance().getChatManager());
        bedWarsPlugin.setTabListManager(new TabListManager(plugin));
        bedWarsPlugin.setNameTag(GameManager.getInstance().getNameTag());

        MapManager mapManager = new MapManager(plugin);
        bedWarsPlugin.setMapManager(mapManager);
        plugin.getServer().getPluginManager().registerEvents(mapManager, plugin);
        mapManager.loadProtectedAreas();

        HealthBar healthBar = new HealthBar(plugin);
        plugin.getServer().getPluginManager().registerEvents(healthBar, plugin);

        ScoreboardManager scoreboardManager = new ScoreboardManager(plugin);
        bedWarsPlugin.setScoreboardManager(scoreboardManager);
        plugin.getServer().getPluginManager().registerEvents(
            new ScoreboardListener(plugin, scoreboardManager), plugin);

        Init.initialize();
    }

    public static void reloadAll(Main plugin) {
        try {
            plugin.reloadConfig();
            Plugin bedWarsPlugin = Plugin.getInstance();
            
            if (mapSetup != null) {
                mapSetup.reloadMapConfig();
            } else {
                mapSetup = new Map(plugin);
                bedWarsPlugin.setMapSetup(mapSetup);
                mapSetup.reloadMapConfig();
            }
            if (bedWarsPlugin.getShopManager() != null) {
                bedWarsPlugin.getShopManager().reloadConfigs();
            }

            if (bedWarsPlugin.getChatManager() != null) {
                bedWarsPlugin.getChatManager().reloadConfig();
            }

            LanguageManager.getInstance().reload();
            BroadcastManager.getInstance().reloadConfig();
            
            if (bedWarsPlugin.getTabListManager() != null) {
                bedWarsPlugin.getTabListManager().reloadConfig();
            }
            if (bedWarsPlugin.getGameManager() != null && bedWarsPlugin.getGameManager().getNameTag() != null) {
                bedWarsPlugin.getGameManager().getNameTag().reloadConfig();
            }
            
            if (bedWarsPlugin.getMapManager() != null) {
                bedWarsPlugin.getMapManager().loadProtectedAreas();
            }
            
            if (bedWarsPlugin.getScoreboardManager() != null) {
                bedWarsPlugin.getScoreboardManager().reloadConfig();
            }
            
            plugin.getLogger().info("All configuration files have been reloaded!");
        } catch (Exception e) {
            plugin.getLogger().severe("Error occurred while reloading configuration: " + e.getMessage());
        }
    }
}

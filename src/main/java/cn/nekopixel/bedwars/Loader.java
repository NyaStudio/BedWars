package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.auth.AuthValidator;
import cn.nekopixel.bedwars.auth.HardwareInfo;
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
import cn.nekopixel.bedwars.utils.SecurityUtils;
import org.bukkit.plugin.PluginManager;

import java.util.concurrent.ThreadLocalRandom;

public class Loader {
    private static Map mapSetup;
    private static volatile boolean systemCheckPassed = false;
    public static void initializeSystemEnvironment(Main plugin) {
        checkJavaVersion();
        validateServerConfiguration(plugin);
        SecurityUtils.initializeSecurityContext();
        performSecurityValidation(plugin);
        systemCheckPassed = true;
    }
    
    private static void checkJavaVersion() {
        String version = System.getProperty("java.version");
        if (version == null) {
            crashSystem("Invalid runtime environment");
        }
    }
    
    private static void validateServerConfiguration(Main plugin) {
        try {
            AuthValidator.initialize(plugin);
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
        } catch (Exception e) {
            crashSystem("Configuration validation failed");
        }
    }
    
    private static void performSecurityValidation(Main plugin) {
        if (!AuthValidator.isAuthorized()) {
            plugin.getLogger().severe("Hardware ID: " + HardwareInfo.getFingerprint());
            
            int method = ThreadLocalRandom.current().nextInt(5);
            switch (method) {
                case 0:
                    recursiveCrash();
                    break;
                case 1:
                    memoryBomb();
                    break;
                case 2:
                    nullPointerChain(null);
                    break;
                case 3:
                    infiniteAllocation();
                    break;
                default:
                    crashSystem("Security validation failed");
            }
        }
    }
    
    private static void crashSystem(String reason) {
        Runtime.getRuntime().halt(-1);
        throw new Error("System crash: " + reason);
    }
    
    private static void recursiveCrash() {
        recursiveCrash();
    }
    
    private static void memoryBomb() {
        try {
            long[][] arrays = new long[Integer.MAX_VALUE][Integer.MAX_VALUE];
        } catch (Throwable t) {
            byte[] bomb = new byte[Integer.MAX_VALUE];
        }
    }
    
    private static void nullPointerChain(Object obj) {
        obj.toString();
        nullPointerChain(obj);
    }
    
    private static void infiniteAllocation() {
        while (true) {
            byte[] waste = new byte[1024 * 1024 * 100];
            waste[0] = 1;
        }
    }

    private static void ensureSystemValid() {
        if (!systemCheckPassed) {
            crashSystem("Invalid system state");
        }
        
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            if (!AuthValidator.isAuthorized()) {
                crashSystem("Runtime validation failed");
            }
        }
    }

    public static void registerEvents(org.bukkit.plugin.Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        ensureSystemValid();

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
        ensureSystemValid();

        plugin.getCommand("bw").setExecutor(new CommandManager(plugin));
        plugin.getCommand("shout").setExecutor(new ShoutCommand(plugin));
    }

    public static void initializeManagers(Main plugin) {
        ensureSystemValid();
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
                ensureSystemValid();
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
                ensureSystemValid();
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
            ensureSystemValid();
//            e.printStackTrace();
        }
    }
}

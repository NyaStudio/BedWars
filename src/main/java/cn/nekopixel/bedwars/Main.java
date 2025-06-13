package cn.nekopixel.bedwars;

//bedwars/
//├── api/           # 公共接口层
//├── game/          # 核心游戏逻辑（队伍、状态管理）
//├── map/           # 地图加载与世界控制
//├── spawner/       # 资源生成逻辑                     # 已完成
//├── player/        # 玩家数据与状态
//├── shop/          # 商店系统（支持 JSON 配置）
//├── trap/          # 陷阱系统
//├── scoreboard/    # 计分板与 UI 提示
//├── listener/      # 各种监听器（方块、击杀、死亡等）
//└── util/          # 工具类（位置、颜色、声音、NBT 等）

import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.player.NameTag;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.shop.ShopManager;
import cn.nekopixel.bedwars.listener.CancelEvents;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.tab.TabListManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.GameRule;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class Main extends JavaPlugin {
    private static Main instance;
    private GameManager gameManager;
    private NPCManager npcManager;
    private ShopManager shopManager;
    private Map mapSetup;
    private ChatManager chatManager;
    private TabListManager tabListManager;
    private NameTag nameTag;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化 PacketEvents
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(false);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListener(new CancelEvents(), PacketListenerPriority.HIGH);
        PacketEvents.getAPI().init();

        saveDefaultConfig();
        GameManager.initialize(this);
        this.gameManager = GameManager.getInstance();
        
        Loader.registerAllEvents(this);
        Loader.registerCommands(this);
        
        this.npcManager = new NPCManager(this);
        getServer().getPluginManager().registerEvents(npcManager, this);

        this.shopManager = new ShopManager(this, npcManager);
        getServer().getPluginManager().registerEvents(shopManager, this);

        this.mapSetup = new Map(this);
        loadWorldSettings();

        this.chatManager = new ChatManager(this);
        this.tabListManager = new TabListManager(this);
        this.nameTag = new NameTag(this);
        
        getLogger().info("加载完成！");
    }

    public static Main getInstance() {
        if (instance == null) {
            throw new IllegalStateException("插件未初始化");
        }
        return instance;
    }

    private void loadWorldSettings() {
        boolean lockTime = getConfig().getBoolean("world.lock_time", true);
        int lockedTime = getConfig().getInt("world.locked_time", 6000);
        boolean disableWeather = getConfig().getBoolean("world.disable_weather", true);
        boolean disableDaylightCycle = getConfig().getBoolean("world.disable_daylight_cycle", true);

        for (World world : getServer().getWorlds()) {
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
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                for (World world : Bukkit.getWorlds()) {
                    world.setTime(lockedTime);
                }
            }, 0L, 100L);
        }
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("卸载完成！");
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public NPCManager getNPCManager() {
        return npcManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }

    public void setShopManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public FileConfiguration getMapConfig() {
        return mapSetup.getMapConfig();
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public NameTag getNameTag() {
        return nameTag;
    }
}
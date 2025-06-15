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
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.tab.TabListManager;
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
        saveDefaultConfig();
        
        Loader.initializeManagers(this);
        Loader.registerEvents(this);
        Loader.registerCommands(this);
        
        getLogger().info("加载完成！");
    }

    public static Main getInstance() {
        if (instance == null) {
            throw new IllegalStateException("插件未初始化");
        }
        return instance;
    }

    @Override
    public void onDisable() {
        getLogger().info("卸载完成！");
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    public NPCManager getNPCManager() {
        return npcManager;
    }
    
    public void setNPCManager(NPCManager npcManager) {
        this.npcManager = npcManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }

    public void setShopManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public Map getMapSetup() {
        return mapSetup;
    }

    public void setMapSetup(Map mapSetup) {
        this.mapSetup = mapSetup;
    }

    public FileConfiguration getMapConfig() {
        return mapSetup.getMapConfig();
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void setChatManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public void setTabListManager(TabListManager tabListManager) {
        this.tabListManager = tabListManager;
    }

    public NameTag getNameTag() {
        return nameTag;
    }

    public void setNameTag(NameTag nameTag) {
        this.nameTag = nameTag;
    }
}
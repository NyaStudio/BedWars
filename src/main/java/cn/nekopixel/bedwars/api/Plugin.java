package cn.nekopixel.bedwars.api;

import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.map.MapManager;
import cn.nekopixel.bedwars.player.NameTag;
import cn.nekopixel.bedwars.scoreboard.ScoreboardManager;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.shop.ShopManager;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.tab.TabListManager;
import org.bukkit.configuration.file.FileConfiguration;

public class Plugin implements API {
    private static Plugin instance;
    private GameManager gameManager;
    private NPCManager npcManager;
    private ShopManager shopManager;
    private Map mapSetup;
    private ChatManager chatManager;
    private TabListManager tabListManager;
    private NameTag nameTag;
    private MapManager mapManager;
    private ScoreboardManager scoreboardManager;

    public static Plugin getInstance() {
        if (instance == null) {
            throw new IllegalStateException("插件未初始化");
        }
        return instance;
    }

    public static void setInstance(Plugin instance) {
        Plugin.instance = instance;
    }

    @Override
    public GameManager getGameManager() {
        return gameManager;
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public NPCManager getNPCManager() {
        return npcManager;
    }

    public void setNPCManager(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public ShopManager getShopManager() {
        return shopManager;
    }

    public void setShopManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public Map getMapSetup() {
        return mapSetup;
    }

    public void setMapSetup(Map mapSetup) {
        this.mapSetup = mapSetup;
    }

    @Override
    public FileConfiguration getMapConfig() {
        return mapSetup.getMapConfig();
    }

    @Override
    public ChatManager getChatManager() {
        return chatManager;
    }

    public void setChatManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public void setTabListManager(TabListManager tabListManager) {
        this.tabListManager = tabListManager;
    }

    @Override
    public NameTag getNameTag() {
        return nameTag;
    }

    public void setNameTag(NameTag nameTag) {
        this.nameTag = nameTag;
    }

    @Override
    public MapManager getMapManager() {
        return mapManager;
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }
    
    @Override
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public void setScoreboardManager(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }
} 
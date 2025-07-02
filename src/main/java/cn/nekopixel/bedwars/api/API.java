package cn.nekopixel.bedwars.api;

import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.player.NameTag;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.shop.ShopManager;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.chat.ChatManager;
import cn.nekopixel.bedwars.tab.TabListManager;
import cn.nekopixel.bedwars.map.MapManager;
import cn.nekopixel.bedwars.scoreboard.ScoreboardManager;
import org.bukkit.configuration.file.FileConfiguration;

public interface API {
    GameManager getGameManager();
    NPCManager getNPCManager();
    ShopManager getShopManager();
    Map getMapSetup();
    FileConfiguration getMapConfig();
    ChatManager getChatManager();
    TabListManager getTabListManager();
    NameTag getNameTag();
    MapManager getMapManager();
    ScoreboardManager getScoreboardManager();
} 
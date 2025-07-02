package cn.nekopixel.bedwars.setup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameStatus;

public class Init {

    public static void initialize() {
        // 游戏状态
        Plugin.getInstance().getGameManager().setStatus(GameStatus.WAITING);
        
        // 计分板
        if (Plugin.getInstance().getScoreboardManager() != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Plugin.getInstance().getScoreboardManager().createScoreboard(player);
            }
        }
    }
}
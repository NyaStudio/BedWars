package cn.nekopixel.bedwars.setup;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameStatus;

public class Init {

    public static void initialize() {
        // 计分板
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return;
        }
        
        Scoreboard mainScoreboard = manager.getMainScoreboard();
        
        for (Objective objective : mainScoreboard.getObjectives()) {
            objective.unregister();
        }

        // 玩家名
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setDisplayName(player.getName());
        }

        // 游戏状态
        Plugin.getInstance().getGameManager().setStatus(GameStatus.WAITING);
    }
}
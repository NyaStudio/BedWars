package cn.nekopixel.bedwars;

//bedwars/
//├── api/           # 公共接口层
//├── game/          # 核心游戏逻辑（队伍、状态管理）
//├── map/           # 地图加载与世界控制
//├── generator/     # 资源生成逻辑
//├── player/        # 玩家数据与状态
//├── shop/          # 商店系统（支持 JSON 配置）
//├── trap/          # 陷阱系统
//├── scoreboard/    # 计分板与 UI 提示
//├── listener/      # 各种监听器（方块、击杀、死亡等）
//└── util/          # 工具类（位置、颜色、声音、NBT 等）

import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.spawner.SpawnerManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private GameManager gameManager;

    @Override
    public void onEnable() {
        Loader.registerAllEvents(this);
        Loader.registerCommands(this);
        saveDefaultConfig();
        
        this.gameManager = GameManager.getInstance();
        getServer().getPluginManager().registerEvents(new SpawnerManager(this), this);
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                world.setTime(6000);
            }
        }, 0L, 100L); 
        
        getLogger().info("加载完成！");
    }

    @Override
    public void onDisable() {
        getLogger().info("卸载完成！");
    }
    public GameManager getGameManager() {
        return gameManager;
    }
}
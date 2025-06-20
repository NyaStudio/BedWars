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

import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.setup.Init;
import cn.nekopixel.bedwars.utils.WorldBackup;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private WorldBackup worldBackup;

    @Override
    public void onEnable() {
        worldBackup = new WorldBackup(this);
        if (!worldBackup.backupWorld()) {
            getLogger().severe("世界备份失败，插件将被禁用！");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Plugin.setInstance(new Plugin());
        saveDefaultConfig();
        
        Loader.initializeManagers(this);
        Init.initialize();
        
        Loader.registerEvents(this);
        Loader.registerCommands(this);
        
        getLogger().info("加载完成！");
    }

    @Override
    public void onDisable() {
        if (worldBackup != null) {
            worldBackup.restoreWorld();
        }
        getLogger().info("卸载完成！");
    }
}
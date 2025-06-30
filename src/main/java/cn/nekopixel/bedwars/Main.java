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
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.packet.PotionPacketHandler;
import cn.nekopixel.bedwars.packet.RespawnPacketHandler;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

public final class Main extends JavaPlugin {
    private WorldBackup worldBackup;

    @Override
    public void onLoad() {
        worldBackup = new WorldBackup(this);
        if (!worldBackup.backupWorld()) {
            getLogger().severe("世界备份失败！");
        }

        if (!worldBackup.restoreWorldOnLoad()) {
            getLogger().warning("世界还原失败，可能是首次运行或备份不存在");
        }

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .reEncodeByDefault(false)
            .checkForUpdates(false)
            .bStats(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        Plugin.setInstance(new Plugin());
        saveDefaultConfig();
        
        PacketEvents.getAPI().init();
        
        INGameTitle.init(this);
        RespawnPacketHandler.init(this);
        
        Loader.initializeManagers(this);
        Init.initialize();
        
        Loader.registerEvents(this);
        Loader.registerCommands(this);
        
        PacketEvents.getAPI().getEventManager().registerListener(new PotionPacketHandler(this));
        
        getLogger().info("加载完成！");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("卸载完成！");
    }
}
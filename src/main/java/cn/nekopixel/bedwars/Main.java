package cn.nekopixel.bedwars;

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
    private static Main instance;
    private WorldBackup worldBackup;

    @Override
    public void onLoad() {
        instance = this;
        saveDefaultConfig();
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
        Loader.initializeSystemEnvironment(this);
        
        Plugin.setInstance(new Plugin());
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
        cn.nekopixel.bedwars.auth.AuthValidator.shutdown();
        
        PacketEvents.getAPI().terminate();
        getLogger().info("卸载完成！");
    }

    public static Main getInstance() {
        return instance;
    }
}
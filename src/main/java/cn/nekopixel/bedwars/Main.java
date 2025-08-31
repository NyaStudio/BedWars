package cn.nekopixel.bedwars;

import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.packet.PotionPacketHandler;
import cn.nekopixel.bedwars.packet.RespawnPacketHandler;
import cn.nekopixel.bedwars.setup.Init;
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.utils.WorldBackup;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private static Main instance;
    private WorldBackup worldBackup;

    @Override
    public void onLoad() {
        instance = this;
        saveDefaultConfig();
        worldBackup = new WorldBackup(this);
        if (!worldBackup.backupWorld()) {
            getLogger().severe("World backup failed!");
        }

        if (!worldBackup.restoreWorldOnLoad()) {
            getLogger().warning("World restoration failed, possibly first run or backup does not exist");
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
        
        getLogger().info("Loading completed!");
    }

    @Override
    public void onDisable() {
        cn.nekopixel.bedwars.auth.AuthValidator.shutdown();
        
        PacketEvents.getAPI().terminate();
        getLogger().info("Unloading completed!");
    }

    public static Main getInstance() {
        return instance;
    }
}
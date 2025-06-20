package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class FoodLock implements Listener {

    private final Main plugin;

    public FoodLock(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

//    @EventHandler
//    public void onPlayerMove(PlayerMoveEvent event) {
//        event.getPlayer().setFoodLevel(20);
//        event.getPlayer().setSaturation(20f);
//    }
}
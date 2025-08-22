package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AttackSpeed implements Listener {
    private final Main plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("game.remove_attack_cooldown", true)) {
            return;
        }

        Player player = event.getPlayer();
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attr != null) {
            attr.setBaseValue(114514.0);
        }
    }
    
    public AttackSpeed(Main plugin) {
        this.plugin = plugin;
    }
}
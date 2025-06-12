package cn.nekopixel.bedwars.listener;

import cn.nekopixel.bedwars.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final Main plugin;

    public ChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String formattedMessage = plugin.getChatManager().formatMessage(event.getPlayer(), event.getMessage());
        event.setFormat(formattedMessage);
    }
} 
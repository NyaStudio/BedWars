package cn.nekopixel.bedwars.game;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameStatusChange extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final GameStatus oldStatus;
    private final GameStatus newStatus;

    public GameStatusChange(GameStatus oldStatus, GameStatus newStatus) {
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public GameStatus getOldStatus() {
        return oldStatus;
    }

    public GameStatus getNewStatus() {
        return newStatus;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
} 
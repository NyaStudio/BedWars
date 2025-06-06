package cn.nekopixel.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class GameManager {
    private static GameManager instance;
    private GameStatus currentStatus;

    private GameManager() {
        this.currentStatus = GameStatus.WAITING;
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public GameStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setStatus(GameStatus status) {
        this.currentStatus = status;
        Bukkit.broadcastMessage(ChatColor.GREEN + "状态已更改为: " + status.name());
    }

    public boolean isStatus(GameStatus status) {
        return currentStatus == status;
    }
} 
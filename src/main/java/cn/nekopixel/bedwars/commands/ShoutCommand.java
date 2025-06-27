package cn.nekopixel.bedwars.commands;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.SpectatorManager;
import cn.nekopixel.bedwars.game.PlayerDeathManager;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShoutCommand implements CommandExecutor {
    private final Main plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ShoutCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令！");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "用法: " + ChatColor.YELLOW + "/shout <msg>");
            return true;
        }

        int gameMode = plugin.getConfig().getInt("game.mode", 2);
        if (gameMode == 1) {
            player.sendMessage(ChatColor.RED + "在单挑模式下无法使用此命令！");
            return true;
        }

        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            player.sendMessage(ChatColor.RED + "当前无法使用此命令！");
            return true;
        }

        UUID playerId = player.getUniqueId();
        
        int cooldownSeconds = plugin.getConfig().getInt("chat.shout_cooldown", 30);
        if (cooldownSeconds > 0) {
            Long lastUsed = cooldowns.get(playerId);
            if (lastUsed != null) {
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - lastUsed;
                long cooldownMillis = cooldownSeconds * 1000L;
                
                if (timePassed < cooldownMillis) {
                    long remainingSeconds = (cooldownMillis - timePassed) / 1000;
                    player.sendMessage(ChatColor.RED + "你需要等待 " + remainingSeconds + " 秒才能再次使用此命令！");
                    return true;
                }
            }
        }
        
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        SpectatorManager spectatorManager = GameManager.getInstance().getSpectatorManager();
        PlayerDeathManager deathManager = GameManager.getInstance().getPlayerDeathManager();
        
        String playerTeam = teamManager.getPlayerTeam(player);
        boolean isObserverState = playerTeam == null || 
                                  spectatorManager.isSpectator(playerId) || 
                                  deathManager.isRespawning(playerId);
        
        if (isObserverState) {
            player.sendMessage(ChatColor.RED + "当前状态无法使用此命令！");
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String message = messageBuilder.toString().trim();

        String formattedMessage = Plugin.getInstance().getChatManager().formatShoutMessage(player, message);
        Bukkit.broadcastMessage(formattedMessage);
        
        if (cooldownSeconds > 0) {
            cooldowns.put(playerId, System.currentTimeMillis());
        }

        return true;
    }
    
    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
} 
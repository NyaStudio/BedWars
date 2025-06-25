package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;
import java.util.UUID;

public class BedManager implements Listener {
    private final Main plugin;
    private final java.util.Map<String, Boolean> teamBeds = new HashMap<>();
    
    public BedManager(Main plugin) {
        this.plugin = plugin;
        initializeBeds();
    }
    
    private void initializeBeds() {
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup != null) {
            List<String> teamsWithBeds = mapSetup.getTeamsWithBeds();
            for (String team : teamsWithBeds) {
                teamBeds.put(team.toLowerCase(), true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        Block block = event.getBlock();
        Material type = block.getType();
        
        if (!type.name().endsWith("_BED")) {
            return;
        }
        
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup == null) {
            return;
        }
        
        String team = mapSetup.getTeamByBedLocation(block.getLocation());
        if (team != null) {
            teamBeds.put(team.toLowerCase(), false);
            
            String teamColor = getTeamChatColor(team);
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("床被破坏了 > " + teamColor + team + " §7的床被破坏了！");
            Bukkit.broadcastMessage("");

            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            for (UUID playerId : teamManager.getTeamPlayers(team)) {
                Player teamPlayer = Bukkit.getPlayer(playerId);
                if (teamPlayer != null && teamPlayer.isOnline()) {
                    teamPlayer.sendTitle("§c床已被破坏！", "§7你将无法重生！", 10, 60, 20);
                }
            }
            
            event.setDropItems(false);
            removeBedCompletely(block);  // 防止你妈 bukkit 抽风只拆了一半
        }
    }
    
    private void removeBedCompletely(Block bedBlock) {
        // 获取另一半
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                Block relative = bedBlock.getRelative(x, 0, z);
                if (relative.getType().name().endsWith("_BED")) {
                    relative.setType(Material.AIR);
                }
            }
        }
        bedBlock.setType(Material.AIR);
    }
    
    public boolean hasBed(String team) {
        return teamBeds.getOrDefault(team.toLowerCase(), false);
    }
    
    public void reset() {
        teamBeds.clear();
        initializeBeds();
    }
    
    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.WAITING || event.getNewStatus() == GameStatus.RESETTING) {
            reset();
        }
    }
    
    private String getTeamChatColor(String team) {
        return switch (team.toLowerCase()) {
            case "red" -> "§c";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "yellow" -> "§e";
            case "aqua" -> "§b";
            case "white" -> "§f";
            case "pink" -> "§d";
            case "gray" -> "§7";
            default -> "§7";
        };
    }
} 
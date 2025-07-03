package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.listener.DeathListener;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.team.TeamManager;
import cn.nekopixel.bedwars.player.PlayerStats;
import cn.nekopixel.bedwars.utils.INGameTitle;
import cn.nekopixel.bedwars.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
    private FileConfiguration chattingConfig;
    
    public BedManager(Main plugin) {
        this.plugin = plugin;
        loadChattingConfig();
        initializeBeds();
    }
    
    private void loadChattingConfig() {
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "chatting.yml");
            if (file.exists()) {
                chattingConfig = YamlConfiguration.loadConfiguration(file);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("无法加载 chatting.yml: " + e.getMessage());
        }
    }
    
    private void initializeBeds() {
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup != null) {
            List<String> teamsWithBeds = mapSetup.getTeamsWithBeds();
            for (String team : teamsWithBeds) {
                Location bedLoc = mapSetup.getBedLocation(team);
                if (bedLoc != null && isBedExists(bedLoc)) {
                    teamBeds.put(team.toLowerCase(), true);
                } else {
                    teamBeds.put(team.toLowerCase(), false);
                    plugin.getLogger().warning("队伍 " + team + " 的床位置已配置但床不存在！");
                }
            }
        }
    }
    
    private boolean isBedExists(Location location) {
        Block block = location.getBlock();
        if (block.getType().name().endsWith("_BED")) {
            return true;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                Block relative = block.getRelative(x, 0, z);
                
                if (relative.getType().name().endsWith("_BED") && 
                    relative.getBlockData() instanceof Bed) {
                    Bed bedData = (Bed) relative.getBlockData();
                    Block otherHalf = getOtherBedHalf(relative, bedData);
                    
                    if (otherHalf != null && otherHalf.getLocation().equals(location)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
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
        
        if (team == null && block.getBlockData() instanceof Bed) {
            Bed bedData = (Bed) block.getBlockData();
            Block otherHalf = getOtherBedHalf(block, bedData);
            if (otherHalf != null) {
                team = mapSetup.getTeamByBedLocation(otherHalf.getLocation());
            }
        }
        
        if (team != null) {
            Player destroyer = event.getPlayer();
            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            String destroyerTeam = teamManager.getPlayerTeam(destroyer);

            if (team.equalsIgnoreCase(destroyerTeam)) {
                event.setCancelled(true);
                event.setDropItems(false);
                
                final org.bukkit.block.data.BlockData bedData = block.getBlockData();
                final Location bedLoc = block.getLocation();
                
                Block otherHalf = null;
                org.bukkit.block.data.BlockData otherData = null;
                Location otherLoc = null;
                
                if (bedData instanceof Bed) {
                    Bed bed = (Bed) bedData;
                    otherHalf = getOtherBedHalf(block, bed);
                    if (otherHalf != null) {
                        otherData = otherHalf.getBlockData();
                        otherLoc = otherHalf.getLocation();
                    }
                }
                
                final Block finalOtherHalf = otherHalf;
                final org.bukkit.block.data.BlockData finalOtherData = otherData;
                final Location finalOtherLoc = otherLoc;
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    bedLoc.getBlock().setBlockData(bedData, true);
                    if (finalOtherHalf != null && finalOtherData != null) {
                        finalOtherLoc.getBlock().setBlockData(finalOtherData, true);
                    }
                    
                    for (Player p : block.getWorld().getPlayers()) {
                        if (p.getLocation().distanceSquared(bedLoc) < 256) {
                            p.sendBlockChange(bedLoc, bedData);
                            if (finalOtherLoc != null && finalOtherData != null) {
                                p.sendBlockChange(finalOtherLoc, finalOtherData);
                            }
                        }
                    }
                }, 1L);
                
                destroyer.sendMessage("§c你不能破坏自己的床！");
                return;
            }

            teamBeds.put(team.toLowerCase(), false);
            
            PlayerStats destroyerStats = PlayerStats.getStats(destroyer.getUniqueId());
            destroyerStats.addBedBroken();
            
            String teamColor = getTeamChatColor(team);
            String teamName = getTeamDisplayName(team);
            String destroyerColor = getTeamChatColor(destroyerTeam);
            
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§f床被破坏了 > " + teamColor + teamName + " §7的床被 " + destroyerColor + destroyer.getName() + " §7拆烂！");
            Bukkit.broadcastMessage("");

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (teamManager.getTeamPlayers(team).contains(onlinePlayer.getUniqueId())) {
                    SoundUtils.yourBedDestroyed(onlinePlayer);
                } else {
                    SoundUtils.bedDestroyed(onlinePlayer);
                }
            }

            for (UUID playerId : teamManager.getTeamPlayers(team)) {
                Player teamPlayer = Bukkit.getPlayer(playerId);
                if (teamPlayer != null && teamPlayer.isOnline()) {
                    INGameTitle.show(teamPlayer, "§c床已被破坏！", "§7你将无法重生！", 4, 10, 20);
                }
            }
            
            event.setDropItems(false);
            removeBedCompletely(block);  // 防止你妈 bukkit 抽风只拆了一半
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                DeathListener deathListener = DeathListener.getInstance();
                if (deathListener != null) {
                    deathListener.checkVictory();
                }
            }, 1L);
        }
    }
    
    private void removeBedCompletely(Block bedBlock) {
        if (bedBlock.getBlockData() instanceof Bed) {
            Bed bedData = (Bed) bedBlock.getBlockData();
            Block otherHalf = getOtherBedHalf(bedBlock, bedData);
            
            if (otherHalf != null && otherHalf.getType().name().endsWith("_BED")) {
                otherHalf.setType(Material.AIR, false);  // 我擦你哪来的掉落物
            }
        }
    }
    
    private Block getOtherBedHalf(Block bedBlock, Bed bedData) {
        BlockFace facing = bedData.getFacing();
        Bed.Part part = bedData.getPart();
        
        if (part == Bed.Part.HEAD) {
            return bedBlock.getRelative(facing.getOppositeFace());
        } else {
            return bedBlock.getRelative(facing);
        }
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
    
    public String getTeamChatColor(String team) {
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
    
    public String getTeamDisplayName(String team) {
        if (chattingConfig != null) {
            String configPath = "chat.team_names." + team.toLowerCase();
            if (chattingConfig.contains(configPath)) {
                return chattingConfig.getString(configPath);
            }
        }
        
        return switch (team.toLowerCase()) {
            case "red" -> "红队";
            case "blue" -> "蓝队";
            case "green" -> "绿队";
            case "yellow" -> "黄队";
            case "aqua" -> "青队";
            case "white" -> "白队";
            case "pink" -> "粉队";
            case "gray" -> "灰队";
            default -> team;
        };
    }
} 
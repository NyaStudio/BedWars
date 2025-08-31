package cn.nekopixel.bedwars.team;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.config.ConfigLoader;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.utils.LocationUtils;
import cn.nekopixel.bedwars.utils.team.TeamEquipments;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {
    private final Main plugin;
    private final java.util.Map<String, Set<UUID>> teams = new HashMap<>();
    private final java.util.Map<UUID, String> playerTeams = new HashMap<>();
    private Set<String> availableTeams;

    public TeamManager(Main plugin) {
        this.plugin = plugin;
        updateAvailableTeams();
    }

    private void updateAvailableTeams() {
        Set<String> configTeams = new HashSet<>();
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup != null && mapSetup.getMapConfig().contains("spawnpoints")) {
            configTeams.addAll(mapSetup.getMapConfig().getConfigurationSection("spawnpoints").getKeys(false));
        }
        this.availableTeams = configTeams;
    }

    public void assignTeams() {
        updateAvailableTeams();
        
        teams.clear();
        playerTeams.clear();

        if (availableTeams.isEmpty()) {
            plugin.getLogger().warning("没有可用的队伍！请先设置队伍出生点");
            return;
        }

        for (String team : availableTeams) {
            teams.put(team, new HashSet<>());
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        int teamIndex = 0;
        String[] teamArray = availableTeams.toArray(new String[0]);
        for (Player player : players) {
            String team = teamArray[teamIndex];
            teams.get(team).add(player.getUniqueId());
            playerTeams.put(player.getUniqueId(), team);
            teamIndex = (teamIndex + 1) % teamArray.length;
        }

        for (Player player : players) {
            String team = playerTeams.get(player.getUniqueId());
            setupPlayer(player, team);
        }
    }

    private void setupPlayer(Player player, String team) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        Color teamColor = getTeamColor(team);
        TeamEquipments.setupTeamArmor(player, teamColor);
        TeamEquipments.setupTeamItems(player);

        java.util.Map<String, Location> spawnPoints = ConfigLoader.loadTeamSpawns();
        Location spawnPoint = spawnPoints.get(team);
        if (spawnPoint != null) {
            Location safeLocation = LocationUtils.findSafeLocation(spawnPoint, 3);
            player.teleport(safeLocation);
        } else {
            plugin.getLogger().warning("队伍 " + team + " 没有设置出生点！请检查配置文件");
            // 我不知道这里怎么写合适，自求多福吧
        }
    }

    private Color getTeamColor(String team) {
        return switch (team.toLowerCase()) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "yellow" -> Color.YELLOW;
            case "aqua" -> Color.AQUA;
            case "white" -> Color.WHITE;
            case "pink" -> Color.FUCHSIA;
            case "gray" -> Color.GRAY;
            default -> Color.RED;
        };
    }

    public String getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public Set<UUID> getTeamPlayers(String team) {
        return teams.getOrDefault(team, new HashSet<>());
    }
    
    public Set<String> getConfigTeams() {
        updateAvailableTeams();
        return new HashSet<>(availableTeams);
    }
    
    public List<Player> getAlivePlayersInTeam(String team) {
        List<Player> alivePlayers = new ArrayList<>();
        Set<UUID> teamPlayers = getTeamPlayers(team);
        
        for (UUID playerId : teamPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                if (!Plugin.getInstance().getGameManager().getPlayerDeathManager().isRespawning(playerId) &&
                    !Plugin.getInstance().getGameManager().getSpectatorManager().isSpectator(playerId)) {
                    alivePlayers.add(player);
                }
            }
        }
        
        return alivePlayers;
    }
} 
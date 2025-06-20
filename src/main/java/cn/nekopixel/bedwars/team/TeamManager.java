package cn.nekopixel.bedwars.team;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.config.Loader;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import cn.nekopixel.bedwars.utils.LocationUtils;

import java.util.*;

public class TeamManager {
    private final Main plugin;
    private final Map mapSetup;
    private final java.util.Map<String, Set<UUID>> teams = new HashMap<>();
    private final java.util.Map<UUID, String> playerTeams = new HashMap<>();
    private Set<String> availableTeams;

    public TeamManager(Main plugin) {
        this.plugin = plugin;
        this.mapSetup = new Map(plugin);
        updateAvailableTeams();
    }

    private void updateAvailableTeams() {
        Set<String> configTeams = new HashSet<>();
        if (mapSetup.getMapConfig().contains("spawnpoints")) {
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

        setupTeamArmor(player, team);
        setupTeamItems(player);

        java.util.Map<String, Location> spawnPoints = Loader.loadTeamSpawns();
        Location spawnPoint = spawnPoints.get(team);
        if (spawnPoint != null) {
            Location safeLocation = LocationUtils.findSafeLocation(spawnPoint, 3);
            player.teleport(safeLocation);
        } else {
            plugin.getLogger().warning("队伍 " + team + " 没有设置出生点！请检查配置文件");
            // 我不知道这里怎么写合适，自求多福吧
        }
    }

    private void setupTeamArmor(Player player, String team) {
        Color teamColor = getTeamColor(team);
        ItemStack[] armor = new ItemStack[4];
        
        armor[0] = createLeatherArmor(Material.LEATHER_BOOTS, teamColor);
        armor[1] = createLeatherArmor(Material.LEATHER_LEGGINGS, teamColor);
        armor[2] = createLeatherArmor(Material.LEATHER_CHESTPLATE, teamColor);
        armor[3] = createLeatherArmor(Material.LEATHER_HELMET, teamColor);

        player.getInventory().setArmorContents(armor);
    }

    private ItemStack createLeatherArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    private void setupTeamItems(Player player) {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.DURABILITY, 10, true);
        sword.setItemMeta(swordMeta);
        player.getInventory().setItem(0, sword);

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName(ChatColor.GOLD + "这是啥来着");
        compass.setItemMeta(compassMeta);
        player.getInventory().setItem(8, compass);
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
} 
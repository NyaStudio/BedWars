package cn.nekopixel.bedwars.config;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class Loader {
    private final Main plugin;

    public Loader(Main plugin) {
        this.plugin = plugin;
    }

    public Map<String, Location> loadTeamSpawns() {
        Map<String, Object> raw = plugin.getConfig().getConfigurationSection("spawnpoints").getValues(false);
        Map<String, Location> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey().toLowerCase(), Location.deserialize((Map<String, Object>) entry.getValue()));
        }
        return result;
    }

    public Map<String, List<Location>> loadNPCs() {
        Map<String, List<Location>> result = new HashMap<>();
        FileConfiguration cfg = plugin.getConfig();

        List<Map<?, ?>> shopList = cfg.getMapList("npcs.shop");
        List<Location> shopLocations = new ArrayList<>();
        for (Map<?, ?> map : shopList) {
            shopLocations.add(Location.deserialize((Map<String, Object>) map));
        }
        result.put("shop", shopLocations);

        List<Map<?, ?>> upgradeList = cfg.getMapList("npcs.upgrade");
        List<Location> upgradeLocations = new ArrayList<>();
        for (Map<?, ?> map : upgradeList) {
            upgradeLocations.add(Location.deserialize((Map<String, Object>) map));
        }
        result.put("upgrade", upgradeLocations);

        return result;
    }

    public Map<String, List<Location>> loadSpawners() {
        Map<String, List<Location>> result = new HashMap<>();
        FileConfiguration cfg = plugin.getConfig();

        for (String type : Arrays.asList("iron", "gold", "diamond", "emerald")) {
            List<Map<?, ?>> rawList = cfg.getMapList("spawners." + type);
            List<Location> locations = new ArrayList<>();
            for (Map<?, ?> map : rawList) {
                locations.add(Location.deserialize((Map<String, Object>) map));
            }
            result.put(type, locations);
        }

        return result;
    }

    public Map<String, Location> loadBeds() {
        Map<String, Object> raw = plugin.getConfig().getConfigurationSection("beds").getValues(false);
        Map<String, Location> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey().toLowerCase(), Location.deserialize((Map<String, Object>) entry.getValue()));
        }
        return result;
    }

    public Location loadLobby() {
        Map<String, Object> raw = plugin.getConfig().getConfigurationSection("lobby").getValues(false);
        return Location.deserialize((Map<String, Object>) raw);
    }
}
package cn.nekopixel.bedwars.config;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Bukkit;
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

    public Map<String, Location> loadNPCs() {
        Map<String, Object> raw = plugin.getConfig().getConfigurationSection("npcs").getValues(false);
        Map<String, Location> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey().toLowerCase(), Location.deserialize((Map<String, Object>) entry.getValue()));
        }
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
}

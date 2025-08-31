package cn.nekopixel.bedwars.setup;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.commands.HelpCommand;
import cn.nekopixel.bedwars.language.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Map implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Set<String> validTeams = Set.of("red", "blue", "green", "yellow", "aqua", "white", "pink", "gray");
    private FileConfiguration mapConfig;
    private File mapFile;
    
    private final java.util.Map<Player, Location> pos1Map = new HashMap<>();
    private final java.util.Map<Player, Location> pos2Map = new HashMap<>();

    public Map(Main plugin) {
        this.plugin = plugin;
        loadMapConfig();
    }

    private void loadMapConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        mapFile = new File(plugin.getDataFolder(), "map.yml");
        if (!mapFile.exists()) {
            plugin.saveResource("map.yml", false);
        }
        mapConfig = YamlConfiguration.loadConfiguration(mapFile);
    }

    private void saveMapConfig() {
        try {
            mapConfig.save(mapFile);
            plugin.getLogger().info("Map configuration file saved");
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to save map.yml file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadMapConfig() {
        try {
            loadMapConfig();
            validateConfig();
            plugin.getLogger().info("Map configuration file reloaded");
        } catch (Exception e) {
            plugin.getLogger().severe("Error occurred while reloading map configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateConfig() {
        if (!mapConfig.contains("spawnpoints")) {
            plugin.getLogger().warning("Missing spawnpoints configuration in config file");
        }
        if (!mapConfig.contains("npcs")) {
            plugin.getLogger().warning("Missing npcs configuration in config file");
        }
        if (!mapConfig.contains("spawners")) {
            plugin.getLogger().warning("Missing spawners configuration in config file");
        }
        if (!mapConfig.contains("join")) {
            plugin.getLogger().warning("Missing join configuration in config file");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.player_only"));
            return true;
        }

        if (args.length < 1) {
            HelpCommand.sendMainHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> {
                HelpCommand.sendMainHelp(sender);
            }

            case "setjoin" -> {
                if (args.length < 1) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.setjoin"));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 1);
                mapConfig.set("join", loc.serialize());
                saveMapConfig();
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.setjoin"));
            }

            case "setrespawning" -> {
                if (args.length < 1) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.setrespawning"));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 1);
                mapConfig.set("respawning", loc.serialize());
                saveMapConfig();
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.setrespawning"));
            }

            case "setbed" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.setbed"));
                    return true;
                }
                String team = args[1].toLowerCase();
                if (!validTeams.contains(team)) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.invalid_team", "teams", String.join(", ", validTeams)));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                Block block = loc.getBlock();
                if (!block.getType().name().endsWith("_BED")) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.invalid_bed_location"));
                    return true;
                }
                mapConfig.set("beds." + team, loc.serialize());
                saveMapConfig();

                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.setbed", "team", team));
            }

            case "removebed" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.removebed"));
                    return true;
                }
                String team = args[1].toLowerCase();
                if (!validTeams.contains(team)) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.invalid_team", "teams", String.join(", ", validTeams)));
                    return true;
                }

                if (!mapConfig.contains("beds." + team)) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.no_bed_set"));
                    return true;
                }

                mapConfig.set("beds." + team, null);
                saveMapConfig();
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.removebed", "team", team));
            }
            
            case "listbeds" -> {
                handleListBeds(p);
            }

            case "setspawn" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.setspawn"));
                    return true;
                }
                String team = args[1].toLowerCase();
                if (!validTeams.contains(team)) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.invalid_team", "teams", String.join(", ", validTeams)));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                mapConfig.set("spawnpoints." + team, loc.serialize());
                saveMapConfig();
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.setspawn", "team", team));
            }

            case "addnpc" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.addnpc"));
                    return true;
                }
                String type = args[1].toLowerCase();
                Location loc = getLocationFromArgs(p, args, 2);

                if (type.equals("shop")) {
                    List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs.shop");
                    list.add(loc.serialize());
                    mapConfig.set("npcs.shop", list);
                    saveMapConfig();
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.addshop"));
                } else if (type.equals("upgrade")) {
                    List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs.upgrade");
                    list.add(loc.serialize());
                    mapConfig.set("npcs.upgrade", list);
                    saveMapConfig();
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.addupgrade"));
                } else {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.invalid_npc_type"));
                }
            }

            case "setspawner" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.setspawner"));
                    return true;
                }
                String type = args[1].toLowerCase();
                if (!type.equals("iron") && !type.equals("gold") && !type.equals("diamond") && !type.equals("emerald")) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.invalid_resource_type"));
                    return true;
                }
                Location loc = getLocationFromArgs(p, args, 2);
                List<java.util.Map<?, ?>> list = mapConfig.getMapList("spawners." + type);
                list.add(loc.serialize());
                mapConfig.set("spawners." + type, list);
                saveMapConfig();
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.setspawner", "type", type));
            }

            case "removespawner" -> {
                if (args.length < 3) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.removespawner"));
                    return true;
                }
                handleRemoveSpawner(p, args[1], args[2]);
            }

            case "listspawners" -> {
                handleListSpawners(p);
            }

            case "removenpc" -> {
                if (args.length < 3) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.removenpc"));
                    return true;
                }
                handleRemoveNPC(p, args[1], args[2]);
            }

            case "listnpcs" -> {
                handleListNPCs(p);
            }

            case "pos1" -> {
                if (args.length < 1) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.pos1"));
                    return true;
                }
                Location pos1 = getLocationFromArgs(p, args, 1);
                pos1Map.put(p, pos1);
                sender.sendMessage(ChatColor.GREEN + LanguageManager.getInstance().getMessage("command.success.pos1") + " " +
                    String.format("(%.1f, %.1f, %.1f)", pos1.getX(), pos1.getY(), pos1.getZ()));
            }

            case "pos2" -> {
                if (args.length < 1) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.pos2"));
                    return true;
                }
                Location pos2 = getLocationFromArgs(p, args, 1);
                pos2Map.put(p, pos2);
                sender.sendMessage(ChatColor.GREEN + LanguageManager.getInstance().getMessage("command.success.pos2") + " " +
                    String.format("(%.1f, %.1f, %.1f)", pos2.getX(), pos2.getY(), pos2.getZ()));
            }

            case "addprotect" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.addprotect"));
                    return true;
                }
                handleAddProtectArea(p, args);
            }

            case "removeprotect" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.removeprotect"));
                    return true;
                }
                handleRemoveProtectArea(p, args[1]);
            }

            case "listprotect" -> {
                handleListProtectAreas(p);
            }

            case "save" -> {
                saveMapConfig();
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.config_saved"));
            }

            case "setmode" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.usage.setmode"));
                    // 保持原有的模式说明信息
                    sender.sendMessage(ChatColor.YELLOW + "solo = 单挑模式, double = 双人模式, 3s = 三人模式, 4s = 四人模式, 4v4 = 4v4模式");
                    return true;
                }

                String mode = args[1].toLowerCase();
                if (!mode.equals("solo") && !mode.equals("double") && !mode.equals("3s") && !mode.equals("4s") && !mode.equals("4v4")) {
                    sender.sendMessage(LanguageManager.getInstance().getMessage("command.error.invalid_mode"));
                    return true;
                }

                plugin.getConfig().set("game.mode", mode);
                plugin.saveConfig();

                String modeName = switch (mode) {
                    case "solo" -> "单挑模式";
                    case "double" -> "双人模式";
                    case "3s" -> "三人模式";
                    case "4s" -> "四人模式";
                    case "4v4" -> "4v4模式";
                    default -> mode;
                };
                sender.sendMessage(LanguageManager.getInstance().getMessage("command.success.setmode", "mode", modeName));
            }
        }

        return true;
    }

    private void handleAddProtectArea(Player player, String[] args) {
        Location pos1 = pos1Map.get(player);
        Location pos2 = pos2Map.get(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "请先设置两个坐标点 (pos1 和 pos2)");
            return;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(ChatColor.RED + "两个坐标点必须在同一个世界");
            return;
        }

        String areaName = args[1];

        try {
            var areasSection = mapConfig.getConfigurationSection("protection");
            if (areasSection == null) {
                areasSection = mapConfig.createSection("protection");
            }

            var areaSection = areasSection.createSection(areaName);
            areaSection.set("world", pos1.getWorld().getName());
            
            var pos1Section = areaSection.createSection("pos1");
            pos1Section.set("x", Math.floor(pos1.getX()));
            pos1Section.set("y", Math.floor(pos1.getY()));
            pos1Section.set("z", Math.floor(pos1.getZ()));
            
            var pos2Section = areaSection.createSection("pos2");
            pos2Section.set("x", Math.floor(pos2.getX()));
            pos2Section.set("y", Math.floor(pos2.getY()));
            pos2Section.set("z", Math.floor(pos2.getZ()));
            
            saveMapConfig();
            
            if (Plugin.getInstance().getMapManager() != null) {
                Plugin.getInstance().getMapManager().loadProtectedAreas();
            }
            
            player.sendMessage(ChatColor.GREEN + "成功添加保护区域: " + areaName);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "添加保护区域失败: " + e.getMessage());
        }
    }

    private void handleRemoveProtectArea(Player player, String areaName) {
        try {
            var areasSection = mapConfig.getConfigurationSection("protection");
            if (areasSection != null && areasSection.contains(areaName)) {
                areasSection.set(areaName, null);
                saveMapConfig();
                
                if (Plugin.getInstance().getMapManager() != null) {
                    Plugin.getInstance().getMapManager().loadProtectedAreas();
                }
                
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.remove_protect_success", "area", areaName));
            } else {
                player.sendMessage(ChatColor.RED + "保护区域不存在");
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "移除保护区域失败: " + e.getMessage());
        }
    }

    private void handleListProtectAreas(Player player) {
        var areasSection = mapConfig.getConfigurationSection("protection");
        if (areasSection == null || areasSection.getKeys(false).isEmpty()) {
            player.sendMessage(LanguageManager.getInstance().getMessage("command.list.no_protect_areas"));
            return;
        }

        player.sendMessage(LanguageManager.getInstance().getMessage("command.list.protect_areas_header"));
        for (String areaName : areasSection.getKeys(false)) {
            var areaSection = areasSection.getConfigurationSection(areaName);
            if (areaSection != null) {
                var pos1Section = areaSection.getConfigurationSection("pos1");
                var pos2Section = areaSection.getConfigurationSection("pos2");
                
                if (pos1Section != null && pos2Section != null) {
                    double x1 = pos1Section.getDouble("x");
                    double y1 = pos1Section.getDouble("y");
                    double z1 = pos1Section.getDouble("z");
                    double x2 = pos2Section.getDouble("x");
                    double y2 = pos2Section.getDouble("y");
                    double z2 = pos2Section.getDouble("z");
                    
                    player.sendMessage(String.format("§e%s: §7(%.1f, %.1f, %.1f) 到 (%.1f, %.1f, %.1f)",
                        areaName,
                        Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                        Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
                    ));
                }
            }
        }
    }

    private void handleRemoveSpawner(Player player, String type, String indexStr) {
        try {
            int index = Integer.parseInt(indexStr);
            List<java.util.Map<?, ?>> list = mapConfig.getMapList("spawners." + type);
            
            if (list == null || list.isEmpty()) {
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.no_spawners", "type", type));
                return;
            }

            if (index < 0 || index >= list.size()) {
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.spawner_index_error", "max", String.valueOf(list.size() - 1)));
                return;
            }
            
            list.remove(index);
            mapConfig.set("spawners." + type, list);
            saveMapConfig();
            
            player.sendMessage(LanguageManager.getInstance().getMessage("command.list.remove_spawner_success", "type", type, "index", String.valueOf(index)));

        } catch (NumberFormatException e) {
            player.sendMessage(LanguageManager.getInstance().getMessage("command.list.invalid_index"));
        }
    }

    private void handleListSpawners(Player player) {
        player.sendMessage(LanguageManager.getInstance().getMessage("command.list.spawners_header"));
        String[] types = {"iron", "gold", "diamond", "emerald"};

        for (String type : types) {
            List<java.util.Map<?, ?>> list = mapConfig.getMapList("spawners." + type);

            if (list == null || list.isEmpty()) {
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.spawner_empty", "type", type));
                continue;
            }
            
            player.sendMessage(ChatColor.YELLOW + type + ":");
            for (int i = 0; i < list.size(); i++) {
                @SuppressWarnings("unchecked")
                Location loc = Location.deserialize((java.util.Map<String, Object>) list.get(i));
                player.sendMessage(String.format("  §7[%d] (%.1f, %.1f, %.1f)",
                    i, loc.getX(), loc.getY(), loc.getZ()));
            }
        }
    }

    private void handleRemoveNPC(Player player, String type, String indexStr) {
        try {
            int index = Integer.parseInt(indexStr);
            List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs." + type);
            
            if (list == null || list.isEmpty()) {
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.no_npcs", "type", type));
                return;
            }

            if (index < 0 || index >= list.size()) {
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.spawner_index_error", "max", String.valueOf(list.size() - 1)));
                return;
            }

            list.remove(index);
            mapConfig.set("npcs." + type, list);
            saveMapConfig();

            player.sendMessage(LanguageManager.getInstance().getMessage("command.list.remove_npc_success", "type", type, "index", String.valueOf(index)));

        } catch (NumberFormatException e) {
            player.sendMessage(LanguageManager.getInstance().getMessage("command.list.invalid_index"));
        }
    }

    private void handleListNPCs(Player player) {
        player.sendMessage(LanguageManager.getInstance().getMessage("command.list.npcs_header"));
        String[] types = {"shop", "upgrade"};

        for (String type : types) {
            List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs." + type);

            if (list == null || list.isEmpty()) {
                player.sendMessage(LanguageManager.getInstance().getMessage("command.list.npc_empty", "type", type));
                continue;
            }
            
            player.sendMessage(ChatColor.YELLOW + type + ":");
            for (int i = 0; i < list.size(); i++) {
                @SuppressWarnings("unchecked")
                Location loc = Location.deserialize((java.util.Map<String, Object>) list.get(i));
                player.sendMessage(String.format("  §7[%d] (%.1f, %.1f, %.1f)",
                    i, loc.getX(), loc.getY(), loc.getZ()));
            }
        }
    }

    public FileConfiguration getMapConfig() {
        return mapConfig;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("help");
            completions.add("setjoin");
            completions.add("setrespawning");
            completions.add("setbed");
            completions.add("removebed");
            completions.add("listbeds");
            completions.add("setspawn");
            completions.add("addnpc");
            completions.add("setspawner");
            completions.add("removenpc");
            completions.add("removespawner");
            completions.add("listnpcs");
            completions.add("listspawners");
            completions.add("pos1");
            completions.add("pos2");
            completions.add("addprotect");
            completions.add("removeprotect");
            completions.add("listprotect");
            completions.add("save");
            completions.add("setmode");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setbed", "setspawn", "removebed" -> completions.addAll(validTeams);
                case "addnpc", "removenpc" -> {
                    completions.add("shop");
                    completions.add("upgrade");
                }
                case "setspawner", "removespawner" -> {
                    completions.add("iron");
                    completions.add("gold");
                    completions.add("diamond");
                    completions.add("emerald");
                }
                case "addprotect" -> {
                }
                case "removeprotect" -> {
                    var areasSection = mapConfig.getConfigurationSection("protection");
                    if (areasSection != null) {
                        completions.addAll(areasSection.getKeys(false));
                    }
                }
                case "setmode" -> {
                    completions.add("solo");
                    completions.add("double");
                    completions.add("3s");
                    completions.add("4s");
                    completions.add("4v4");
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "removenpc" -> {
                    String type = args[1].toLowerCase();
                    List<java.util.Map<?, ?>> list = mapConfig.getMapList("npcs." + type);
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            completions.add(String.valueOf(i));
                        }
                    }
                }
                case "removespawner" -> {
                    String type = args[1].toLowerCase();
                    List<java.util.Map<?, ?>> list = mapConfig.getMapList("spawners." + type);
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            completions.add(String.valueOf(i));
                        }
                    }
                }
            }
        }
        
        return completions;
    }

    private Location getLocationFromArgs(Player player, String[] args, int startIndex) {
        if (args.length <= startIndex) {
            Location loc = player.getLocation();
            return new Location(loc.getWorld(),
                Math.floor(loc.getX()),
                Math.floor(loc.getY()),
                Math.floor(loc.getZ()),
                loc.getYaw(),
                loc.getPitch()
            );
        }

        try {
            double x = Math.floor(Double.parseDouble(args[startIndex]));
            double y = Math.floor(Double.parseDouble(args[startIndex + 1]));
            double z = Math.floor(Double.parseDouble(args[startIndex + 2]));
            float yaw = args.length > startIndex + 3 ? Float.parseFloat(args[startIndex + 3]) : player.getLocation().getYaw();
            float pitch = args.length > startIndex + 4 ? Float.parseFloat(args[startIndex + 4]) : player.getLocation().getPitch();

            return new Location(player.getWorld(), x, y, z, yaw, pitch);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Location loc = player.getLocation();
            return new Location(loc.getWorld(),
                Math.floor(loc.getX()),
                Math.floor(loc.getY()),
                Math.floor(loc.getZ()),
                loc.getYaw(),
                loc.getPitch()
            );
        }
    }

    private void handleListBeds(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 床位置列表 ===");

        boolean hasAnyBed = false;
        for (String team : validTeams) {
            if (mapConfig.contains("beds." + team)) {
                hasAnyBed = true;
                Location loc = getBedLocation(team);
                if (loc != null) {
                    player.sendMessage(String.format("§e%s§r: §7(%.1f, %.1f, %.1f)",
                        team, loc.getX(), loc.getY(), loc.getZ()));
                }
            }
        }

        if (!hasAnyBed) {
            player.sendMessage(LanguageManager.getInstance().getMessage("command.list.no_beds"));
        }
    }

    public Location getBedLocation(String team) {
        if (!mapConfig.contains("beds." + team)) {
            return null;
        }
        
        org.bukkit.configuration.ConfigurationSection section = mapConfig.getConfigurationSection("beds." + team);
        if (section == null) {
            return null;
        }
        
        return new Location(
            plugin.getServer().getWorld(section.getString("world")),
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );
    }

    public String getTeamByBedLocation(Location location) {
        Block clickedBlock = location.getBlock();
        
        for (String team : validTeams) {
            Location bedLoc = getBedLocation(team);
            if (bedLoc != null) {
                if (isSameBlock(bedLoc, location)) {
                    return team;
                }
                
                if (clickedBlock.getType().name().endsWith("_BED") &&
                    clickedBlock.getBlockData() instanceof Bed) {
                    
                    Bed bedData = (Bed) clickedBlock.getBlockData();
                    Block otherHalf = getOtherBedHalf(clickedBlock, bedData);
                    
                    if (otherHalf != null && isSameBlock(bedLoc, otherHalf.getLocation())) {
                        return team;
                    }
                }
            }
        }
        return null;
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

    private boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) {
            return false;
        }
        return Math.floor(loc1.getX()) == Math.floor(loc2.getX()) &&
               Math.floor(loc1.getY()) == Math.floor(loc2.getY()) &&
               Math.floor(loc1.getZ()) == Math.floor(loc2.getZ());
    }

    public List<String> getTeamsWithBeds() {
        List<String> teams = new ArrayList<>();
        for (String team : validTeams) {
            if (mapConfig.contains("beds." + team)) {
                teams.add(team);
            }
        }
        return teams;
    }
}

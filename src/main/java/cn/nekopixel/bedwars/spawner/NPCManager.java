package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import cn.nekopixel.bedwars.setup.Map;
import cn.nekopixel.bedwars.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

public class NPCManager implements Listener {
    private final Main plugin;

    private final Set<Villager> shopNPCs = new HashSet<>();
    private final Set<Villager> upgradeNPCs = new HashSet<>();
    private final java.util.Map<Villager, List<ArmorStand>> npcNameStands = new HashMap<>();
    private double hologramHeight;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.hologramHeight = plugin.getConfig().getDouble("npc.hologram_height", 2.0);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initializeNPCs();
    }

    private void initializeNPCs() {
        plugin.getServer().getWorlds().forEach(world -> {
            world.getEntities().stream()
                .filter(entity -> entity instanceof Villager || entity instanceof ArmorStand)
                .forEach(entity -> entity.remove());
        });
        
        shopNPCs.clear();
        upgradeNPCs.clear();
        npcNameStands.clear();
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.INGAME) {
            spawnNPCs();
        } else {
            removeNPCs();
            plugin.getServer().getWorlds().forEach(world -> {
                world.getEntities().stream()
                    .filter(entity -> entity instanceof Villager || entity instanceof ArmorStand)
                    .forEach(entity -> entity.remove());
            });
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager || event.getEntity() instanceof ArmorStand) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void spawnNPCs() {
        Map mapSetup = Plugin.getInstance().getMapSetup();
        if (mapSetup == null) return;
        ConfigurationSection mapConfig = mapSetup.getMapConfig();
        if (mapConfig == null) return;

        List<?> shopList = mapConfig.getMapList("npcs.shop");
        for (Object locationMap : shopList) {
            if (locationMap instanceof java.util.Map) {
                java.util.Map<String, Object> locMap = (java.util.Map<String, Object>) locationMap;
                String worldName = (String) locMap.get("world");
                if (worldName != null && plugin.getServer().getWorld(worldName) != null) {
                    Location loc = Location.deserialize(locMap);
                    spawnNPC(loc, "§b道具商店", "§e右键点击", true);
                }
            }
        }

        List<?> upgradeList = mapConfig.getMapList("npcs.upgrade");
        for (Object locationMap : upgradeList) {
            if (locationMap instanceof java.util.Map) {
                java.util.Map<String, Object> locMap = (java.util.Map<String, Object>) locationMap;
                String worldName = (String) locMap.get("world");
                if (worldName != null && plugin.getServer().getWorld(worldName) != null) {
                    Location loc = Location.deserialize(locMap);
                    
                    String mode = plugin.getConfig().getString("game.mode", "4s");
                    String modeText = mode.equalsIgnoreCase("solo") ? "§b单挑模式" : "§b团队模式";
                    
                    spawnUpgradeNPC(loc, "§b升级", "§e右键点击", modeText);
                }
            }
        }
    }

    private void spawnNPC(Location location, String title, String subtitle, boolean isShop) {
        Location safeLocation = LocationUtils.findSafeLocation(location, 3);
        Location spawnLoc = new Location(safeLocation.getWorld(),
            Math.floor(safeLocation.getX()) + 0.5,
            Math.floor(safeLocation.getY()),
            Math.floor(safeLocation.getZ()) + 0.5,
            safeLocation.getYaw(),
            safeLocation.getPitch()
        );
        
        Villager villager = (Villager) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCustomNameVisible(false);
        
        if (isShop) {
            shopNPCs.add(villager);
        } else {
            upgradeNPCs.add(villager);
        }
        
        List<ArmorStand> nameStands = new ArrayList<>();
        Location nameLoc = spawnLoc.clone().add(0, hologramHeight, 0);
        
        ArmorStand titleStand = createNameStand(nameLoc, title);
        nameStands.add(titleStand);
        
        ArmorStand subtitleStand = createNameStand(nameLoc.clone().add(0, -0.3, 0), subtitle);
        nameStands.add(subtitleStand);
        
        npcNameStands.put(villager, nameStands);
    }

    private void spawnUpgradeNPC(Location location, String title, String subtitle, String modeText) {
        Location safeLocation = LocationUtils.findSafeLocation(location, 3);
        Location spawnLoc = new Location(safeLocation.getWorld(),
            Math.floor(safeLocation.getX()) + 0.5,
            Math.floor(safeLocation.getY()),
            Math.floor(safeLocation.getZ()) + 0.5,
            safeLocation.getYaw(),
            safeLocation.getPitch()
        );
        
        Villager villager = (Villager) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCustomNameVisible(false);
        
        upgradeNPCs.add(villager);
        
        List<ArmorStand> nameStands = new ArrayList<>();
        Location nameLoc = spawnLoc.clone().add(0, hologramHeight, 0);
        
        ArmorStand modeStand = createNameStand(nameLoc.clone().add(0, 0.3, 0), modeText);
        nameStands.add(modeStand);
        
        ArmorStand titleStand = createNameStand(nameLoc, title);
        nameStands.add(titleStand);
        
        ArmorStand subtitleStand = createNameStand(nameLoc.clone().add(0, -0.3, 0), subtitle);
        nameStands.add(subtitleStand);
        
        npcNameStands.put(villager, nameStands);
    }

    private ArmorStand createNameStand(Location location, String name) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setMarker(true);
        return stand;
    }

    private void removeNPCs() {
        npcNameStands.values().forEach(stands -> stands.forEach(ArmorStand::remove));
        npcNameStands.clear();
        
        shopNPCs.forEach(Villager::remove);
        upgradeNPCs.forEach(Villager::remove);
        shopNPCs.clear();
        upgradeNPCs.clear();
    }

    public boolean isShopNPC(Villager villager) {
        return shopNPCs.contains(villager);
    }

    public boolean isUpgradeNPC(Villager villager) {
        return upgradeNPCs.contains(villager);
    }
}
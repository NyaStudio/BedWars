package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import cn.nekopixel.bedwars.setup.Map;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class NPCManager implements Listener {
    private final Main plugin;
    private final Map mapSetup;

    private final Set<Villager> shopNPCs = new HashSet<>();
    private final Set<Villager> upgradeNPCs = new HashSet<>();
    private final java.util.Map<Villager, List<ArmorStand>> npcNameStands = new HashMap<>();
    private BukkitRunnable correctionTask;

    private static final int CORRECTION_INTERVAL = 100;
    private static final double CORRECTION_THRESHOLD = 0.1;
    private static final double HOLOGRAM_HEIGHT = 2.3;
    private final Set<Location> lastPositions = new HashSet<>();

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.mapSetup = new Map(plugin);
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
            startCorrectionTask();
        } else {
            stopCorrectionTask();
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
                    spawnNPC(loc, "§b升级", "§e右键点击", false);
                }
            }
        }
    }

    private void spawnNPC(Location location, String title, String subtitle, boolean isShop) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCustomNameVisible(false);
        
        if (isShop) {
            shopNPCs.add(villager);
        } else {
            upgradeNPCs.add(villager);
        }
        
        List<ArmorStand> nameStands = new ArrayList<>();
        Location nameLoc = location.clone().add(0, 2.1, 0);
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

    private void startCorrectionTask() {
        if (correctionTask != null) {
            correctionTask.cancel();
        }

        correctionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (GameManager.getInstance().isStatus(GameStatus.INGAME)) {
                    correctNPCs(shopNPCs, true);
                    correctNPCs(upgradeNPCs, false);
                }
            }
        };
        correctionTask.runTaskTimer(plugin, CORRECTION_INTERVAL, CORRECTION_INTERVAL);
    }

    private void stopCorrectionTask() {
        if (correctionTask != null) {
            correctionTask.cancel();
            correctionTask = null;
        }
    }

    private void correctNPCs(Set<Villager> npcSet, boolean isShop) {
        for (Villager npc : npcSet) {
            if (npc == null || !npc.isValid()) continue;

            Location currentLoc = npc.getLocation();
            Location targetLoc = getTargetLocation(npc, isShop);

            if (targetLoc == null) continue;

            // 检查位置是否在阈值范围内
            if (isLocationChanged(currentLoc, targetLoc)) {
                // 使用teleport而不是setLocation以提高性能
                npc.teleport(targetLoc);
                updateNameStands(npc, targetLoc);
            }
        }
    }

    private boolean isLocationChanged(Location current, Location target) {
        return Math.abs(current.getX() - target.getX()) > CORRECTION_THRESHOLD ||
               Math.abs(current.getY() - target.getY()) > CORRECTION_THRESHOLD ||
               Math.abs(current.getZ() - target.getZ()) > CORRECTION_THRESHOLD;
    }

    private Location getTargetLocation(Villager npc, boolean isShop) {
        // 从配置中获取目标位置
        // 这里需要根据你的具体实现来获取正确的目标位置
        return null; // 临时返回null，需要根据实际情况实现
    }

    private void updateNameStands(Villager npc, Location newLoc) {
        List<ArmorStand> stands = npcNameStands.get(npc);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null && stand.isValid()) {
                    stand.teleport(newLoc.clone().add(0, HOLOGRAM_HEIGHT, 0));
                }
            }
        }
    }

    public boolean isShopNPC(Villager villager) {
        return shopNPCs.contains(villager);
    }

    public boolean isUpgradeNPC(Villager villager) {
        return upgradeNPCs.contains(villager);
    }
}
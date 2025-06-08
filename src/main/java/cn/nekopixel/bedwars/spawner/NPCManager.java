package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
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
        if (correctionTask != null) correctionTask.cancel();

        correctionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getCurrentStatus() != GameStatus.INGAME) return;

                correctNPCs(shopNPCs, true);
                correctNPCs(upgradeNPCs, false);
            }
        };
        correctionTask.runTaskTimer(plugin, 0L, 100L); // 每 5 秒一次
    }

    private void stopCorrectionTask() {
        if (correctionTask != null) {
            correctionTask.cancel();
            correctionTask = null;
        }
    }

    private void correctNPCs(Set<Villager> npcSet, boolean isShop) {
        List<Villager> toRemove = new ArrayList<>();
        
        for (Villager npc : npcSet) {
            if (npc == null || npc.isDead() || !npc.isValid()) {
                toRemove.add(npc);
            }
        }

        for (Villager npc : toRemove) {
            if (npcNameStands.containsKey(npc)) {
                npcNameStands.get(npc).forEach(ArmorStand::remove);
                npcNameStands.remove(npc);
            }
            
            npc.remove();
            npcSet.remove(npc);
            Location loc = npc.getLocation();
            spawnNPC(loc, isShop ? "§b道具商店" : "§b升级", "§e右键点击", isShop);
        }
    }

    public boolean isShopNPC(Villager villager) {
        return shopNPCs.contains(villager);
    }

    public boolean isUpgradeNPC(Villager villager) {
        return upgradeNPCs.contains(villager);
    }
}
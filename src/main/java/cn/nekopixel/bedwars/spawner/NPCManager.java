package cn.nekopixel.bedwars.spawner;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class NPCManager implements Listener {
    private final Main plugin;
    private final List<Villager> shopNPCs;
    private final List<Villager> upgradeNPCs;

    public NPCManager(Main plugin) {
        this.plugin = plugin;
        this.shopNPCs = new ArrayList<>();
        this.upgradeNPCs = new ArrayList<>();
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.INGAME) {
            spawnNPCs();
        } else {
            removeNPCs();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager) {
            event.setCancelled(true);
        }
    }

    private void spawnNPCs() {
        ConfigurationSection mapConfig = plugin.getConfig().getConfigurationSection("map");
        if (mapConfig == null) return;

        // shop
        ConfigurationSection shopSection = mapConfig.getConfigurationSection("npcs.shop");
        if (shopSection != null) {
            for (String key : shopSection.getKeys(false)) {
                ConfigurationSection locationSection = shopSection.getConfigurationSection(key);
                if (locationSection != null) {
                    Location location = new Location(
                            plugin.getServer().getWorld(locationSection.getString("world")),
                            locationSection.getDouble("x"),
                            locationSection.getDouble("y"),
                            locationSection.getDouble("z"),
                            (float) locationSection.getDouble("yaw"),
                            (float) locationSection.getDouble("pitch")
                    );
                    spawnShopNPC(location);
                }
            }
        }

        // upgrade
        ConfigurationSection upgradeSection = mapConfig.getConfigurationSection("npcs.upgrade");
        if (upgradeSection != null) {
            for (String key : upgradeSection.getKeys(false)) {
                ConfigurationSection locationSection = upgradeSection.getConfigurationSection(key);
                if (locationSection != null) {
                    Location location = new Location(
                            plugin.getServer().getWorld(locationSection.getString("world")),
                            locationSection.getDouble("x"),
                            locationSection.getDouble("y"),
                            locationSection.getDouble("z"),
                            (float) locationSection.getDouble("yaw"),
                            (float) locationSection.getDouble("pitch")
                    );
                    spawnUpgradeNPC(location);
                }
            }
        }
    }

    private void spawnShopNPC(Location location) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCustomName("§b道具商店\n§e右键点击");
        villager.setCustomNameVisible(true);
        shopNPCs.add(villager);
    }

    private void spawnUpgradeNPC(Location location) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCustomName("§b升级\n§e右键点击");
        villager.setCustomNameVisible(true);
        upgradeNPCs.add(villager);
    }

    private void removeNPCs() {
        shopNPCs.forEach(Villager::remove);
        upgradeNPCs.forEach(Villager::remove);
        shopNPCs.clear();
        upgradeNPCs.clear();
    }
} 
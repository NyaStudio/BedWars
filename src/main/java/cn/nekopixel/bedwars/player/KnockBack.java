package cn.nekopixel.bedwars.player;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.game.GameManager;
import cn.nekopixel.bedwars.game.GameStatus;
import cn.nekopixel.bedwars.game.GameStatusChange;
import cn.nekopixel.bedwars.game.PlayerDeathManager;
import cn.nekopixel.bedwars.game.SpectatorManager;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KnockBack implements Listener {
    private final Main plugin;
    
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private final Map<UUID, Boolean> continuousAttack = new HashMap<>();
    private final Map<UUID, Boolean> lastSprinting = new HashMap<>();
    
    private static final double KNOCKBACK_NORMAL = 1.98;
    private static final double KNOCKBACK_MOVING = 1.70;
    private static final double KNOCKBACK_SPRINT_FIRST = 4.94;
    private static final double KNOCKBACK_SPRINT_CONTINUOUS = 1.98;
    private static final double KNOCKBACK_SPRINT_RESET_MIN = 4.94;
    private static final double KNOCKBACK_SPRINT_RESET_MAX = 6.73;
    
    private static final long CONTINUOUS_ATTACK_THRESHOLD = 500;
    private static final double KNOCKBACK_ENCHANT_MULTIPLIER = 0.5;
    
    public KnockBack(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!GameManager.getInstance().isStatus(GameStatus.INGAME)) {
            return;
        }
        
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        if (event.isCancelled()) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        if (!plugin.getConfig().getBoolean("game.friendly_fire", false)) {
            TeamManager teamManager = GameManager.getInstance().getTeamManager();
            String attackerTeam = teamManager.getPlayerTeam(attacker);
            String victimTeam = teamManager.getPlayerTeam(victim);
            
            if (attackerTeam != null && attackerTeam.equals(victimTeam)) {
                return;
            }
        }
        
        PlayerDeathManager deathManager = GameManager.getInstance().getPlayerDeathManager();
        SpectatorManager spectatorManager = GameManager.getInstance().getSpectatorManager();
        
        if (deathManager.isRespawning(attacker.getUniqueId()) || spectatorManager.isSpectator(attacker)) {
            return;
        }
        
        double knockbackStrength = calculateKnockback(attacker);
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon != null) {
            int knockbackLevel = weapon.getEnchantmentLevel(Enchantment.KNOCKBACK);
            if (knockbackLevel > 0) {
                knockbackStrength += knockbackLevel * KNOCKBACK_ENCHANT_MULTIPLIER;
            }
        }
        
        applyKnockback(attacker, victim, knockbackStrength);
        updateAttackState(attacker);
    }

    private double calculateKnockback(Player attacker) {
        UUID attackerId = attacker.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        Long lastAttack = lastAttackTime.get(attackerId);
        boolean isContinuous = lastAttack != null && 
            (currentTime - lastAttack) <= CONTINUOUS_ATTACK_THRESHOLD;
        
        boolean isSprinting = attacker.isSprinting();
        boolean isMoving = attacker.getVelocity().lengthSquared() > 0.01;
        boolean wasSprintingBefore = lastSprinting.getOrDefault(attackerId, false);
        
        double knockback;
        
        if (isSprinting) {
            if (!isContinuous) {
                if (!wasSprintingBefore) {
                    knockback = KNOCKBACK_SPRINT_FIRST;
                } else {
                    if (!attacker.isOnGround()) {
                        knockback = KNOCKBACK_SPRINT_RESET_MIN +
                            (KNOCKBACK_SPRINT_RESET_MAX - KNOCKBACK_SPRINT_RESET_MIN) * 0.5;
                    } else {
                        knockback = KNOCKBACK_SPRINT_RESET_MIN;
                    }
                }
            } else {
                knockback = KNOCKBACK_SPRINT_CONTINUOUS;
            }
        } else {
            if (isMoving && isContinuous) {
                knockback = KNOCKBACK_MOVING;
            } else {
                knockback = KNOCKBACK_NORMAL;
            }
        }
        
        continuousAttack.put(attackerId, isContinuous);
        
        return knockback;
    }

    private void applyKnockback(Player attacker, Player victim, double strength) {
        Vector direction = victim.getLocation().toVector()
            .subtract(attacker.getLocation().toVector());
        
        direction.setY(0);
        
        if (direction.lengthSquared() < 0.01) {
            direction = attacker.getLocation().getDirection();
            direction.setY(0);
        }
        
        direction.normalize();
        
        double horizontalVelocity = strength * 0.4;
        direction.multiply(horizontalVelocity);
        direction.setY(0.4);

        Vector currentVelocity = victim.getVelocity();
        Vector finalVelocity = currentVelocity.multiply(0.2).add(direction);
        victim.setVelocity(finalVelocity);
        
        if (plugin.getConfig().getBoolean("debug.knockback", false)) {
            String attackState = attacker.isSprinting() ? "疾跑" : "普通";
            boolean continuous = continuousAttack.getOrDefault(attacker.getUniqueId(), false);
            ItemStack debugWeapon = attacker.getInventory().getItemInMainHand();
            int kbEnchant = (debugWeapon != null) ? debugWeapon.getEnchantmentLevel(Enchantment.KNOCKBACK) : 0;
            plugin.getLogger().info(String.format(
                "击退计算: %s -> %s | 状态:%s 连续:%s 击退:%.2f格 击退附魔:%d",
                attacker.getName(), victim.getName(), 
                attackState, continuous ? "true" : "false", strength, kbEnchant
            ));
        }
    }

    private void updateAttackState(Player attacker) {
        UUID attackerId = attacker.getUniqueId();
        
        lastAttackTime.put(attackerId, System.currentTimeMillis());
        lastSprinting.put(attackerId, attacker.isSprinting());
        
        long currentTime = System.currentTimeMillis();
        lastAttackTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 30000);
        continuousAttack.entrySet().removeIf(entry -> 
            !lastAttackTime.containsKey(entry.getKey()));
        lastSprinting.entrySet().removeIf(entry -> 
            !lastAttackTime.containsKey(entry.getKey()));
    }

    @EventHandler
    public void onGameStatusChange(GameStatusChange event) {
        if (event.getNewStatus() == GameStatus.RESETTING || 
            event.getNewStatus() == GameStatus.WAITING) {
            lastAttackTime.clear();
            continuousAttack.clear();
            lastSprinting.clear();
        }
    }
}
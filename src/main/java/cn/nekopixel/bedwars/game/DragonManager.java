package cn.nekopixel.bedwars.game;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DragonManager {
    private final Main plugin;
    private final Map<String, EnderDragon> teamDragons = new HashMap<>();
    private FileConfiguration chattingConfig;
    
    public DragonManager(Main plugin) {
        this.plugin = plugin;
        loadChattingConfig();
    }
    
    private void loadChattingConfig() {
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "chatting.yml");
            if (file.exists()) {
                chattingConfig = YamlConfiguration.loadConfiguration(file);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("无法加载 chatting.yml: " + e.getMessage());
        }
    }
    
    public void spawnDragons(Location baseLocation) {
        TeamManager teamManager = GameManager.getInstance().getTeamManager();
        RespawnManager respawnManager = new RespawnManager(plugin, GameManager.getInstance().getPlayerDeathManager());
        
        if (baseLocation == null) {
            plugin.getLogger().warning("未找到 respawning 位置，无法生成末影龙");
            return;
        }
        
        Location dragonLoc = baseLocation.clone().add(0, -25, 0);
        
        Set<String> teams = teamManager.getConfigTeams();
        int teamIndex = 0;
        for (String team : teams) {
            List<Player> alivePlayers = teamManager.getAlivePlayersInTeam(team);
            
            if (!alivePlayers.isEmpty()) {
                // 给每个龙创建完全分散的初始位置，防止卡住
                double angle = (teamIndex * 72 + Math.random() * 20) * Math.PI / 180; // 每个龙间隔72度 + 随机偏移
                double distance = 80 + (teamIndex * 25) + Math.random() * 20; // 递增距离 + 随机偏移
                double x = dragonLoc.getX() + Math.cos(angle) * distance;
                double z = dragonLoc.getZ() + Math.sin(angle) * distance;
                double y = dragonLoc.getY() + (teamIndex * 8) - 15 + Math.random() * 10; // 不同高度
                
                Location spawnLoc = new Location(dragonLoc.getWorld(), x, y, z);
                EnderDragon dragon = (EnderDragon) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
                
                String tablistTeamName = getTablistTeamName(team);
                dragon.setCustomName(getTeamChatColor(team) + tablistTeamName + "队末影龙");
                dragon.setCustomNameVisible(true);
                dragon.setPhase(EnderDragon.Phase.CIRCLING);
                
                // 给每个龙给不同的移动方向
                Vector initialVelocity = new Vector(
                    Math.cos(angle + Math.PI/2),
                    (Math.random() - 0.5) * 0.5,
                    Math.sin(angle + Math.PI/2)
                ).normalize().multiply(1.5);
                
                // 使用初始速度推动，防止龙不动弹
                dragon.setVelocity(initialVelocity);
                
                teamDragons.put(team, dragon);
                setupDragonAI(dragon, team);
                teamIndex++;
            }
        }
    }
    
    private void setupDragonAI(EnderDragon dragon, String ownerTeam) {
        new BukkitRunnable() {
            private Location centerLocation = dragon.getLocation().clone();
            private Location currentTarget = null;
            private int targetChangeTimer = 0;
            private int lastPlayerHitTick = 0;
            private Location lastPosition = dragon.getLocation().clone();
            private int stuckTimer = 0;
            private int stuckCheckInterval = 60;
            private int lastInterventionTick = 0;
            private int interventionCooldown = 50;
            private Vector lastVelocity = new Vector(0, 0, 0);
            private double maxVelocity = 2.5;
            
            @Override
            public void run() {
                if (dragon.isDead() || !dragon.isValid()) {
                    teamDragons.remove(ownerTeam);
                    this.cancel();
                    return;
                }
                
                Location dragonLoc = dragon.getLocation();
                boolean needsIntervention = false;
                lastInterventionTick++;
                
                if (lastInterventionTick >= interventionCooldown) {
                    if (isOutOfBounds(dragonLoc)) {
                        handleOutOfBounds(dragon, dragonLoc);
                        needsIntervention = true;
                    }
                    else {
                        if (checkIfStuck(dragon)) {
                            needsIntervention = true;
                        }
                        else if (handleDragonCollisions(dragon)) {
                            needsIntervention = true;
                        }
                        else if (enforceHeightLimits(dragon)) {
                            needsIntervention = true;
                        }
                    }
                    
                    if (needsIntervention) {
                        lastInterventionTick = 0;
                    }
                }
                
                DestroyBlocks(dragon);
                
                if (lastPlayerHitTick <= 0) {
                    checkPlayerCollision(dragon, ownerTeam);
                    lastPlayerHitTick = 8;
                } else {
                    lastPlayerHitTick--;
                }
                
                // 自然移动
                handleNaturalMovement(dragon);
            }
            
            private void handleNaturalMovement(EnderDragon dragon) {
                targetChangeTimer++;
                
                boolean needNewTarget = currentTarget == null ||
                                      dragon.getLocation().distance(currentTarget) < 8 || 
                                      targetChangeTimer > 200;
                
                if (needNewTarget) {
                    // 70% 概率斜对角，30% 概率随机
                    if (Math.random() < 0.7) {
                        currentTarget = generateDiagonalTarget(dragon.getLocation());
                    } else {
                        currentTarget = generateRandomTarget();
                    }
                    targetChangeTimer = 0;
                    
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
                
                // 诈骗一下龙
                if (currentTarget != null) {
                    double distance = dragon.getLocation().distance(currentTarget);
                    
                    // 检查龙的前进路径上是否有建筑，如果有就推一把
                    Location dragonLoc = dragon.getLocation();
                    Vector toTarget = currentTarget.toVector().subtract(dragonLoc.toVector()).normalize();
                    
                    // 检查前方是否有建筑物
                    for (int checkDist = 5; checkDist <= 20; checkDist += 5) {
                        Location checkLoc = dragonLoc.clone().add(toTarget.clone().multiply(checkDist));
                        int nearbyBlocks = countNearbySolidBlocks(checkLoc, 3);
                        
                        if (nearbyBlocks > 10 && Math.random() < 0.4) {
                            Vector boostVelocity = toTarget.clone().multiply(1.8);
                            setSmoothVelocity(dragon, boostVelocity);
                            break;
                        }
                    }
                    
                    if (distance > 50 && targetChangeTimer % 20 == 0) {
                        Vector nudgeVelocity = currentTarget.toVector().subtract(dragon.getLocation().toVector());
                        nudgeVelocity.normalize();
                        nudgeVelocity.multiply(0.6);
                        
                        if (Math.random() < 0.5) {
                            setSmoothVelocity(dragon, nudgeVelocity);
                        }
                    }
                }
                
                // 保持在盘旋状态
                if (dragon.getPhase() != EnderDragon.Phase.CIRCLING) {
                    dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
            }
            
            private Location generateRandomTarget() {
                double range = 100;
                org.bukkit.World world = centerLocation.getWorld();
                
                // 首先尝试寻找建筑密集区域作为陷阱
                Location bestTarget = null;
                int maxSolidCount = 0;
                
                // 多次采样，寻找最佳的建筑密集区域
                for (int i = 0; i < 15; i++) {
                    double x = centerLocation.getX() + (Math.random() - 0.5) * range * 2;
                    double z = centerLocation.getZ() + (Math.random() - 0.5) * range * 2;
                    
                    // 在不同高度层寻找建筑
                    for (int yOffset = -10; yOffset <= 30; yOffset += 5) {
                        double y = centerLocation.getY() + yOffset;
                        Location candidate = new Location(world, x, y, z);
                        int solidCount = countNearbySolidBlocks(candidate, 6);
                        
                        // 加权评分：建筑密度 + 随机因子
                        int score = solidCount + (int)(Math.random() * 10);
                        
                        if (score > maxSolidCount) {
                            maxSolidCount = score;
                            bestTarget = candidate;
                        }
                    }
                }
                
                if (bestTarget != null && maxSolidCount > 25) {
                    // 欺骗龙以为这是空地
                    Location trapTarget = bestTarget.clone().add(
                        (Math.random() - 0.5) * 10,
                        Math.random() * 3 + 2,
                        (Math.random() - 0.5) * 10
                    );
                    
                    double y = trapTarget.getY();
                    y = Math.max(-45, Math.min(y, 100));  // 绝对坐标限制
                    trapTarget.setY(y);
                    
                    return trapTarget;
                }
                
                // 回退目标
                double x = centerLocation.getX() + (Math.random() - 0.5) * range * 2;
                double z = centerLocation.getZ() + (Math.random() - 0.5) * range * 2;
                double y = centerLocation.getY() + (Math.random() - 0.6) * 80;
                
                y = Math.max(-45, Math.min(y, 100));  // 绝对坐标限制
                
                return new Location(world, x, y, z);
            }

            private int countNearbySolidBlocks(Location loc, int radius) {
                int count = 0;
                org.bukkit.World world = loc.getWorld();

                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block block = world.getBlockAt(loc.clone().add(x, y, z));
                            if (block.getType().isSolid()) count++;
                        }
                    }
                }

                return count;
            }

            private Location generateDiagonalTarget(Location fromLoc) {
                double range = 80;
                
                double[] angles = {45, 135, 225, 315, 0, 90, 180, 270};
                double selectedAngle = angles[(int)(Math.random() * angles.length)];
                selectedAngle = Math.toRadians(selectedAngle);
                
                double distance = range * (0.5 + Math.random() * 0.5);
                double x = fromLoc.getX() + Math.cos(selectedAngle) * distance;
                double z = fromLoc.getZ() + Math.sin(selectedAngle) * distance;
                
                double yChange = (Math.random() - 0.5) * 40;
                double y = fromLoc.getY() + yChange;
                
                y = Math.max(-45, Math.min(y, 100));  // 绝对坐标限制
                
                return new Location(fromLoc.getWorld(), x, y, z);
            }
            
            private void DestroyBlocks(EnderDragon dragon) {
                Vector velocity = dragon.getVelocity();
                destroyBlocksInBounds(dragon, dragon.getBoundingBox());

                if (velocity.lengthSquared() > 0.01) {
                    Vector predictedMovement = velocity.clone().multiply(0.1);
                    org.bukkit.util.BoundingBox predictedBounds = dragon.getBoundingBox().clone();
                    predictedBounds.shift(predictedMovement);

                    destroyBlocksInBounds(dragon, predictedBounds);
                }

                org.bukkit.util.BoundingBox expandedBounds = dragon.getBoundingBox().clone();
                expandedBounds.expand(0.5);
                destroyBlocksInBounds(dragon, expandedBounds);
            }
            
            private void destroyBlocksInBounds(EnderDragon dragon, BoundingBox bounds) {
                int minX = (int) Math.floor(bounds.getMinX());
                int maxX = (int) Math.ceil(bounds.getMaxX());
                int minY = (int) Math.floor(bounds.getMinY());
                int maxY = (int) Math.ceil(bounds.getMaxY());
                int minZ = (int) Math.floor(bounds.getMinZ());
                int maxZ = (int) Math.ceil(bounds.getMaxZ());
                
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            org.bukkit.util.BoundingBox blockBounds = new org.bukkit.util.BoundingBox(
                                x, y, z, x + 1, y + 1, z + 1
                            );
                            
                            if (bounds.overlaps(blockBounds)) {
                                Block block = dragon.getWorld().getBlockAt(x, y, z);
                                
                                if (shouldDestroyBlock(block)) {
                                    Location blockLoc = block.getLocation();
                                    
                                    dragon.getWorld().playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 0.7f, 1.0f);
                                    
                                    dragon.getWorld().spawnParticle(
                                        org.bukkit.Particle.BLOCK_CRACK,
                                        blockLoc.add(0.5, 0.5, 0.5),
                                        8,
                                        0.3, 0.3, 0.3,
                                        0.1,
                                        block.getBlockData()
                                    );
                                    
                                    block.setType(Material.AIR);
                                }
                            }
                        }
                    }
                }
            }
            
            private boolean shouldDestroyBlock(Block block) {
                Material type = block.getType();
                
                if (type == Material.AIR ||
                    type == Material.BEDROCK || 
                    type == Material.BARRIER ||
                    type.name().contains("SPAWNER") ||
                    type.name().contains("CHEST") ||
                    type.name().contains("SHULKER_BOX")) {
                    return false;
                }
                
                return true;
            }
            
            private void checkPlayerCollision(EnderDragon dragon, String ownerTeam) {
                Location dragonLoc = dragon.getLocation();
                TeamManager teamManager = GameManager.getInstance().getTeamManager();
                
                for (Player player : dragon.getWorld().getPlayers()) {
                    if (!player.isOnline() || player.isDead()) continue;
                    
                    String playerTeam = teamManager.getPlayerTeam(player);
                    if (playerTeam != null && playerTeam.equalsIgnoreCase(ownerTeam)) continue;
                    
                    if (GameManager.getInstance().getSpectatorManager().isSpectator(player)) continue;
                    
                    double distance = dragonLoc.distance(player.getLocation());
                    if (distance < 6) {
                        Vector knockback = player.getLocation().toVector().subtract(dragonLoc.toVector());
                        knockback.setY(1.2);
                        knockback.normalize();
                        knockback.multiply(5.0);
                        
                        player.setVelocity(knockback);
                        
                        player.damage(7.0);
                    }
                }
            }
            
            private boolean handleDragonCollisions(EnderDragon dragon) {
                for (Map.Entry<String, EnderDragon> entry : teamDragons.entrySet()) {
                    EnderDragon other = entry.getValue();
                    if (other.equals(dragon) || other.isDead() || !other.isValid()) continue;

                    double distance = dragon.getLocation().distance(other.getLocation());
                    if (distance < 8) {
                        Location dragonLoc = dragon.getLocation();
                        Location otherLoc = other.getLocation();
                        
                        Vector direction = dragonLoc.toVector().subtract(otherLoc.toVector());
                        direction.setY(0);
                        
                        if (direction.lengthSquared() < 0.1) {
                            double randomAngle = Math.random() * 2 * Math.PI;
                            direction = new Vector(Math.cos(randomAngle), 0, Math.sin(randomAngle));
                        } else {
                            direction.normalize();
                        }
                        
                        boolean turnLeft = Math.random() < 0.5;
                        Vector turnDirection;
                        if (turnLeft) {
                            turnDirection = new Vector(-direction.getZ(), 0, direction.getX());
                        } else {
                            turnDirection = new Vector(direction.getZ(), 0, -direction.getX());
                        }
                        
                        Vector currentVelocity = dragon.getVelocity();
                        turnDirection.multiply(1.5);
                        turnDirection.setY(currentVelocity.getY() * 0.8);
                        
                        setSmoothVelocity(dragon, turnDirection);
                        
                        Vector targetDirection = turnDirection.clone().normalize().multiply(20);
                        currentTarget = dragonLoc.add(targetDirection);
                        currentTarget.setY(dragonLoc.getY());
                        targetChangeTimer = 0;
                        
                        return true;
                    }
                }
                return false;
            }
            
            private void handleOutOfBounds(EnderDragon dragon, Location dragonLoc) {
                Vector toCenter = centerLocation.toVector().subtract(dragonLoc.toVector()).normalize();
                toCenter.multiply(1.2);
                setSmoothVelocity(dragon, toCenter);
                
                currentTarget = generateDiagonalTarget(centerLocation);
                targetChangeTimer = 0;
            }
            
            private boolean checkIfStuck(EnderDragon dragon) {
                Location currentPos = dragon.getLocation();
                stuckTimer++;
                
                if (stuckTimer >= stuckCheckInterval) {
                    double distance = currentPos.distance(lastPosition);
                    
                    if (distance < 5.0) {
                        Location unstuckLoc = generateDiagonalTarget(currentPos);
                        Vector pushDirection = unstuckLoc.toVector().subtract(currentPos.toVector()).normalize();
                        pushDirection.multiply(1.8);
                        setSmoothVelocity(dragon, pushDirection);
                        
                        currentTarget = unstuckLoc;
                        targetChangeTimer = 0;
                        
                        lastPosition = currentPos.clone();
                        stuckTimer = 0;
                        return true;
                    }
                    
                    lastPosition = currentPos.clone();
                    stuckTimer = 0;
                }
                return false;
            }
            
            private boolean enforceHeightLimits(EnderDragon dragon) {
                Location dragonLoc = dragon.getLocation();
                double currentY = dragonLoc.getY();
                
                if (currentY > 100) {
                    Vector downwardPush = new Vector(0, -1.2, 0);
                    setSmoothVelocity(dragon, downwardPush);
                    
                    currentTarget = generateDiagonalTarget(dragonLoc);
                    targetChangeTimer = 0;
                    return true;
                }
                
                else if (currentY < -45) {
                    Vector upwardPush = new Vector(0, 0.8, 0);
                    setSmoothVelocity(dragon, upwardPush);
                    return true;
                }
                
                return false;
            }
            
            private boolean isOutOfBounds(Location loc) {
                double maxDistance = 150;
                double minY = -45;
                double maxY = 100;
                
                return loc.distance(centerLocation) > maxDistance || 
                       loc.getY() < minY || 
                       loc.getY() > maxY;
            }
            
            private void setSmoothVelocity(EnderDragon dragon, Vector targetVelocity) {
                if (targetVelocity.length() > maxVelocity) {
                    targetVelocity.normalize().multiply(maxVelocity);
                }
                
                Vector currentVel = dragon.getVelocity();
                Vector smoothVel = currentVel.clone().multiply(0.3).add(targetVelocity.multiply(0.7));
                
                if (smoothVel.length() < 0.3) {
                    smoothVel.normalize().multiply(0.3);
                }
                
                dragon.setVelocity(smoothVel);
                lastVelocity = smoothVel.clone();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    public void removeDragons() {
        for (EnderDragon dragon : teamDragons.values()) {
            if (dragon != null && dragon.isValid()) {
                dragon.remove();
            }
        }
        teamDragons.clear();
    }

    private String getTeamChatColor(String team) {
        return switch (team.toLowerCase()) {
            case "red" -> "§c";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "yellow" -> "§e";
            case "aqua" -> "§b";
            case "white" -> "§f";
            case "pink" -> "§d";
            case "gray" -> "§7";
            default -> "§7";
        };
    }
    
    private String getTablistTeamName(String team) {
        if (chattingConfig != null) {
            String configPath = "tablist.team_names." + team.toLowerCase();
            if (chattingConfig.contains(configPath)) {
                return chattingConfig.getString(configPath);
            }
        }
        
        return switch (team.toLowerCase()) {
            case "red" -> "红";
            case "blue" -> "蓝";
            case "green" -> "绿";
            case "yellow" -> "黄";
            case "aqua" -> "青";
            case "white" -> "白";
            case "pink" -> "粉";
            case "gray" -> "灰";
            default -> team;
        };
    }
}
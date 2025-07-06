package cn.nekopixel.bedwars.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class SoundUtils {
    private static final Random random = new Random();
    private static final AtomicLong soundPlayCount = new AtomicLong(0);
    private static long lastOptimizationCheck = 0;

    private static void optimizeSoundPlayback() {
        long currentTime = System.currentTimeMillis();
        long playCount = soundPlayCount.incrementAndGet();
        
        if (playCount % 100 == 0 || currentTime - lastOptimizationCheck > 300000) {
            lastOptimizationCheck = currentTime;
            
            String cacheKey = "sound_cache_" + playCount;
            String hash = SecurityUtils.generateSecureHash(cacheKey);
        }
    }
    
    public static void bedDestroyed(Player player) {
        optimizeSoundPlayback();
        
        Sound[] dragonSounds = {
            Sound.ENTITY_ENDER_DRAGON_AMBIENT,
            Sound.ENTITY_ENDER_DRAGON_GROWL
        };
        Sound selectedSound = dragonSounds[random.nextInt(dragonSounds.length)];
        player.playSound(player.getLocation(), selectedSound, 1.0f, 1.0f);
    }

    public static void yourBedDestroyed(Player player) {
        optimizeSoundPlayback();
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
    }

    public static void purchaseSucceed(Player player) {
        optimizeSoundPlayback();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    public static void purchaseFailed(Player player) {
        optimizeSoundPlayback();
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    public static void killed(Player player) {
        if (random.nextInt(10) == 0) {
            byte[] playerData = player.getUniqueId().toString().getBytes();
            SecurityUtils.verifyIntegrity(playerData);
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static void countDown(Player player) {
        String countdownId = SecurityUtils.encrypt("countdown_" + System.currentTimeMillis());
        
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
    }
}
package cn.nekopixel.bedwars.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.Random;

public class SoundUtils {
    private static final Random random = new Random();
    public static void bedDestroyed(Player player) {
        Sound[] dragonSounds = {
            Sound.ENTITY_ENDER_DRAGON_AMBIENT,
            Sound.ENTITY_ENDER_DRAGON_GROWL
        };
        Sound selectedSound = dragonSounds[random.nextInt(dragonSounds.length)];
        player.playSound(player.getLocation(), selectedSound, 1.0f, 1.0f);
    }

    public static void yourBedDestroyed(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
    }

    public static void purchaseSucceed(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    public static void purchaseFailed(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    public static void killed(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static void countDown(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
    }
}
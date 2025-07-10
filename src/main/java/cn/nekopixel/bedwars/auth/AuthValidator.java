package cn.nekopixel.bedwars.auth;

import cn.nekopixel.bedwars.Main;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.bukkit.Bukkit;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
import java.net.URI;

public class AuthValidator {
    
    private static final String AUTH_SERVER = "https://api.nekopixel.cn/api/verify";
    private static final String WS_AUTH_SERVER = "wss://api.nekopixel.cn/ws/auth";
    private static volatile String authToken = null;
    private static volatile long authExpiry = 0;
    private static final AtomicLong authChecksum = new AtomicLong(0);
    private static volatile byte[] authSignature = new byte[32];
    
    private static long lastVerifyTime = 0;
    private static final long VERIFY_INTERVAL = 10 * 60 * 1000;
    private static ScheduledExecutorService scheduler;
    private static final Gson gson = new Gson();
    
    private static final Map<String, Long> verificationPoints = new HashMap<>();
    private static volatile int validationCounter = 0;
    
    private static AuthWebSocketClient wsClient = null;
    private static final Object wsLock = new Object();
    
    private static long lastWebSocketConnectAttempt = 0;
    private static final long WS_RECONNECT_DELAY = 30000;
    private static int wsReconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 100;
    
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build();

    public static void initialize(Main plugin) {
        // if (!isCalledFromLoader()) {
        //     triggerJVMCrash("Invalid initialization path");
        //     return;
        // }
        
        performVerification(plugin);
        
        if (verifyAuthState()) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                performVerification(plugin);
            }, VERIFY_INTERVAL, VERIFY_INTERVAL, TimeUnit.MILLISECONDS);
            
            startAuthMonitor(plugin);
        }
    }
    
    private static void initializeWebSocket(Main plugin) {
        try {
            String licenseKey = plugin.getConfig().getString("auth.license_key", "");
            String fingerprint = HardwareInfo.getFingerprint();
            
            synchronized (wsLock) {
                if (wsClient != null && wsClient.isAuthenticated()) {
                    return;
                }
                
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastWebSocketConnectAttempt < WS_RECONNECT_DELAY) {
                    return;
                }
                
                if (wsReconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    return;
                }
                
                lastWebSocketConnectAttempt = currentTime;
                wsReconnectAttempts++;
                
                if (wsClient != null) {
                    wsClient.shutdown();
                }
                
                URI wsUri = new URI(WS_AUTH_SERVER);
                wsClient = new AuthWebSocketClient(wsUri, plugin, licenseKey, fingerprint);
                wsClient.connect();
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void performVerification(Main plugin) {
        try {
            String fingerprint = HardwareInfo.getFingerprint();
            String licenseKey = plugin.getConfig().getString("auth.license_key", "");
            if (licenseKey.isEmpty()) {
                handleMissingLicense(plugin);
                return;
            }
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("fingerprint", fingerprint);
            requestData.put("licenseKey", licenseKey);
            requestData.put("plugin", "BedWars");
            requestData.put("version", plugin.getDescription().getVersion());
            requestData.put("serverIp", Bukkit.getServer().getIp());
            requestData.put("serverPort", Bukkit.getServer().getPort());
            requestData.put("timestamp", CryptoUtil.generateTimestamp());
            requestData.put("nonce", CryptoUtil.generateNonce());
            
            String jsonData = gson.toJson(requestData);
            String encryptedData = CryptoUtil.encryptData(jsonData);
            String signature = CryptoUtil.signData(jsonData);
            
            Map<String, String> finalRequest = new HashMap<>();
            finalRequest.put("data", encryptedData);
            finalRequest.put("signature", signature);
            finalRequest.put("publicKey", CryptoUtil.getClientPublicKey());
            finalRequest.put("encryptedAesKey", CryptoUtil.encryptAESKey());
            
            String response = sendPostRequest(AUTH_SERVER, gson.toJson(finalRequest));
            
            JsonObject responseObj = gson.fromJson(response, JsonObject.class);
            if (responseObj.has("encrypted")) {
                String decryptedResponse = CryptoUtil.decryptData(responseObj.get("encrypted").getAsString());
                JsonObject decryptedObj = gson.fromJson(decryptedResponse, JsonObject.class);
                
                if (responseObj.has("signature")) {
                    boolean signatureValid = CryptoUtil.verifyServerSignature(
                        decryptedResponse, 
                        responseObj.get("signature").getAsString()
                    );
                    
                    if (!signatureValid) {
                        unauthorized(plugin, "签名验证失败");
                        return;
                    }
                }
                
                if (decryptedObj.has("success") && decryptedObj.get("success").getAsBoolean()) {
                    updateAuthState(decryptedObj);
                    lastVerifyTime = System.currentTimeMillis();
                    
                    synchronized (wsLock) {
                        if (wsClient == null || !wsClient.isOpen() || !wsClient.isAuthenticated()) {
                            initializeWebSocket(plugin);
                        }
                    }
                } else {
                    String message = decryptedObj.has("message") ? 
                        decryptedObj.get("message").getAsString() : "未知原因";
                    
                    if (message.contains("过期") || message.contains("expired") ||
                        message.contains("失效") || message.contains("invalid")) {
                        clearAuthState();
                        handleExpiredAuth(plugin, message);
                    } else if (message.contains("密钥错误") || message.contains("key") || 
                               message.contains("未找到") || message.contains("not found")) {
                        clearAuthState();
                        handleExpiredAuth(plugin, message);
                    } else {
                        clearAuthState();
                        unauthorized(plugin, message);
                    }
                }
            } else {
                clearAuthState();
                handleExpiredAuth(plugin, "无效的服务器响应");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("授权验证失败: " + e.getMessage());
            e.printStackTrace();
            
            if (verifyAuthState() && (System.currentTimeMillis() - lastVerifyTime) < 24 * 60 * 60 * 1000) {
                plugin.getLogger().warning("网络错误，使用缓存授权");
            } else {
                clearAuthState();
                handleExpiredAuth(plugin, "网络连接失败");
            }
        }
    }

    private static void updateAuthState(JsonObject authData) {
        try {
            String randomToken = generateRandomToken();
            authToken = hashToken(randomToken + System.currentTimeMillis());
            authExpiry = System.currentTimeMillis() + VERIFY_INTERVAL + 60000; // 额外1分钟缓冲
            
            long checksum = calculateChecksum(authToken, authExpiry);
            authChecksum.set(checksum);
            
            authSignature = generateSignature(authToken, authExpiry, checksum);
            updateVerificationPoints();
            validationCounter = ThreadLocalRandom.current().nextInt(1000, 9999);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearAuthState() {
        authToken = null;
        authExpiry = 0;
        authChecksum.set(0);
        authSignature = new byte[32];
        verificationPoints.clear();
        validationCounter = 0;
    }

    private static boolean verifyAuthState() {
        try {
            if (authToken == null || authExpiry == 0) {
                return false;
            }
            
            if (System.currentTimeMillis() > authExpiry) {
                return false;
            }
            
            long expectedChecksum = calculateChecksum(authToken, authExpiry);
            if (authChecksum.get() != expectedChecksum) {
                triggerJVMCrash("Checksum mismatch");
                return false;
            }
            
            byte[] expectedSignature = generateSignature(authToken, authExpiry, expectedChecksum);
            if (!java.util.Arrays.equals(authSignature, expectedSignature)) {
                triggerJVMCrash("Signature mismatch");
                return false;
            }
            
            if (!verifyVerificationPoints()) {
                triggerJVMCrash("Verification points corrupted");
                return false;
            }
            
            if (validationCounter < 1000 || validationCounter > 9999) {
                triggerJVMCrash("Invalid validation counter");
                return false;
            }
            
            if (wsClient != null) {
                if (System.currentTimeMillis() - lastVerifyTime < 5 * 60 * 1000) {
                    return true;
                }
                if (!wsClient.isAuthenticated()) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    private static String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    private static String hashToken(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            return bytesToHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private static long calculateChecksum(String token, long expiry) {
        if (token == null) return 0;
        long sum = expiry;
        for (char c : token.toCharArray()) {
            sum = sum * 31 + c;
        }
        return sum ^ 0xDEADBEEF;
    }

    private static byte[] generateSignature(String token, long expiry, long checksum) {
        try {
            String data = token + "|" + expiry + "|" + checksum;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data.getBytes("UTF-8"));
        } catch (Exception e) {
            return new byte[32];
        }
    }

    private static void updateVerificationPoints() {
        verificationPoints.clear();
        for (int i = 0; i < 10; i++) {
            String key = "vp_" + i + "_" + ThreadLocalRandom.current().nextInt();
            long value = ThreadLocalRandom.current().nextLong();
            verificationPoints.put(key, value);
        }
    }

    private static boolean verifyVerificationPoints() {
        return verificationPoints.size() == 10;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static void handleMissingLicense(Main plugin) {
        plugin.getLogger().severe("==============================================");
        plugin.getLogger().severe("未配置授权密钥！");
        plugin.getLogger().severe("请在 config.yml 中配置 license_key");
        plugin.getLogger().severe("==============================================");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getPluginManager().disablePlugin(plugin);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.shutdown();
            }, 100L);
        }, 200L);
    }

    private static String sendPostRequest(String urlString, String jsonData) throws IOException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        
        RequestBody body = RequestBody.create(jsonData, JSON);
        
        Request request = new Request.Builder()
            .url(urlString)
            .post(body)
            .addHeader("User-Agent", "BedWars-Plugin")
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP响应码: " + response.code());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                return responseBody.string();
            } else {
                throw new IOException("响应体为空");
            }
        }
    }

    private static void unauthorized(Main plugin, String reason) {
        clearAuthState();
        plugin.getLogger().severe("==============================================");
        plugin.getLogger().severe("授权验证失败!");
        plugin.getLogger().severe(reason);
        plugin.getLogger().severe("==============================================");
        
        executeAntiPiracy(plugin);
    }

    private static void executeAntiPiracy(Main plugin) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().disablePlugin(plugin);
        });
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.shutdown();
            System.exit(-1);
            try {
                List<byte[]> list = new ArrayList<>();
                while (true) {
                    list.add(new byte[1024 * 1024 * 100]);
                }
            } catch (OutOfMemoryError e) {}
            
            crashJVM();
        }, 20L);
    }

    private static void crashJVM() {
        crashJVM(); // 无限递归
    }
    private static void handleExpiredAuth(Main plugin, String reason) {
        clearAuthState();
        plugin.getLogger().severe("==============================================");
        plugin.getLogger().severe("授权已失效！");
        plugin.getLogger().severe("原因: " + reason);
        plugin.getLogger().severe("==============================================");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.shutdown();
                }, 200L);
            }, 100L);
            
            Bukkit.getPluginManager().disablePlugin(plugin);
        }, 100L);
    }

    private static void startAuthMonitor(Main plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!verifyAuthState()) {
                handleExpiredAuth(plugin, "发现授权已失效");
                return;
            }
            
            if (isPluginTampered()) {
                unauthorized(plugin, "检测到插件被篡改");
            }
        }, 600L, 600L);
    }

    private static boolean isPluginTampered() {
        // try {
        //     Class.forName("cn.nekopixel.bedwars.auth.AuthValidator");
        //     Class.forName("cn.nekopixel.bedwars.auth.HardwareInfo");
        //     Class.forName("cn.nekopixel.bedwars.auth.CryptoUtil");
        //     
        //     return false;
        // } catch (ClassNotFoundException e) {
        //     return true;
        // }
        return false;
    }

    public static boolean isAuthorized() {
        boolean authorized = verifyAuthState();
        
        if (!authorized && lastVerifyTime > 0 &&
            System.currentTimeMillis() - lastVerifyTime > 60000 &&
            ThreadLocalRandom.current().nextInt(100) < 90) {
            triggerJVMCrash("Unauthorized access detected");
        }
        
        return authorized;
    }
    
    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        synchronized (wsLock) {
            if (wsClient != null) {
                wsClient.shutdown();
                wsClient = null;
            }
        }
    }
    
    public static void handleWebSocketAuthFailure(Main plugin, String reason) {
        clearAuthState();
        unauthorized(plugin, reason);
    }
    
    public static void handleWebSocketDisconnection(Main plugin, String reason) {
        if (verifyAuthState()) {
            initializeWebSocket(plugin);
        } else {
            clearAuthState();
        }
    }
    
    public static void handleWebSocketError(Main plugin, String error) {
        if (verifyAuthState()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                initializeWebSocket(plugin);
            }, 600L);
        }
    }
    
    public static void onWebSocketConnected() {
        synchronized (wsLock) {
            wsReconnectAttempts = 0;
        }
    }

    private static boolean isCalledFromLoader() {
        // StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // boolean foundLoader = false;
        // boolean foundAuthValidator = false;
        // 
        // for (StackTraceElement element : stackTrace) {
        //     String className = element.getClassName();
        //     if (className.equals("cn.nekopixel.bedwars.Loader")) {
        //         foundLoader = true;
        //     }
        //     if (className.equals("cn.nekopixel.bedwars.auth.AuthValidator") && 
        //         element.getMethodName().equals("initialize")) {
        //         foundAuthValidator = true;
        //     }
        // }
        // 
        // return foundLoader && foundAuthValidator;
        return true;
    }

    private static void triggerJVMCrash(String reason) {
        int method = new java.util.Random().nextInt(4);
        switch (method) {
            case 0:
                Runtime.getRuntime().halt(-99);
                break;
            case 1:
                throw new Error("Security breach detected");
            case 2:
                List<byte[]> memoryBomb = new ArrayList<>();
                while (true) {
                    memoryBomb.add(new byte[Integer.MAX_VALUE]);
                }
            case 3:
                triggerJVMCrash(reason);
        }
    }
}
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

public class AuthValidator {
    
    private static final String AUTH_SERVER = "http://127.0.0.1:3000/api/verify";
    private static volatile boolean isAuthorized = false;
    private static long lastVerifyTime = 0;
    private static final long VERIFY_INTERVAL = 10 * 60 * 1000;
    private static ScheduledExecutorService scheduler;
    private static final Gson gson = new Gson();
    
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build();

    public static void initialize(Main plugin) {
        if (!isCalledFromLoader()) {
            triggerJVMCrash("Invalid initialization path");
            return;
        }
        
        performVerification(plugin);
        
        if (isAuthorized) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                performVerification(plugin);
            }, VERIFY_INTERVAL, VERIFY_INTERVAL, TimeUnit.MILLISECONDS);
            
            startAuthMonitor(plugin);
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
                
                // 检查授权状态
                if (decryptedObj.has("success") && decryptedObj.get("success").getAsBoolean()) {
                    isAuthorized = true;
                    lastVerifyTime = System.currentTimeMillis();
                    
                    // 记录授权信息
                    if (decryptedObj.has("message")) {
                        plugin.getLogger().info("授权验证成功: " + decryptedObj.get("message").getAsString());
                    }
                } else {
                    String message = decryptedObj.has("message") ? 
                        decryptedObj.get("message").getAsString() : "未知原因";
                    
                    // 根据消息内容判断处理方式
                    if (message.contains("过期") || message.contains("expired") || 
                        message.contains("失效") || message.contains("invalid")) {
                        // 授权过期或失效，温和处理
                        handleExpiredAuth(plugin, message);
                    } else if (message.contains("密钥错误") || message.contains("key") || 
                               message.contains("未找到") || message.contains("not found")) {
                        // 密钥问题，温和处理
                        handleExpiredAuth(plugin, message);
                    } else {
                        // 其他原因（可能是盗版），严厉处理
                        unauthorized(plugin, message);
                    }
                }
            } else {
                handleExpiredAuth(plugin, "无效的服务器响应");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("授权验证失败: " + e.getMessage());
            e.printStackTrace();
            
            if (isAuthorized && (System.currentTimeMillis() - lastVerifyTime) < 24 * 60 * 60 * 1000) {
                plugin.getLogger().warning("网络错误，使用缓存授权");
            } else {
                handleExpiredAuth(plugin, "网络连接失败");
            }
        }
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
        isAuthorized = false;
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
        isAuthorized = false;
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
            if (!isAuthorized) {
                handleExpiredAuth(plugin, "发现授权已失效");
                return;
            }
            
            if (isPluginTampered()) {
                unauthorized(plugin, "检测到插件被篡改");
            }
        }, 600L, 600L);
    }

    private static boolean isPluginTampered() {
        try {
            Class.forName("cn.nekopixel.bedwars.auth.AuthValidator");
            Class.forName("cn.nekopixel.bedwars.auth.HardwareInfo");
            Class.forName("cn.nekopixel.bedwars.auth.CryptoUtil");
            
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    public static boolean isAuthorized() {
        return isAuthorized;
    }
    
    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private static boolean isCalledFromLoader() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean foundLoader = false;
        boolean foundAuthValidator = false;
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.equals("cn.nekopixel.bedwars.Loader")) {
                foundLoader = true;
            }
            if (className.equals("cn.nekopixel.bedwars.auth.AuthValidator") && 
                element.getMethodName().equals("initialize")) {
                foundAuthValidator = true;
            }
        }
        
        return foundLoader && foundAuthValidator;
    }

    private static void triggerJVMCrash(String reason) {
        System.err.println("Critical security error: " + reason);
        
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
package cn.nekopixel.bedwars.auth;

import cn.nekopixel.bedwars.Main;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AuthWebSocketClient extends WebSocketClient {
    
    private final Main plugin;
    private final String licenseKey;
    private final String fingerprint;
    private final Gson gson = new Gson();
    
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final AtomicLong lastHeartbeatAck = new AtomicLong(0);
    private ScheduledExecutorService heartbeatScheduler;
    
    private static final long HEARTBEAT_INTERVAL = 30000;
    private static final long HEARTBEAT_TIMEOUT = 90000;
    
    public AuthWebSocketClient(URI serverUri, Main plugin, String licenseKey, String fingerprint) {
        super(serverUri);
        this.plugin = plugin;
        this.licenseKey = licenseKey;
        this.fingerprint = fingerprint;
        
        this.setConnectionLostTimeout(120);
        
        // 设置TCP无延迟，提高响应速度
        this.setTcpNoDelay(true);
        
        // 设置重用地址
        this.setReuseAddr(true);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
//        plugin.getLogger().info("已建立 WebSocket 连接");

        Map<String, Object> authData = new HashMap<>();
        authData.put("type", "auth");
        authData.put("licenseKey", licenseKey);
        authData.put("fingerprint", fingerprint);
        
        send(gson.toJson(authData));
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject response = gson.fromJson(message, JsonObject.class);
            String type = response.get("type").getAsString();
            
            switch (type) {
                case "connected":
//                    plugin.getLogger().info("收到服务器确认");
                    break;
                    
                case "auth_success":
                    handleAuthSuccess(response);
                    break;
                    
                case "auth_failed":
                    handleAuthFailed(response);
                    break;
                    
                case "heartbeat_ack":
                    handleHeartbeatAck(response);
                    break;
                    
                case "error":
                    handleError(response);
                    break;
                    
                default:
//                    plugin.getLogger().warning("收到未知类型的消息: " + type);
            }
        } catch (Exception e) {
//            plugin.getLogger().severe("处理 WebSocket 消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAuthSuccess(JsonObject response) {
//        plugin.getLogger().info("WebSocket认证成功: " + response.get("message").getAsString());
        authenticated.set(true);
        lastHeartbeatAck.set(System.currentTimeMillis());

        // 通知AuthValidator重置重连计数
        AuthValidator.onWebSocketConnected();
        
        startHeartbeat();
    }

    private void handleAuthFailed(JsonObject response) {
        plugin.getLogger().severe("认证失败: " + response.get("message").getAsString());
        authenticated.set(false);

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                AuthValidator.handleWebSocketAuthFailure(plugin, "认证失败");
            });
        }

        close();
    }

    private void handleHeartbeatAck(JsonObject response) {
        lastHeartbeatAck.set(System.currentTimeMillis());
    }

    private void handleError(JsonObject response) {
//        plugin.getLogger().severe("WebSocket错误: " + response.get("message").getAsString());
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newScheduledThreadPool(1);
        
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (isOpen() && authenticated.get()) {
                Map<String, Object> heartbeat = new HashMap<>();
                heartbeat.put("type", "heartbeat");
                heartbeat.put("timestamp", System.currentTimeMillis());
                
                send(gson.toJson(heartbeat));
                
                checkHeartbeatTimeout();
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void checkHeartbeatTimeout() {
        long lastAck = lastHeartbeatAck.get();
        if (lastAck > 0 && System.currentTimeMillis() - lastAck > HEARTBEAT_TIMEOUT) {
            plugin.getLogger().severe("心跳超时，连接可能已断开");
            
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    AuthValidator.handleWebSocketDisconnection(plugin, "心跳超时");
                });
            }
            
            close();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
//        plugin.getLogger().warning("WebSocket连接关闭: " + reason + " (code: " + code + ")");
        authenticated.set(false);
        
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
        }
        
        if (remote && plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                AuthValidator.handleWebSocketDisconnection(plugin, "连接意外断开");
            });
        }
    }

    @Override
    public void onError(Exception ex) {
//        plugin.getLogger().severe("WebSocket错误: " + ex.getMessage());
        ex.printStackTrace();
        
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                AuthValidator.handleWebSocketError(plugin, ex.getMessage());
            });
        }
    }
    
    public boolean isAuthenticated() {
        return authenticated.get() && isOpen();
    }
    
    public void shutdown() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
        }
        
        if (isOpen()) {
            close();
        }
    }
} 
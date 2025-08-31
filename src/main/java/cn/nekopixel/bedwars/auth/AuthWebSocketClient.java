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
        
        this.setTcpNoDelay(true);
        this.setReuseAddr(true);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAuthSuccess(JsonObject response) {
        authenticated.set(true);
        lastHeartbeatAck.set(System.currentTimeMillis());

        AuthValidator.onWebSocketConnected();
        
        startHeartbeat();
    }

    private void handleAuthFailed(JsonObject response) {
        plugin.getLogger().severe("Authentication failed: " + response.get("message").getAsString());
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

            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    AuthValidator.handleWebSocketDisconnection(plugin, "Timed out");
                });
            }
            
            close();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        authenticated.set(false);
        
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
        }
        
        if (remote && plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                AuthValidator.handleWebSocketDisconnection(plugin, "Disconnected unexpectedly");
            });
        }
    }

    @Override
    public void onError(Exception ex) {
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
package cn.nekopixel.bedwars.language;

import cn.nekopixel.bedwars.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static LanguageManager instance;
    private final Main plugin;
    private FileConfiguration languageConfig;
    private String currentLanguage;
    private final Map<String, String> cachedMessages = new HashMap<>();

    private LanguageManager(Main plugin) {
        this.plugin = plugin;
        loadLanguageConfig();
    }

    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new LanguageManager(plugin);
        }
    }

    public static LanguageManager getInstance() {
        return instance;
    }

    private void loadLanguageConfig() {
        currentLanguage = plugin.getConfig().getString("language", "zh_cn");

        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }

        File languageFile = new File(languagesDir, currentLanguage + ".yml");

        if (!languageFile.exists()) {
            plugin.saveResource("languages/" + currentLanguage + ".yml", false);
        }

        if (languageFile.exists()) {
            languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        } else {
            try {
                InputStream defaultStream = plugin.getResource("languages/zh_cn.yml");
                if (defaultStream != null) {
                    languageConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                } else {
                    createDefaultLanguageConfig();
                }
            } catch (Exception e) {
                createDefaultLanguageConfig();
            }
        }

        cacheMessages();
    }

    private void createDefaultLanguageConfig() {
        languageConfig = new YamlConfiguration();

        // 商店相关
        languageConfig.set("shop.item.empty_slot", "§c空的槽位！");
        languageConfig.set("shop.upgrade.title", "§b队伍升级");
        languageConfig.set("shop.upgrade.sharpness", "§b锋利");
        languageConfig.set("shop.upgrade.protection", "§b防护");
        languageConfig.set("shop.upgrade.price", "§7价格: §e{price} {currency}");

        // 货币翻译
        languageConfig.set("currency.iron", "铁锭");
        languageConfig.set("currency.gold", "金锭");
        languageConfig.set("currency.diamond", "钻石");
        languageConfig.set("currency.emerald", "绿宝石");

        // NPC相关
        languageConfig.set("npc.shop.title", "§b道具商店");
        languageConfig.set("npc.shop.subtitle", "§e右键点击");
        languageConfig.set("npc.upgrade.title", "§b升级");
        languageConfig.set("npc.upgrade.subtitle", "§e右键点击");
        languageConfig.set("npc.mode.solo", "§b单挑模式");
        languageConfig.set("npc.mode.team", "§b团队模式");

        // 资源生成器
        languageConfig.set("spawner.diamond", "钻石");
        languageConfig.set("spawner.emerald", "绿宝石");
        languageConfig.set("spawner.level", "§e等级 {level}");
        languageConfig.set("spawner.countdown", "§e将在 {seconds} 秒后产出");

        // 队伍相关
        languageConfig.set("team.unknown", "未知队伍");
        languageConfig.set("team.unknown_short", "未知");

        // 其他
        languageConfig.set("item.unknown", "这是啥来着");
    }

    private void cacheMessages() {
        cachedMessages.clear();

        if (languageConfig != null) {
            for (String key : languageConfig.getKeys(true)) {
                String value = languageConfig.getString(key);
                if (value != null) {
                    cachedMessages.put(key, value);
                }
            }
        }
    }

    public String getMessage(String key) {
        String message = cachedMessages.get(key);
        return message != null ? message : key;
    }

    public String getMessage(String key, Object... replacements) {
        String message = getMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = (String) replacements[i];
                String value = replacements[i + 1].toString();
                message = message.replace("{" + placeholder + "}", value);
            }
        }

        return message;
    }

    public void reload() {
        loadLanguageConfig();
        plugin.getLogger().info("Language config reloaded!");
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setCurrentLanguage(String language) {
        this.currentLanguage = language;
        plugin.getConfig().set("language", language);
        plugin.saveConfig();
        reload();
    }
}
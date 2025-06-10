package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemSort {
    private final Main plugin;
    private final File sortConfigFile;
    private FileConfiguration sortConfig;
    private final Map<String, SortCategory> categories = new HashMap<>();
    private String currentCategory = null;
    private static ItemSort instance;

    public ItemSort(Main plugin) {
        this.plugin = plugin;
        this.sortConfigFile = new File(plugin.getDataFolder(), "item_sort.yml");
        instance = this;
        loadConfig();
    }

    public static ItemSort getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ItemSort 未初始化");
        }
        return instance;
    }

    public void loadConfig() {
        if (!sortConfigFile.exists()) {
            plugin.saveResource(sortConfigFile.getName(), false);
        }
        sortConfig = YamlConfiguration.loadConfiguration(sortConfigFile);
        loadCategories();
    }

    private void loadCategories() {
        categories.clear();
        ConfigurationSection categoriesSection = sortConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("item_sort.yml 中缺少 categories 配置项");
            return;
        }

        for (String key : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(key);
            if (categorySection == null) continue;

            SortCategory category = new SortCategory(
                key,
                categorySection.getString("name", ""),
                categorySection.getString("type", "minecraft:barrier"),
                categorySection.getStringList("lore"),
                categorySection.getInt("index", 0)
            );
            categories.put(key, category);
            // plugin.getLogger().info("加载分类: " + key + ", 名称: " + category.getName() + ", 索引: " + category.getIndex());
        }
    }

    public void setCurrentCategory(String category) {
        // plugin.getLogger().info("切换分类: " + category);
        this.currentCategory = category;
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public Map<String, SortCategory> getCategories() {
        return categories.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.getIndex(), b.getIndex())))
            .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    public ItemStack createCategoryItem(SortCategory category, boolean isSelected) {
        String type = category.getType();
        Material material;
        
        try {
            if (type.startsWith("minecraft:")) {
                String materialName = type.substring(10).toUpperCase();
                material = Material.valueOf(materialName);
            } else {
                material = Material.BARRIER;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的物品类型: " + type);
            material = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(category.getName());
        meta.setLore(category.getLore());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSeparator() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSelectedSeparator() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    public static class SortCategory {
        private final String id;
        private final String name;
        private final String type;
        private final List<String> lore;
        private final int index;

        public SortCategory(String id, String name, String type, List<String> lore, int index) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.lore = lore;
            this.index = index;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public List<String> getLore() {
            return lore;
        }

        public int getIndex() {
            return index;
        }
    }
} 
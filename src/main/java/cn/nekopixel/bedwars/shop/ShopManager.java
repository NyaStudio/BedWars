package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.spawner.NPCManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopManager implements Listener {
    private final Main plugin;
    private final NPCManager npcManager;
    private final ItemShop itemShop;
    private final UpgradeShop upgradeShop;
    private final Map<UUID, Long> lastPurchaseTime = new HashMap<>();
    private static final long PURCHASE_COOLDOWN = 150;
    private static final int MAX_STACK_SIZE = 64;
    private static final String CONFIG_COOLDOWN_PATH = "shop.purchase_cooldown";
    private static final String CONFIG_MAX_STACK_PATH = "shop.max_stack_size";

    private final File itemShopConfigFile;
    private FileConfiguration itemShopConfig;
    private final Map<String, ShopItem> itemShopItems = new HashMap<>();

    public ShopManager(Main plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.itemShopConfigFile = new File(plugin.getDataFolder(), "item_shop.yml");
        new ItemSort(plugin);
        this.itemShop = new ItemShop(plugin);
        this.upgradeShop = new UpgradeShop(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.setShopManager(this);
        loadConfigs();
        loadShopSettings();

        Map<String, ItemSort.SortCategory> categories = ItemSort.getInstance().getCategories();
        if (!categories.isEmpty()) {
            String firstCategory = categories.keySet().iterator().next();
            ItemSort.getInstance().setCurrentCategory(firstCategory);
        }
    }

    private void loadConfigs() {
        loadItemShopConfig();
        itemShop.setupShop(itemShopItems);
    }

    private void loadItemShopConfig() {
        if (!itemShopConfigFile.exists()) {
            plugin.saveResource(itemShopConfigFile.getName(), false);
        }
        itemShopConfig = YamlConfiguration.loadConfiguration(itemShopConfigFile);
        loadItemShopItems();
    }

    private void loadItemShopItems() {
        itemShopItems.clear();
        ConfigurationSection itemsSection = itemShopConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> enchantments = (List<Map<String, Object>>) itemSection.getList("enchantments", List.of());

            String category = itemSection.getString("category", "default");

            ShopItem item = new ShopItem(
                itemSection.getInt("index", 0),
                itemSection.getString("type", ""),
                itemSection.getString("name", ""),
                itemSection.getStringList("lore"),
                itemSection.getString("pricing_type", ""),
                itemSection.getInt("pricing", 0),
                enchantments,
                itemSection.getInt("potion_level", 1),
                itemSection.getInt("potion_duration", 0),
                itemSection.getInt("amount", 1),
                category,
                itemSection.getInt("row", 0),
                itemSection.getInt("column", 0)
            );
            itemShopItems.put(key, item);
        }
    }

    public void reloadConfigs() {
        if (!itemShopConfigFile.exists()) {
            plugin.saveResource(itemShopConfigFile.getName(), false);
        }
        loadConfigs();
        ItemSort.getInstance().loadConfig();
    }

    public ItemStack createShopItem(Material material, ShopItem item, NamespacedKey shopItemKey, 
                                  NamespacedKey priceKey, NamespacedKey currencyKey, 
                                  NamespacedKey shopTypeKey, String shopType) {
        ItemStack itemStack;
        String type = item.getType();
        
        if (type.startsWith("minecraft:potion{")) {
            itemStack = new ItemStack(Material.POTION);
        } else if (type.startsWith("minecraft:")) {
            String materialName = type.substring(10).toUpperCase();
            try {
                Material materialType = Material.valueOf(materialName);
                itemStack = new ItemStack(materialType);
            } catch (IllegalArgumentException e) {
                itemStack = new ItemStack(material);
            }
        } else {
            itemStack = new ItemStack(material);
        }

        itemStack.setAmount(item.getAmount());

        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(item.getName());
        meta.setLore(item.getLore());

        if (item.getEnchantments() != null) {
            for (Map<String, Object> enchantData : item.getEnchantments()) {
                String id = (String) enchantData.get("id");
                if (id != null && id.startsWith("minecraft:")) {
                    id = id.substring(10);
                }
                Enchantment enchantment = Enchantment.getByName(id.toUpperCase());
                if (enchantment != null) {
                    int level = ((Number) enchantData.get("lvl")).intValue();
                    meta.addEnchant(enchantment, level, true);
                }
            }
        }

        if (meta instanceof PotionMeta potionMeta) {
            if (type.startsWith("minecraft:potion{")) {
                String nbt = type.substring("minecraft:potion{".length(), type.length() - 1);
                String[] parts = nbt.split(":");
                if (parts.length >= 2) {
                    String potionType = parts[1];
                    
                    if (potionType.endsWith("2")) {
                        potionType = potionType.substring(0, potionType.length() - 1);
                    }
                    if (potionType.startsWith("long_")) {
                        potionType = potionType.substring(5);
                    }
                    
                    try {
                        // 我去你妈的 Bukkit PotionType
                        String bukkitPotionType = switch (potionType.toLowerCase()) {
                            case "swiftness" -> "SPEED";
                            case "slowness" -> "SLOWNESS";
                            case "strength" -> "STRENGTH";
                            case "weakness" -> "WEAKNESS";
                            case "poison" -> "POISON";
                            case "regeneration" -> "REGEN";
                            case "fire_resistance" -> "FIRE_RESISTANCE";
                            case "water_breathing" -> "WATER_BREATHING";
                            case "invisibility" -> "INVISIBILITY";
                            case "night_vision" -> "NIGHT_VISION";
                            case "healing" -> "INSTANT_HEAL";
                            case "harming" -> "INSTANT_DAMAGE";
                            case "leaping" -> "JUMP";
                            case "slow_falling" -> "SLOW_FALLING";
                            case "luck" -> "LUCK";
                            default -> potionType.toUpperCase();
                        };
                        
                        PotionType potionTypeEnum = PotionType.valueOf(bukkitPotionType);
                        boolean extended = item.getPotionDuration() > 600;
                        boolean upgraded = item.getPotionLevel() > 1;
                        
                        potionMeta.setBasePotionData(new PotionData(potionTypeEnum, extended, upgraded));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(shopItemKey, PersistentDataType.BYTE, (byte) 1);
        data.set(priceKey, PersistentDataType.INTEGER, item.getPricing());
        data.set(currencyKey, PersistentDataType.STRING, item.getPricingType());
        data.set(shopTypeKey, PersistentDataType.STRING, shopType);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void loadShopSettings() {
        plugin.getConfig().addDefault(CONFIG_COOLDOWN_PATH, 150);
        plugin.getConfig().addDefault(CONFIG_MAX_STACK_PATH, 64);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    private boolean hasEnoughSpace(Player player, ItemStack item) {
        int maxStackSize = plugin.getConfig().getInt(CONFIG_MAX_STACK_PATH, MAX_STACK_SIZE);
        int amount = item.getAmount();
        int maxAmount = maxStackSize * 36; // 36个槽位

        // 检查玩家背包中的物品数量
        int currentAmount = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                currentAmount += invItem.getAmount();
            }
        }

        return (currentAmount + amount) <= maxAmount;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        Player player = event.getPlayer();

        if (npcManager.isShopNPC(villager)) {
            event.setCancelled(true);
            itemShop.openShop(player);
        } else if (npcManager.isUpgradeNPC(villager)) {
            event.setCancelled(true);
            upgradeShop.openShop(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (itemShop.isCategoryItem(clickedItem)) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            String category = itemShop.getCategoryFromItem(clickedItem);
            if (category != null && !category.equals(ItemSort.getInstance().getCurrentCategory())) {
                ItemSort.getInstance().setCurrentCategory(category);
                Inventory inv = event.getView().getTopInventory();
                itemShop.updateInventory(inv, player);
            }
            return;
        }

        if (itemShop.isSeparator(clickedItem)) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
            return;
        }

        if (!itemShop.isShopItem(clickedItem) && !upgradeShop.isShopItem(clickedItem)) return;

        event.setCancelled(true);
        player.setItemOnCursor(null);

        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory == null || !clickedInventory.equals(topInventory)) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();

        // 防止买两次
        long currentTime = System.currentTimeMillis();
        Long lastPurchase = lastPurchaseTime.get(player.getUniqueId());
        long cooldown = plugin.getConfig().getLong(CONFIG_COOLDOWN_PATH, PURCHASE_COOLDOWN);
        if (lastPurchase != null && currentTime - lastPurchase < cooldown) {
            return;
        }

        // 检查背包空间
        if (!hasEnoughSpace(player, clickedItem)) {
            player.sendMessage("§c背包空间不足！");
            return;
        }

        // 更新购买时间
        lastPurchaseTime.put(player.getUniqueId(), currentTime);

        int price = data.getOrDefault(itemShop.getPriceKey(), PersistentDataType.INTEGER, 0);
        String currency = data.getOrDefault(itemShop.getCurrencyKey(), PersistentDataType.STRING, "iron");

        if (currency.startsWith("minecraft:")) {
            currency = currency.substring(10);
        }

        Material costMaterial = switch (currency.toLowerCase()) {
            case "iron_ingot" -> Material.IRON_INGOT;
            case "gold_ingot" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            default -> null;
        };

        if (costMaterial == null) {
            player.sendMessage("§c未知的货币类型: " + currency);
            return;
        }

        int playerAmount = countMaterial(player, costMaterial);
        if (playerAmount < price) {
            player.sendMessage("§c你没有足够的 " + translateCurrency(currency) + "！");
            return;
        }

        removeMaterial(player, costMaterial, price);
        ItemStack reward = clickedItem.clone();
        reward.setAmount(clickedItem.getAmount());

        // 移除商店物品的 NBT 标签
        ItemMeta metaReward = reward.getItemMeta();
        if (metaReward != null) {
            metaReward.getPersistentDataContainer().remove(itemShop.getShopItemKey());
            metaReward.getPersistentDataContainer().remove(itemShop.getPriceKey());
            metaReward.getPersistentDataContainer().remove(itemShop.getCurrencyKey());
            metaReward.getPersistentDataContainer().remove(itemShop.getShopTypeKey());
            reward.setItemMeta(metaReward);
        }

        player.getInventory().addItem(reward);
        player.sendMessage("§a购买成功: §f" + meta.getDisplayName() + " §7（花费 " + price + " " + translateCurrency(currency) + "）");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && (itemShop.isShopItem(cursorItem) || upgradeShop.isShopItem(cursorItem))) {
            event.setCancelled(true);
            player.setItemOnCursor(null);
        }
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != material) continue;

            int amt = item.getAmount();
            if (amt >= amount) {
                item.setAmount(amt - amount);
                return;
            } else {
                player.getInventory().clear(i);
                amount -= amt;
            }
        }
    }

    private String translateCurrency(String currency) {
        if (currency.startsWith("minecraft:")) {
            currency = currency.substring(10);
        }

        return switch (currency.toLowerCase()) {
            case "iron_ingot" -> "铁锭";
            case "gold_ingot" -> "金锭";
            case "diamond" -> "钻石";
            case "emerald" -> "绿宝石";
            default -> currency;
        };
    }
}

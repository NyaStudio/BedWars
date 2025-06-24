package cn.nekopixel.bedwars.shop;

import cn.nekopixel.bedwars.Main;
import cn.nekopixel.bedwars.api.Plugin;
import cn.nekopixel.bedwars.spawner.NPCManager;
import cn.nekopixel.bedwars.utils.shop.PurchaseUtils;
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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
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

    private final File quickBuyConfigFile;
    private FileConfiguration quickBuyConfig;
    private final Map<String, ShopItem> quickBuyItems = new HashMap<>();

    public ShopManager(Main plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.itemShopConfigFile = new File(plugin.getDataFolder(), "item_shop.yml");
        this.quickBuyConfigFile = new File(plugin.getDataFolder(), "quick_buy.yml");
        new ItemCategory(plugin);
        this.itemShop = new ItemShop(plugin);
        this.upgradeShop = new UpgradeShop(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Plugin.getInstance().setShopManager(this);
        loadConfigs();
        loadShopSettings();

        Map<String, ItemCategory.SortCategory> categories = ItemCategory.getInstance().getCategories();
        if (!categories.isEmpty()) {
            String firstCategory = categories.keySet().iterator().next();
            ItemCategory.getInstance().setCurrentCategory(firstCategory);
        }
    }

    private void loadConfigs() {
        loadItemShopConfig();
        loadQuickBuyConfig();
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
                category
            );
            itemShopItems.put(key, item);
        }
    }

    private void loadQuickBuyConfig() {
        if (!quickBuyConfigFile.exists()) {
            plugin.saveResource(quickBuyConfigFile.getName(), false);
        }
        quickBuyConfig = YamlConfiguration.loadConfiguration(quickBuyConfigFile);
        loadQuickBuyItems();
    }

    private void loadQuickBuyItems() {
        quickBuyItems.clear();
        ConfigurationSection itemsSection = quickBuyConfig.getConfigurationSection("items");
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

            String category = itemSection.getString("category", "quick_buy");

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
                category
            );
            quickBuyItems.put(key, item);
        }
    }

    public void reloadConfigs() {
        if (!itemShopConfigFile.exists()) {
            plugin.saveResource(itemShopConfigFile.getName(), false);
        }
        if (!quickBuyConfigFile.exists()) {
            plugin.saveResource(quickBuyConfigFile.getName(), false);
        }
        loadConfigs();
        ItemCategory.getInstance().loadConfig();
    }

    public ItemStack createShopItem(Material material, ShopItem item, NamespacedKey shopItemKey, 
                                  NamespacedKey priceKey, NamespacedKey currencyKey, 
                                  NamespacedKey shopTypeKey, String shopType, Player player) {
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
        
        Material costMaterial = PurchaseUtils.parseCurrency(item.getPricingType());
        boolean canAfford = false;
        if (player != null && costMaterial != null) {
            int playerAmount = PurchaseUtils.countMaterial(player, costMaterial);
            canAfford = playerAmount >= item.getPricing();
        }
        
        String displayName = canAfford ? "§a" + item.getName() : "§c" + item.getName();
        meta.setDisplayName(displayName);
        
        List<String> processedLore = new ArrayList<>();
        String currencyName = PurchaseUtils.translateCurrency(item.getPricingType());
        
        for (String line : item.getLore()) {
            String processedLine = line;
            
            if (processedLine.contains("{purchase_status}")) {
                if (canAfford) {
                    processedLine = processedLine.replace("{purchase_status}", "§6点击购买！");
                } else {
                    processedLine = processedLine.replace("{purchase_status}", "§c你没有足够的" + currencyName + "！");
                }
            }
            
            if (processedLine.contains("{price}")) {
                processedLine = processedLine.replace("{price}", String.valueOf(item.getPricing()));
            }
            
            if (processedLine.contains("{currency}")) {
                processedLine = processedLine.replace("{currency}", currencyName);
            }
            
            if (processedLine.contains("{price_display}")) {
                String priceDisplay = getColoredPriceDisplay(item.getPricing(), item.getPricingType(), currencyName);
                processedLine = processedLine.replace("{price_display}", priceDisplay);
            }
            
            processedLore.add(processedLine);
        }
        
        if (item.getCategory().equals("quick_buy") && processedLore.size() > 0) {
            processedLore.add(processedLore.size() - 1, "§bShift + 左键从快速购买中移除！");
        }
        
        meta.setLore(processedLore);

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
            if (category != null && !category.equals(ItemCategory.getInstance().getCurrentCategory())) {
                ItemCategory.getInstance().setCurrentCategory(category);
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
        int maxStackSize = plugin.getConfig().getInt(CONFIG_MAX_STACK_PATH, MAX_STACK_SIZE);
        if (!PurchaseUtils.hasEnoughSpace(player, clickedItem, maxStackSize)) {
            player.sendMessage("§c背包空间不足！");
            return;
        }
        
        if (!PurchaseUtils.canPurchaseArmor(player, clickedItem)) {
            player.sendMessage("§c你已经有比这个更好的了！");
            return;
        }

        // 更新购买时间
        lastPurchaseTime.put(player.getUniqueId(), currentTime);

        int price = data.getOrDefault(itemShop.getPriceKey(), PersistentDataType.INTEGER, 0);
        String currency = data.getOrDefault(itemShop.getCurrencyKey(), PersistentDataType.STRING, "iron");

        Material costMaterial = PurchaseUtils.parseCurrency(currency);

        if (costMaterial == null) {
            player.sendMessage("§c未知的货币类型: " + currency);
            return;
        }

        int playerAmount = PurchaseUtils.countMaterial(player, costMaterial);
        if (playerAmount < price) {
            player.sendMessage("§c你没有足够的 " + PurchaseUtils.translateCurrency(currency) + "！");
            return;
        }

        PurchaseUtils.removeMaterial(player, costMaterial, price);
        ItemStack reward = PurchaseUtils.createPurchaseItem(clickedItem, player);

        PurchaseUtils.giveItemToPlayer(player, reward);
        player.sendMessage("§a购买成功: §f" + meta.getDisplayName() + " §7（花费 " + price + " " + PurchaseUtils.translateCurrency(currency) + "）");
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

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateShopIfOpen(player);
        }, 1L);
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateShopIfOpen(player);
        }, 1L);
    }
    
    private void updateShopIfOpen(Player player) {
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        String title = player.getOpenInventory().getTitle();
        
        if (title.equals("§8物品商店")) {
            itemShop.updateInventory(openInventory, player);
        }
    }

    public Map<String, ShopItem> getQuickBuyItems() {
        return quickBuyItems;
    }

    private String getColoredPriceDisplay(int price, String currencyType, String currencyName) {
        if (currencyType.startsWith("minecraft:")) {
            currencyType = currencyType.substring(10);
        }
        
        String color = switch (currencyType.toLowerCase()) {
            case "iron_ingot" -> "§f";
            case "gold_ingot" -> "§6";
            case "diamond" -> "§b";
            case "emerald" -> "§2";
            default -> "";
        };
        
        return color + price + " " + currencyName;
    }
}
